package dev.dtzudontsay.knownworld.simulation.population;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * B4.3A: sparse, world-scoped projection handoffs. Identity/schedule remain
 * in B4.0/B4.2; no 3.76-million-person tick loop or new NPC registry.
 * Samples contain observed location, not invented history or economic work.
 * All methods are called from the server thread.
 */
public final class KnownWorldB43OffscreenTravel {
    public record Sample(String id, String site, int homeTileX, int homeTileZ,
                         int x, int z, long at) { }
    private static final int MAX_SAMPLES = 4096;
    private static final long MAX_AGE_TICKS = 14L * KnownWorldB42Schedule.DAY;
    private static final int MAX_FILE_BYTES = 1_600_000;
    private static MinecraftServer server;
    private static Path file;
    private static final Map<String, Sample> SAMPLES = new HashMap<>();
    private static boolean disabled, dirty;
    private static long lastFlush = -1;
    private KnownWorldB43OffscreenTravel() { }

    public static long now() {
        NpcSimulation sim = NpcSimulation.getNullable();
        if (sim == null) return -1;
        return sim.campaignCalendar().absoluteDay() * KnownWorldB42Schedule.DAY
                + sim.campaignCalendar().tickRemainder();
    }

    public static void bind(MinecraftServer current) {
        if (server == current) return;
        if (server != null && dirty) flush();
        server = current;
        file = current.getWorldPath(LevelResource.ROOT).resolve("data")
                .resolve("knownworld").resolve("offscreen_travel_b4_3a_v1.json");
        SAMPLES.clear(); disabled = false; dirty = false; lastFlush = -1;
        if (!Files.exists(file)) return;
        try {
            if (Files.size(file) > MAX_FILE_BYTES) throw new IOException("Oversized travel state");
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1 ||
                    !"OBSERVED_PROJECTION_HANDOFF_ONLY".equals(root.get("authority").getAsString()))
                throw new IOException("Unexpected B4.3A travel schema");
            JsonArray records = root.getAsJsonArray("samples");
            if (records.size() > MAX_SAMPLES) throw new IOException("Excessive travel sample count");
            for (JsonElement e : records) {
                JsonObject j = e.getAsJsonObject();
                Sample sample = new Sample(j.get("id").getAsString(), j.get("site").getAsString(),
                        j.get("tileX").getAsInt(), j.get("tileZ").getAsInt(),
                        j.get("x").getAsInt(), j.get("z").getAsInt(), j.get("at").getAsLong());
                if (!valid(sample) || SAMPLES.putIfAbsent(sample.id(), sample) != null)
                    throw new IOException("Invalid/duplicate B4.3A identity");
            }
        } catch (Exception err) {
            disabled = true; SAMPLES.clear();
            KnownWorld.LOGGER.error("B4.3A travel state invalid. Leaving original file untouched; travel handoffs disabled.",err);
        }
    }

    private static boolean valid(Sample s) {
        return s.id().startsWith("b4:" + s.site() + ":") && s.id().length() < 175 &&
                s.site().matches("site_[a-z][a-z0-9_]{1,90}") &&
                Math.abs((long)s.x()) < 30_000_000L && Math.abs((long)s.z()) < 30_000_000L &&
                s.at() >= 0;
    }

    public static void leftView(String personId, String siteId, int sourceX, int sourceZ,
                                int observedX, int observedZ) {
        if (server == null || disabled) return;
        long tick = now();
        if (tick < 0) return;
        Sample sample = new Sample(personId,siteId,Math.floorDiv(sourceX,512),Math.floorDiv(sourceZ,512),
                observedX, observedZ,tick);
        if (!valid(sample)) return;
        SAMPLES.put(personId,sample); dirty=true;
        trim(tick);
    }

    public static int[] reconcile(String personId, int[] modeled) {
        if (disabled || server == null || modeled == null || modeled.length < 2) return modeled;
        Sample old = SAMPLES.get(personId);
        if (old == null) return modeled;
        long t = now();
        if (t < old.at() || t - old.at() > MAX_AGE_TICKS) return modeled;
        // B4.2B: the campaign-time route position always wins when an NPC
        // becomes observable. Old sightings may aid debugging and future
        // exceptional-journey handoffs, but must NEVER pin an actor to where
        // the player last saw them (or hide them from a road interception).
        return modeled;
    }

    /** Potentially interceptable RECENT handoffs. Final selection still validates
     * the authoritative B4.0 person tile, live chunk, player radius and owner. */
    public static List<Sample> nearby(int x,int z,int radius) {
        if (disabled || server == null) return List.of();
        long tick=now(); if (tick<0) return List.of();
        List<Sample> result=new ArrayList<>();
        long limit=(long)radius * radius;
        for (Sample s : SAMPLES.values()) {
            if (tick<s.at() || tick-s.at()>MAX_AGE_TICKS) continue;
            long dx=(long)s.x()-x,dz=(long)s.z()-z;
            if (dx*dx+dz*dz<=limit) result.add(s);
        }
        return result;
    }

    /** Once a projected actor exists again, its old observation must not
     * keep biasing subsequent schedule calculations. */
    public static void appeared(String personId) {
        if (SAMPLES.remove(personId) != null) dirty = true;
    }
    public static int count() { return SAMPLES.size(); }
    public static boolean healthy() { return !disabled; }
    private static void trim(long tick) {
        SAMPLES.values().removeIf(s -> tick>=s.at() && tick-s.at()>MAX_AGE_TICKS);
        if (SAMPLES.size() <= MAX_SAMPLES) return;
        List<Sample> oldest = SAMPLES.values().stream()
                .sorted(Comparator.comparingLong(Sample::at).thenComparing(Sample::id)).toList();
        for (int i=0;i<oldest.size()-MAX_SAMPLES;i++) SAMPLES.remove(oldest.get(i).id());
    }
    public static void flushIfDue() {
        long tick=now();
        if (dirty && tick>=0 && (lastFlush<0||tick<lastFlush||tick-lastFlush>=400)) flush();
    }
    public static void flush() {
        if (server==null || !dirty || disabled) return;
        long tick=now();if(tick>=0)trim(tick);
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version",1);
            root.addProperty("authority","OBSERVED_PROJECTION_HANDOFF_ONLY");
            JsonArray records=new JsonArray();
            for (Sample s : SAMPLES.values().stream().sorted(Comparator.comparing(Sample::id)).toList()) {
                JsonObject item=new JsonObject();item.addProperty("id",s.id());item.addProperty("site",s.site());
                item.addProperty("tileX",s.homeTileX());item.addProperty("tileZ",s.homeTileZ());
                item.addProperty("x",s.x());item.addProperty("z",s.z());item.addProperty("at",s.at());
                records.add(item);
            }
            root.add("samples",records);
            String contents = root.toString();
            if (contents.getBytes(StandardCharsets.UTF_8).length > MAX_FILE_BYTES)
                throw new IOException("Travel state too large; no overwrite");
            Path temporary=Files.createTempFile(file.getParent(),"b43travel-",".tmp");
            try {
                Files.writeString(temporary,contents,StandardCharsets.UTF_8);
                try { Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException ignored) {Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
            } finally { Files.deleteIfExists(temporary); }
            dirty=false;lastFlush=tick;
        } catch (IOException err) {
            disabled=true;
            KnownWorld.LOGGER.error("B4.3A offscreen handoff write failed; refusing to overwrite damaged state",err);
        }
    }
    public static void stop(MinecraftServer stopped) {
        if (server!=stopped) return;
        flush();SAMPLES.clear();server=null;file=null;disabled=false;dirty=false;lastFlush=-1;
    }
}
