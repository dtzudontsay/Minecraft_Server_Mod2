package dev.dtzudontsay.knownworld.simulation.attributes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB41Activation;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * AV2A: sparse saved overrides for canonical B4.0 projected people.
 *
 * B4.0 remains the one identity/census authority. The deterministic B4.1A.1
 * starting profile remains immutable. This overlay records only explicitly
 * verified changes to five already-defined numeric profile domains. It does
 * not allocate an NpcId, invent witnesses, simulate work or create household
 * inventories. The legacy CharacterProfileManager remains authoritative for
 * its own authored NpcIds; that reconciliation is a later AV2 sub-batch.
 *
 * All entry points run on the Minecraft server thread. The on-disk JSON is
 * per-site, write-through, checksummed and atomically published. An incomplete
 * pending file blocks further changes until a human investigates it.
 */
public final class KnownWorldAv2CharacterState {
    private static final String SCHEMA = "knownworld.av2a.person_delta.v1";
    private static final int MAX_SITE_BYTES = 8_000_000;
    private static final int MAX_PERSONS_PER_SITE = 12_000;
    private static final int CACHE_SITES = 6;

    private static final Map<String,String> FIELDS = Map.of(
        "CHARACTER_SKILL", "skills",
        "NATURAL_APTITUDE", "aptitudes",
        "DISPOSITION", "dispositions",
        "CHARACTER_VALUE", "characterValues",
        "SOCIAL_NORM", "socialNorms"
    );

    private static MinecraftServer activeServer;
    private static Path root;
    private static final LinkedHashMap<String,JsonObject> CACHED =
        new LinkedHashMap<>(8, 0.75f, true);
    private static int totalEdits;

