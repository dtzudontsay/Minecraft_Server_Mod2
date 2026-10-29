package dev.dtzudontsay.knownworld.simulation.attributes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * AV2C: bounded, authenticated mirror of REAL server-observed B4 workplace
 * arrivals. Independent of AV2A attribute deltas, wages, world events and
 * third-party speech. A failure blocks the evidence mirror, not the existing
 * authoritative B4.2B attendance record. This is deliberately not a skill XP
 * or actual completed-work ledger.
 *
 * Every successful write is an atomic, checksummed, per-person replacement;
 * a left-over pending file fails closed. Only the most recent 32 detailed
 * observations are retained; lifetime total and hash chain retain a compact
 * summary, NOT a complete lifetime forensic history.
 */
public final class KnownWorldAv2CExperienceJournal {
    private static final String SCHEMA = "knownworld.av2c.work_evidence.v1";
    private static final int MAX_BYTES = 24_000;
    private static final int CACHE_LIMIT = 64;
    private static MinecraftServer activeServer;
    private static Path root;
    private static final LinkedHashMap<String, JsonObject> CACHE =
        new LinkedHashMap<>(96, .75f, true);
    private static int newObservations, duplicates, rejected;

    private KnownWorldAv2CExperienceJournal() { }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            if (activeServer == s) {
                activeServer = null; root = null; CACHE.clear();
                newObservations = duplicates = rejected = 0;
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(Commands.literal("kwexperience")
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("person")
                    .then(Commands.argument("personId", StringArgumentType.string())
                        .executes(c -> inspect(c.getSource(),
                            StringArgumentType.getString(c, "personId")))))));
    }

    private static void bind(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("No Minecraft server");
        if (activeServer == server) return;
        CACHE.clear(); newObservations = duplicates = rejected = 0;
        activeServer = server;
        root = server.getWorldPath(LevelResource.ROOT)
            .resolve("knownworld").resolve("npc").resolve("av2c_verified_experiences_v1");
    }

    private static void safePath(Path p) throws IOException {
        if (Files.isSymbolicLink(p) || Files.isSymbolicLink(p.getParent()) ||
            Files.isSymbolicLink(root)) throw new IOException("AV2C symlink rejected");
    }

    private static Path directory(String siteId) { return root.resolve(siteId); }
    private static Path saved(String siteId, String id) {
        return directory(siteId).resolve(KnownWorldAv2CEventPolicy.sha256(id) + ".json");
    }
    private static Path pending(String siteId, String id) {
        return directory(siteId).resolve(KnownWorldAv2CEventPolicy.sha256(id) + ".pending");
    }

    private static String str(JsonObject o, String name) {
        JsonElement v = o.get(name);
        if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString())
            throw new IllegalStateException("Invalid AV2C string: " + name);
        return v.getAsString();
    }
    private static long nonnegative(JsonObject o, String key) {
        JsonElement value = o.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalStateException("Missing AV2C number: " + key);
        long n = value.getAsLong();
        if (n < 0) throw new IllegalStateException("Negative AV2C number: " + key);
        return n;
    }
    private static String checkPayload(JsonObject payload) {
        return KnownWorldAv2CEventPolicy.sha256(payload.toString());
    }

    private static JsonObject payload(String id, String site) {
        JsonObject p = new JsonObject();
        p.addProperty("schema", SCHEMA);p.addProperty("personId",id);p.addProperty("siteId",site);
        p.addProperty("total",0);p.addProperty("compacted",0);
        p.addProperty("chain",KnownWorldAv2CEventPolicy.sha256("AV2C_INITIAL_CHAIN"));
        p.add("recent", new JsonArray());
        return p;
    }

    private static void validate(JsonObject p, String site, String id) {
        if (!SCHEMA.equals(str(p,"schema")) || !id.equals(str(p,"personId")) ||
            !site.equals(str(p,"siteId"))) throw new IllegalStateException("AV2C identity mismatch");
        KnownWorldAv2CEventPolicy.validateSubject(site,id);
        long total = nonnegative(p,"total"), compacted = nonnegative(p,"compacted");
        String chain = str(p,"chain");
        if (chain.length() != 64 || !chain.matches("[0-9a-f]{64}"))
            throw new IllegalStateException("AV2C chain invalid");
        JsonArray recent = p.getAsJsonArray("recent");
        if (recent == null || recent.size() > KnownWorldAv2CEventPolicy.RECENT_LIMIT ||
            total != compacted + recent.size()) throw new IllegalStateException("AV2C count invariant");
        long previous=-1;
        for (JsonElement el : recent) {
            JsonObject row = el.getAsJsonObject();
            String eid = str(row,"id"), work = str(row,"workplace");
            if (!KnownWorldAv2CEventPolicy.KIND_WORK_PRESENCE.equals(str(row,"kind")) ||
                !KnownWorldAv2CEventPolicy.SOURCE_WORK_PRESENCE.equals(str(row,"source")))
                throw new IllegalStateException("AV2C provenance source mismatch");
            long day=nonnegative(row,"day");
            var proof=KnownWorldAv2CEventPolicy.observedAtWork(id,work,day);
            if (!proof.eventId().equals(eid) || day <= previous)
                throw new IllegalStateException("AV2C event ID/ordering mismatch");
            previous=day;
        }
    }

    private static JsonObject read(String site, String id) throws IOException {
        KnownWorldAv2CEventPolicy.validateSubject(site,id);
        Path f=saved(site,id), wait=pending(site,id);
        safePath(f); safePath(wait);
        if (Files.exists(wait, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Interrupted AV2C save: manual recovery needed");
        String key=id;
        JsonObject old=CACHE.get(key);
        if (old != null) return old.deepCopy();
        JsonObject p=payload(id,site);
        if (Files.exists(f,LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(f,LinkOption.NOFOLLOW_LINKS) || Files.size(f)>MAX_BYTES)
                throw new IOException("AV2C file type/size rejected");
            JsonObject wrapper=JsonParser.parseString(Files.readString(f,StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject loaded=wrapper.getAsJsonObject("payload");
            if (loaded==null || !checkPayload(loaded).equals(str(wrapper,"checksum")))
                throw new IOException("AV2C checksum mismatch");
            validate(loaded,site,id);p=loaded;
        }
        CACHE.put(key,p.deepCopy());
        while (CACHE.size()>CACHE_LIMIT) CACHE.remove(CACHE.keySet().iterator().next());
        return p;
    }

    private static void write(String site, String id, JsonObject value) throws IOException {
        validate(value,site,id);
        JsonObject wrapper=new JsonObject();
        wrapper.add("payload",value);wrapper.addProperty("checksum",checkPayload(value));
        byte[] bytes=(wrapper.toString()+"\n").getBytes(StandardCharsets.UTF_8);
        if (bytes.length>MAX_BYTES) throw new IOException("AV2C record size cap reached");
        Path f=saved(site,id), wait=pending(site,id);
        safePath(f);safePath(wait);
        Files.createDirectories(f.getParent());
        try (FileChannel ch=FileChannel.open(wait,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
            ByteBuffer b=ByteBuffer.wrap(bytes);
            while(b.hasRemaining()) ch.write(b);
            ch.force(true);
        }
        try {Files.move(wait,f,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch (AtomicMoveNotSupportedException e) {
            throw new IOException("AV2C atomic save not available; pending kept",e);
        }
        CACHE.put(id,value.deepCopy());
        while (CACHE.size()>CACHE_LIMIT) CACHE.remove(CACHE.keySet().iterator().next());
    }

    /**
     * Called ONLY when B4.2BPersonalEvents accepts newly observed, physically
     * arrived work-shift presence. Does not credit attendance back to B4,
     * award skills, grant wages or create objective world events.
     */
    public static void observedPhysicalWork(MinecraftServer server, String personId,
                                            String workplaceId, long campaignDay) {
        try {
            var proof=KnownWorldAv2CEventPolicy.observedAtWork(personId,workplaceId,campaignDay);
            bind(server);
            JsonObject p=read(proof.siteId(),proof.personId());
            JsonArray records=p.getAsJsonArray("recent");
            long total=nonnegative(p,"total");
            if (records.size()>0 && nonnegative(records.get(records.size()-1).getAsJsonObject(),"day")>=campaignDay) {
                duplicates++; return;
            }
            JsonObject e=new JsonObject();
            e.addProperty("id",proof.eventId());e.addProperty("day",proof.campaignDay());
            e.addProperty("workplace",proof.workplaceId());
            e.addProperty("kind",proof.kind());e.addProperty("source",proof.source());records.add(e);
            p.addProperty("total",Math.addExact(total,1));
            p.addProperty("chain",KnownWorldAv2CEventPolicy.sha256(str(p,"chain")+":"+proof.eventId()));
            if (records.size()>KnownWorldAv2CEventPolicy.RECENT_LIMIT) {
                JsonArray remaining=new JsonArray();
                for(int i=1;i<records.size();i++)remaining.add(records.get(i).deepCopy());
                p.add("recent",remaining);
                p.addProperty("compacted",Math.addExact(nonnegative(p,"compacted"),1));
            }
            write(proof.siteId(),proof.personId(),p);
            newObservations++;
        } catch (Exception ex) {
            rejected++;
            // The legacy B4 event remains its own authoritative record.
            // Do not poison the player tick or make false assertions about a save.
            KnownWorld.LOGGER.error("AV2C evidence mirror rejected observed work; no profile/economy mutation",ex);
        }
    }

    /** Conservative UI readout: evidence, not simulated outputs or skill mastery. */
    public static void enrich(JsonObject sheet, String site, String personId) {
        try {
            NpcSimulation sim=NpcSimulation.getNullable();
            if(sim==null) {sheet.addProperty("av2cEvidenceStatus","SERVER_UNAVAILABLE");return;}
            bind(sim.server());
            JsonObject p=read(site,personId);
            long total=nonnegative(p,"total");
            sheet.addProperty("av2cEvidenceStatus",total==0?"NO_RECORDED_EVIDENCE":"VERIFIED_PHYSICAL_PRESENCE");
            sheet.addProperty("av2cWorkPresenceDays",total);
            sheet.addProperty("av2cCompactedEvidenceCount",nonnegative(p,"compacted"));
            JsonArray recent=p.getAsJsonArray("recent");
            if(recent.size()>0) {
                JsonObject last=recent.get(recent.size()-1).getAsJsonObject();
                sheet.addProperty("av2cLastVerifiedWorkDay",nonnegative(last,"day"));
                sheet.addProperty("av2cLastWorkplace",str(last,"workplace"));
            }
        } catch (Exception ex) {
            sheet.addProperty("av2cEvidenceStatus","BLOCKED_INVALID_OR_PENDING_EVIDENCE");
            // Do not publish unverified counts or dates.
        }
    }

    public static String promptFor(String personId) {
        try {
            NpcSimulation sim=NpcSimulation.getNullable();
            if(sim==null)return "";
            bind(sim.server());
            String site=KnownWorldAv2CEventPolicy.siteOf(personId);
            JsonObject p=read(site,personId);
            long total=nonnegative(p,"total");
            if(total==0)return "";
            JsonArray recent=p.getAsJsonArray("recent");
            if(recent.isEmpty())return "";
            JsonObject last=recent.get(recent.size()-1).getAsJsonObject();
            return "\nSERVER-VERIFIED PERSONAL EXPERIENCE: physically observed at an assigned " +
                "workplace on " + total + " distinct recorded campaign days, last on day " +
                nonnegative(last,"day") + ". This is presence ONLY, not proof of labor output, " +
                "wages, skill improvement, meetings or gossip.\n";
        } catch (Exception ex) {
            return "\nPersonal experience evidence unavailable or unverified; do not claim specific work history.\n";
        }
    }

    private static int status(CommandSourceStack src) {
        bind(src.getServer());
        say(src,"[AV2C] Verified workplace-presence evidence MIRROR active; session written="+
            newObservations+" duplicate="+duplicates+" rejected="+rejected+
            ". Does not create memories, skills, jobs, wages, inventory or NPC identities.");
        return 1;
    }
    private static int inspect(CommandSourceStack src,String id) {
        try {
            bind(src.getServer());
            String site=KnownWorldAv2CEventPolicy.siteOf(id);
            JsonObject p=read(site,id);
            say(src,"[AV2C] "+id+" total="+nonnegative(p,"total")+
                " compacted="+nonnegative(p,"compacted")+
                " recent="+p.getAsJsonArray("recent").size()+
                " chain="+str(p,"chain").substring(0,16)+"... (provenance checksum)");
            return 1;
        } catch(Exception ex) {
            src.sendFailure(Component.literal("[AV2C] BLOCKED: "+ex.getMessage()));return 0;
        }
    }
    private static void say(CommandSourceStack src,String message) {
        src.sendSuccess(()->Component.literal(message),false);
    }
}