    private KnownWorldAv2CharacterState() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(Commands.literal("kwprofile")
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("inspect")
                    .then(Commands.argument("personId", StringArgumentType.string())
                        .executes(c -> inspect(c.getSource(), StringArgumentType.getString(c,"personId")))))
                .then(Commands.literal("set")
                    .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                    .then(Commands.argument("personId", StringArgumentType.string())
                        .then(Commands.argument("domain", StringArgumentType.word())
                            .then(Commands.argument("attribute", StringArgumentType.word())
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(-1.0, 1.0))
                                    .executes(c -> set(c.getSource(),
                                        StringArgumentType.getString(c,"personId"),
                                        StringArgumentType.getString(c,"domain"),
                                        StringArgumentType.getString(c,"attribute"),
                                        DoubleArgumentType.getDouble(c,"value"))))))))
                .then(Commands.literal("clear")
                    .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                    .then(Commands.argument("personId", StringArgumentType.string())
                        .then(Commands.argument("domain", StringArgumentType.word())
                            .then(Commands.argument("attribute", StringArgumentType.word())
                                .executes(c -> clear(c.getSource(),
                                    StringArgumentType.getString(c,"personId"),
                                    StringArgumentType.getString(c,"domain"),
                                    StringArgumentType.getString(c,"attribute")))))))));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (server == activeServer) {
                CACHED.clear(); activeServer = null; root = null; totalEdits = 0;
            }
        });
    }

    private static void bind(MinecraftServer server) {
        Objects.requireNonNull(server, "Minecraft server");
        if (activeServer == server) return;
        // No dirty cache exists: every successful write was synchronously saved.
        CACHED.clear(); totalEdits=0; activeServer=server;
        root = server.getWorldPath(LevelResource.ROOT)
            .resolve("knownworld").resolve("npc").resolve("av2_character_deltas_v1");
    }

    private static void validateSubject(String siteId, String personId) {
        if (siteId == null || !siteId.matches("site_[a-z][a-z0-9_]{1,90}"))
            throw new IllegalArgumentException("Unsafe site identity");
        if (personId == null || personId.length() > 180 ||
            !personId.startsWith("b4:" + siteId + ":") ||
            !personId.matches("[A-Za-z0-9:_-]+"))
            throw new IllegalArgumentException("Person is not a canonical B4.0 site identity");
    }

    private static KnownWorldAv1Vocabulary.Definition definition(String domain, String input) {
        String d = domain == null ? "" : domain.toUpperCase(Locale.ROOT);
        if (!FIELDS.containsKey(d))
            throw new IllegalArgumentException("Domain is not a writable AV2 numeric domain: " + domain);
        KnownWorldAv1Vocabulary.Definition result = KnownWorldAv1Vocabulary.lookup(d, input);
        if (result == null || !result.typeResolvable() || !"ENUM".equals(result.kind()))
            throw new IllegalArgumentException("Not an authoritative canonical numeric attribute: " + d + "/" + input);
        return result;
    }

    private static void checkRange(String domain, double value) {
        if (!Double.isFinite(value) || value > 1.0 ||
            (("CHARACTER_SKILL".equals(domain) || "NATURAL_APTITUDE".equals(domain))
                ? value < 0.0 : value < -1.0))
            throw new IllegalArgumentException("Out-of-range " + domain + " value");
    }

    private static String jsonField(String domain) {
        return FIELDS.get(domain);
    }

    /** Existing B4.1A.1 projection has a singular language key for aptitudes. */
    private static String profileKey(String domain, String canonicalId) {
        if ("NATURAL_APTITUDE".equals(domain) && "LANGUAGES".equals(canonicalId))
            return "language";
        return canonicalId.toLowerCase(Locale.ROOT);
    }

    private static String digest(JsonObject changes) {
        try {
            byte[] sum=MessageDigest.getInstance("SHA-256")
                .digest(changes.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(sum);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256", e);
        }
    }

    private static Path file(String siteId) { return root.resolve(siteId + ".json"); }
    private static Path pending(String siteId) { return root.resolve(siteId + ".pending"); }

    private static JsonObject read(String siteId) {
        // Even a warm cache must not conceal an interrupted durability operation.
        if (root != null && (Files.exists(pending(siteId), LinkOption.NOFOLLOW_LINKS) ||
                            Files.isSymbolicLink(root) || Files.isSymbolicLink(file(siteId))))
            throw new IllegalStateException("Pending or linked AV2 site save blocks reads: " + siteId);
        JsonObject cached = CACHED.get(siteId);
        if (cached != null) return cached;
        Path saved = file(siteId);
        JsonObject changes = new JsonObject();
        try {
            if (Files.isSymbolicLink(root) || Files.isSymbolicLink(saved) ||
                Files.exists(pending(siteId), LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Unsafe link or pending AV2 save; manual inspection required");
            if (Files.exists(saved)) {
                if (Files.size(saved) > MAX_SITE_BYTES) throw new IOException("Oversized AV2 site save");
                JsonObject obj = JsonParser.parseString(Files.readString(saved, StandardCharsets.UTF_8)).getAsJsonObject();
                if (!SCHEMA.equals(string(obj, "schema")) || !siteId.equals(string(obj,"siteId")))
                    throw new IOException("AV2 schema/site mismatch");
                changes = obj.getAsJsonObject("changes");
                if (changes == null || !digest(changes).equals(string(obj,"checksum")))
                    throw new IOException("AV2 checksum mismatch");
                validateStored(siteId, changes);
            }
        } catch (Exception e) {
            throw new IllegalStateException("AV2 site profile overlay BLOCKED for " + siteId, e);
        }
        CACHED.put(siteId, changes);
        while (CACHED.size() > CACHE_SITES) {
            String oldest=CACHED.keySet().iterator().next(); CACHED.remove(oldest);
        }
        return changes;
    }

    private static String string(JsonObject o,String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString())
            throw new IllegalStateException("Missing string: " + key);
        return e.getAsString();
    }

    private static void validateStored(String site, JsonObject changes) {
        if (changes.size() > MAX_PERSONS_PER_SITE) throw new IllegalStateException("Too many AV2 edits in one site");
        for (var person : changes.entrySet()) {
            validateSubject(site, person.getKey());
            JsonObject domains=person.getValue().getAsJsonObject();
            for (var entry : domains.entrySet()) {
                if (!FIELDS.containsKey(entry.getKey())) throw new IllegalStateException("Unknown AV2 domain in save");
                JsonObject values=entry.getValue().getAsJsonObject();
                for (var value : values.entrySet()) {
                    var def=definition(entry.getKey(),value.getKey());
                    if (!def.canonicalId().equals(value.getKey()))
                        throw new IllegalStateException("Noncanonical AV2 stored key");
                    JsonObject change=value.getValue().getAsJsonObject();
                    checkRange(entry.getKey(), change.get("value").getAsDouble());
                    if (change.get("day").getAsLong() < 0 ||
                        !"ADMIN_VERIFIED_PROJECTION".equals(string(change,"source")))
                        throw new IllegalStateException("AV2 provenance/day invalid");
                }
            }
        }
    }

    /** Persist an entire site shard without writing to any NPC or economy save. */
    private static void write(String siteId, JsonObject changes) throws IOException {
        validateStored(siteId, changes);
        JsonObject obj=new JsonObject();
        obj.addProperty("schema",SCHEMA);
        obj.addProperty("siteId",siteId);
        obj.add("changes",changes);
        obj.addProperty("checksum",digest(changes));
        byte[] bytes=(obj.toString()+"\n").getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_SITE_BYTES) throw new IOException("AV2 site shard exceeds safe size");
        if (Files.isSymbolicLink(root)) throw new IOException("Symlinked AV2 directory");
        Files.createDirectories(root);
        Path temp=pending(siteId), target=file(siteId);
        if (Files.isSymbolicLink(temp) || Files.isSymbolicLink(target))
            throw new IOException("Symlinked AV2 target");
        // Pending is exclusive. An interrupted save refuses future writes.
        try (FileChannel out=FileChannel.open(temp,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
            ByteBuffer buf=ByteBuffer.wrap(bytes);
            while (buf.hasRemaining()) out.write(buf);
            out.force(true);
        }
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            throw new IOException("Atomic AV2 profile save unavailable; pending retained",e);
        }
    }

    private static long day() {
        NpcSimulation sim=NpcSimulation.getNullable();
        return sim==null ? 0 : Math.max(0,sim.campaignCalendar().absoluteDay());
    }

    /**
     * Called AFTER B4.1A.1 generates its immutable baseline; affects both the
     * right-click inspector and the K-key LLM context through the same path.
     */
    public static void applyToSheet(JsonObject sheet,String siteId,String personId) {
        validateSubject(siteId,personId);
        NpcSimulation sim=NpcSimulation.getNullable();
        if (sim==null) { sheet.addProperty("av2State","SERVER_UNAVAILABLE"); return; }
        bind(sim.server());
        try {
            JsonObject person=read(siteId).getAsJsonObject(personId);
            int applied=0;
            // Assemble the complete updated profile in a local copy. If any
            // dimension fails validation, the user's original view stays intact.
            JsonObject proposed=sheet.deepCopy();
            if (person != null) {
                for (var domain : person.entrySet()) {
                    String field=jsonField(domain.getKey());
                    JsonObject baseline=proposed.getAsJsonObject(field);
                    if (baseline == null) throw new IllegalStateException("Missing generated base profile: " + field);
                    for (var item : domain.getValue().getAsJsonObject().entrySet()) {
                        String key=profileKey(domain.getKey(),item.getKey());
                        if (!baseline.has(key)) throw new IllegalStateException("Missing B4 baseline axis " + field+"/"+key);
                        baseline.addProperty(key,item.getValue().getAsJsonObject().get("value").getAsDouble());
                        applied++;
                    }
                }
            }
            for (String field : FIELDS.values()) {
                JsonElement revised=proposed.get(field);
                if (revised != null) sheet.add(field,revised);
            }
            sheet.addProperty("av2State",applied==0?"IMMUTABLE_STARTING_PROFILE":"PERSISTED_SPARSE_PROFILE_DELTA");
            sheet.addProperty("av2ChangedDimensions",applied);
        } catch (RuntimeException e) {
            // Report a broken save rather than silently showing changed values.
            sheet.addProperty("av2State","BLOCKED_CORRUPT_OR_PENDING_SAVE");
            sheet.addProperty("av2ChangedDimensions",0);
        }
    }

    private static String siteForVisible(String id) {
        String site=KnownWorldB41Activation.av2VisibleSite(id);
        if (site==null) throw new IllegalArgumentException("Person is not an active, verified B4 projection. Visit the person first.");
        validateSubject(site,id);return site;
    }

    private static int status(CommandSourceStack src) {
        try {
            bind(src.getServer());
            KnownWorldAv1Vocabulary.Index index=KnownWorldAv1Vocabulary.index();
            say(src,"[AV2A] Sparse worldwide B4 profile overlay READY; "+index.definitions()+
                " AV1 definitions preserved; writable domains="+FIELDS.keySet().size()+
                "; cached site shards="+CACHED.size()+"; edits this session="+totalEdits);
            say(src,"[AV2A] No change to B4 identities, legacy NpcRegistry, population, economy, births or autonomous tick behavior.");
            return 1;
        } catch (Exception e) { return error(src,e); }
    }
    private static int inspect(CommandSourceStack src, String personId) {
        try {
            bind(src.getServer());
            String site=siteForVisible(personId);
            JsonObject person=read(site).getAsJsonObject(personId);
            if (person==null) say(src,"[AV2A] "+personId+": immutable baseline only (no saved changes)");
            else say(src,"[AV2A] "+personId+": "+person.toString());
            return 1;
        } catch (Exception e) { return error(src,e); }
    }
    private static int set(CommandSourceStack src,String personId,String domain,String axis,double value) {
        try {
            bind(src.getServer());
            String site=siteForVisible(personId);
            KnownWorldAv1Vocabulary.Definition def=definition(domain,axis);
            checkRange(def.domain(),value);
            JsonObject before=read(site);
            JsonObject next=before.deepCopy();
            JsonObject person=next.getAsJsonObject(personId);
            if (person==null) { person=new JsonObject(); next.add(personId,person); }
            JsonObject axes=person.getAsJsonObject(def.domain());
            if (axes==null) { axes=new JsonObject(); person.add(def.domain(),axes); }
            JsonObject entry=new JsonObject();
            entry.addProperty("value",value);entry.addProperty("day",day());
            entry.addProperty("source","ADMIN_VERIFIED_PROJECTION");
            axes.add(def.canonicalId(),entry);
            write(site,next);
            CACHED.put(site,next);totalEdits++;
            say(src,"[AV2A] SAVED "+personId+" "+def.domain()+":"+def.canonicalId()+"="+value+
                " at campaign day "+day()+"; use /kwprofile clear to revert to generated baseline.");
            return 1;
        } catch (Exception e) { return error(src,e); }
    }
    private static int clear(CommandSourceStack src,String personId,String domain,String axis) {
        try {
            bind(src.getServer());
            String site=siteForVisible(personId);
            KnownWorldAv1Vocabulary.Definition def=definition(domain,axis);
            JsonObject before=read(site), next=before.deepCopy();
            JsonObject person=next.getAsJsonObject(personId);
            if (person==null || !person.has(def.domain()) ||
                !person.getAsJsonObject(def.domain()).has(def.canonicalId())) {
                say(src,"[AV2A] No saved value to clear for "+personId+" "+def.domain()+":"+def.canonicalId());return 1;
            }
            JsonObject axes=person.getAsJsonObject(def.domain());axes.remove(def.canonicalId());
            if (axes.size()==0) person.remove(def.domain());
            if (person.size()==0) next.remove(personId);
            write(site,next);CACHED.put(site,next);totalEdits++;
            say(src,"[AV2A] SAVED CLEAR "+personId+" "+def.domain()+":"+def.canonicalId());
            return 1;
        } catch (Exception e) { return error(src,e); }
    }
    private static void say(CommandSourceStack src,String text) {
        src.sendSuccess(() -> Component.literal(text),false);
    }
    private static int error(CommandSourceStack src,Exception e) {
        src.sendFailure(Component.literal("[AV2A] BLOCKED: "+e.getMessage()));return 0;
    }
}
