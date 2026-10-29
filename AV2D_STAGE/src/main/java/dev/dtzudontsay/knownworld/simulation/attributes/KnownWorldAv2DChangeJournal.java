package dev.dtzudontsay.knownworld.simulation.attributes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB42BPersonalEvents;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * AV2D event-sourced character attribute changes. ONE per-person durable
 * receipt record owns all confirmed-event changes; AV2A owns separate manual
 * edits, B4.0 owns identity. No live gameplay system produces AV2D outcomes yet.
 *
 * Effective profile read order: B4 baseline -> AV2A manual overrides -> AV2D
 * confirmed-event deltas -> AV2B derived psychology. The bounded receipt log
 * is kept in full (up to 512). It is NEVER compacted in a way permitting replay.
 * A full log stops new writes until a versioned migration is introduced.
 */
public final class KnownWorldAv2DChangeJournal {
    private static final int MAX_FILE_BYTES = 290_000;
    private static final int CACHE_SIZE = 48;
    private static final LinkedHashMap<String,JsonObject> CACHE =
        new LinkedHashMap<>(64,.75f,true);
    private static MinecraftServer active;
    private static Path root;
    private static long accepted, replays, rejected;

    private KnownWorldAv2DChangeJournal() { }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (server == active) {
                CACHE.clear();root=null;active=null;accepted=replays=rejected=0;
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(Commands.literal("kwchanges")
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("person")
                    .then(Commands.argument("personId",StringArgumentType.string())
                        .executes(c -> inspect(c.getSource(),StringArgumentType.getString(c,"personId")))))
                .then(Commands.literal("reconcile")
                    .then(Commands.argument("personId",StringArgumentType.string())
                        .executes(c -> reconcile(c.getSource(),StringArgumentType.getString(c,"personId")))))));
    }
    private static void bind(MinecraftServer server) {
        if(server == null)throw new IllegalArgumentException("No active server");
        if(active == server)return;
        CACHE.clear();accepted=replays=rejected=0;active=server;
        root=server.getWorldPath(LevelResource.ROOT).resolve("knownworld").resolve("npc")
            .resolve("av2d_confirmed_changes_v1");
    }
    private static Path directory(String site) {return root.resolve(site);}
    private static Path file(String site,String id) {
        return directory(site).resolve(KnownWorldAv2DChangePolicy.sha256(id)+".json");
    }
    private static Path pending(String site,String id) {
        return directory(site).resolve(KnownWorldAv2DChangePolicy.sha256(id)+".pending");
    }
    private static void safe(Path location) throws IOException {
        if(Files.isSymbolicLink(root) || Files.isSymbolicLink(root.getParent()) ||
           Files.isSymbolicLink(location.getParent()) || Files.isSymbolicLink(location))
            throw new IOException("AV2D symlink rejected");
    }
    private static JsonObject initial(String site,String id) {
        JsonObject p=new JsonObject();p.addProperty("schema",KnownWorldAv2DChangePolicy.SCHEMA);
        p.addProperty("siteId",site);p.addProperty("personId",id);
        p.add("receipts",new JsonArray());return p;
    }
    private static String string(JsonObject obj,String key) {
        JsonElement e=obj.get(key);
        if(e==null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString())
            throw new IllegalStateException("Invalid AV2D string: "+key);
        return e.getAsString();
    }
    private static long natural(JsonObject obj,String key) {
        JsonElement e=obj.get(key);
        if(e==null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
            throw new IllegalStateException("Invalid AV2D whole number: "+key);
        long n;
        try {n=e.getAsBigDecimal().toBigIntegerExact().longValueExact();}
        catch(ArithmeticException ex) {throw new IllegalStateException("Nonintegral or oversized AV2D campaign day",ex);}
        if(n<0)throw new IllegalStateException("Negative AV2D campaign day");
        return n;
    }
    private static double finite(JsonObject obj,String key) {
        JsonElement e=obj.get(key);
        if(e==null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
            throw new IllegalStateException("Invalid AV2D numeric: "+key);
        double value=e.getAsDouble();if(!Double.isFinite(value))
            throw new IllegalStateException("Nonfinite AV2D delta");
        return value;
    }
    private static KnownWorldAv2DChangePolicy.Intent intentFrom(JsonObject row,String id) {
        return new KnownWorldAv2DChangePolicy.Intent(id,string(row,"domain"),
            string(row,"attribute"),finite(row,"delta"),natural(row,"day"),
            string(row,"authority"),string(row,"sourceEventId"));
    }
    private static void validate(JsonObject p,String site,String id) {
        if(!KnownWorldAv2DChangePolicy.SCHEMA.equals(string(p,"schema")) ||
           !site.equals(string(p,"siteId")) || !id.equals(string(p,"personId")))
            throw new IllegalStateException("AV2D schema/identity mismatch");
        if(!site.equals(KnownWorldAv2DChangePolicy.siteOf(id)))
            throw new IllegalStateException("AV2D cross-site record");
        JsonArray rows=p.getAsJsonArray("receipts");
        if(rows==null || rows.size()>KnownWorldAv2DChangePolicy.MAX_RECEIPTS_PER_PERSON)
            throw new IllegalStateException("AV2D receipt cap exceeded");
        Set<String> unique=new HashSet<>();long latest=-1;
        for(JsonElement raw:rows) {
            JsonObject row=raw.getAsJsonObject();
            KnownWorldAv2DChangePolicy.Intent intent=intentFrom(row,id);
            if(!intent.receiptId().equals(string(row,"receiptId")) ||
               !unique.add(intent.receiptId()) || intent.campaignDay()<latest)
                throw new IllegalStateException("AV2D receipt ID/ordering mismatch");
            requireCanonical(intent);latest=intent.campaignDay();
        }
    }
    private static void requireCanonical(KnownWorldAv2DChangePolicy.Intent intent) {
        var definition=KnownWorldAv1Vocabulary.lookup(intent.domain(),intent.attribute());
        if(definition==null || !"ENUM".equals(definition.kind()) ||
           !definition.typeResolvable() ||
           !intent.attribute().equals(definition.canonicalId()))
            throw new IllegalArgumentException("AV2D noncanonical attribute: "+intent.attribute());
    }
    private static JsonObject read(String site,String id) throws IOException {
        if(!site.equals(KnownWorldAv2DChangePolicy.siteOf(id)))
            throw new IllegalArgumentException("Cross-site AV2D read");
        Path f=file(site,id),w=pending(site,id);safe(f);safe(w);
        if(Files.exists(w,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Interrupted AV2D transaction; do not discard pending file");
        JsonObject cached=CACHE.get(id);if(cached!=null)return cached.deepCopy();
        JsonObject p=initial(site,id);
        if(Files.exists(f,LinkOption.NOFOLLOW_LINKS)) {
            if(!Files.isRegularFile(f,LinkOption.NOFOLLOW_LINKS) || Files.size(f)>MAX_FILE_BYTES)
                throw new IOException("AV2D file type/size rejected");
            JsonObject wrapper=JsonParser.parseString(Files.readString(f,StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject stored=wrapper.getAsJsonObject("payload");
            if(stored==null || !KnownWorldAv2DChangePolicy.sha256(stored.toString())
                .equals(string(wrapper,"checksum")))throw new IOException("AV2D checksum mismatch");
            validate(stored,site,id);p=stored;
        }
        CACHE.put(id,p.deepCopy());trimCache();return p;
    }
    private static void trimCache() {
        while(CACHE.size()>CACHE_SIZE)CACHE.remove(CACHE.keySet().iterator().next());
    }
    private static void write(String site,String id,JsonObject p) throws IOException {
        validate(p,site,id);
        JsonObject wrap=new JsonObject();wrap.add("payload",p);
        wrap.addProperty("checksum",KnownWorldAv2DChangePolicy.sha256(p.toString()));
        byte[] bytes=(wrap.toString()+"\n").getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_FILE_BYTES)throw new IOException("AV2D per-person file capacity reached");
        Path f=file(site,id),w=pending(site,id);safe(f);safe(w);
        Files.createDirectories(f.getParent());
        try(FileChannel out=FileChannel.open(w,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)) {
            ByteBuffer b=ByteBuffer.wrap(bytes);while(b.hasRemaining())out.write(b);out.force(true);
        }
        try {Files.move(w,f,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException ex) {
            throw new IOException("Atomic AV2D save unsupported; pending kept",ex);
        }
        CACHE.put(id,p.deepCopy());trimCache();
    }
    private static JsonObject receipt(KnownWorldAv2DChangePolicy.Intent intent) {
        JsonObject row=new JsonObject();row.addProperty("receiptId",intent.receiptId());
        row.addProperty("domain",intent.domain());row.addProperty("attribute",intent.attribute());
        row.addProperty("delta",intent.delta());row.addProperty("day",intent.campaignDay());
        row.addProperty("authority",intent.authority());row.addProperty("sourceEventId",intent.sourceEventId());
        return row;
    }

    /**
     * For a future TRUSTED event source in this same package. AV2D provides no
     * automatic producer. The verifier must re-read the canonical completed
     * event in its originating subsystem before returning true. NEVER pass a
     * constant-true verifier in gameplay; a forged sourceEventId is not proof.
     * A presence/schedule/dialogue event is not a completed action.
     */
    static synchronized boolean acceptVerifiedOutcome(
        MinecraftServer server, KnownWorldAv2DChangePolicy.Intent intent,
        KnownWorldAv2DChangePolicy.VerifiedOutcomeSource verifier) throws IOException {
        bind(server);requireCanonical(intent);
        if(verifier==null || !verifier.confirmsCompletedOutcome(intent)) {
            rejected++;throw new SecurityException("No authoritative completed-outcome proof");
        }
        String site=KnownWorldAv2DChangePolicy.siteOf(intent.personId());
        JsonObject p=read(site,intent.personId());JsonArray receipts=p.getAsJsonArray("receipts");
        for(JsonElement value:receipts) {
            JsonObject previous=value.getAsJsonObject();
            if(intent.receiptId().equals(string(previous,"receiptId"))) {
                if(!receipt(intent).equals(previous))
                    throw new IllegalStateException("AV2D conflicting duplicate receipt");
                replays++;return false;
            }
        }
        if(receipts.size()>=KnownWorldAv2DChangePolicy.MAX_RECEIPTS_PER_PERSON)
            throw new IllegalStateException("AV2D receipt capacity full: migration needed");
        if(receipts.size()>0 && intent.campaignDay()<
           natural(receipts.get(receipts.size()-1).getAsJsonObject(),"day"))
            throw new IllegalStateException("Out-of-order AV2D source event");
        JsonObject updated=p.deepCopy();updated.getAsJsonArray("receipts").add(receipt(intent));
        write(site,intent.personId(),updated);accepted++;return true;
    }

    /** Called after AV2A admin edits, BEFORE AV2B psychology; no new NpcState. */
    public static void applyToSheet(JsonObject sheet,String site,String id) {
        try {
            if("BLOCKED_CORRUPT_OR_PENDING_SAVE".equals(optional(sheet,"av2State")) ||
               "SERVER_UNAVAILABLE".equals(optional(sheet,"av2State")))
                throw new IllegalStateException("AV2A value authority unavailable");
            NpcSimulation sim=NpcSimulation.getNullable();
            if(sim==null)throw new IllegalStateException("Simulation unavailable");
            bind(sim.server());JsonObject p=read(site,id);
            JsonObject proposed=sheet.deepCopy();int count=0;
            for(JsonElement value:p.getAsJsonArray("receipts")) {
                var intent=intentFrom(value.getAsJsonObject(),id);
                JsonObject field=proposed.getAsJsonObject(KnownWorldAv2DChangePolicy.profileField(intent.domain()));
                String key=KnownWorldAv2DChangePolicy.profileKey(intent.domain(),intent.attribute());
                if(field==null || !field.has(key))
                    throw new IllegalStateException("B4 baseline axis missing: "+intent.domain()+"/"+key);
                double before=field.get(key).getAsDouble();
                field.addProperty(key,KnownWorldAv2DChangePolicy.applyBounded(intent.domain(),before,intent.delta()));
                count++;
            }
            for(String field:new String[]{"skills","aptitudes","dispositions","characterValues","socialNorms"}) {
                if(proposed.has(field))sheet.add(field,proposed.get(field).deepCopy());
            }
            sheet.addProperty("av2dChangeState",count==0?"NO_VERIFIED_OUTCOME_CHANGES":"VERIFIED_OUTCOME_RECEIPTS_APPLIED");
            sheet.addProperty("av2dConfirmedChanges",count);
        } catch(Exception ex) {
            // Do not render an uncertain profile as verified. Other layers can
            // still expose their own status, but this layer never publishes data.
            sheet.addProperty("av2dChangeState","BLOCKED_UNVERIFIED_OR_PENDING_CHANGE_SAVE");
            sheet.addProperty("av2dConfirmedChanges",0);
        }
    }
    private static String optional(JsonObject obj,String key) {
        JsonElement v=obj.get(key);return v==null?"":v.getAsString();
    }
    private static void say(CommandSourceStack source,String message) {
        source.sendSuccess(()->Component.literal(message),false);
    }
    private static int status(CommandSourceStack src) {
        bind(src.getServer());
        say(src,"[AV2D] Verified-outcome change ledger READY; no autonomous event producer installed."
            +" session accepted="+accepted+" replays="+replays+" refused="+rejected
            +"; per-person receipt cap="+KnownWorldAv2DChangePolicy.MAX_RECEIPTS_PER_PERSON);
        say(src,"[AV2D] B4 presence does NOT earn XP; AV2A manual edits unchanged; event source proof required.");
        return 1;
    }
    private static int inspect(CommandSourceStack src,String id) {
        try {
            bind(src.getServer());String site=KnownWorldAv2DChangePolicy.siteOf(id);
            JsonObject p=read(site,id);JsonArray rows=p.getAsJsonArray("receipts");
            say(src,"[AV2D] "+id+": "+rows.size()+" validated attribute-change receipts; no invented XP");
            if(rows.size()>0) {
                JsonObject last=rows.get(rows.size()-1).getAsJsonObject();
                say(src,"[AV2D] last event "+string(last,"receiptId")+" source="+string(last,"authority")
                    +" axis="+string(last,"domain")+":"+string(last,"attribute")
                    +" delta="+finite(last,"delta"));
            }
            return 1;
        } catch(Exception ex) {src.sendFailure(Component.literal("[AV2D] BLOCKED: "+ex.getMessage()));return 0;}
    }
    /** Advisory only: the B4 and AV2C files are not jointly transactional. */
    private static int reconcile(CommandSourceStack src,String id) {
        try {
            bind(src.getServer());String site=KnownWorldAv2DChangePolicy.siteOf(id);
            read(site,id); // Also blocks on a damaged/pending AV2D receipt.
            JsonObject b4=new JsonObject();KnownWorldB42BPersonalEvents.enrich(b4,id);
            JsonObject av2c=new JsonObject();KnownWorldAv2CExperienceJournal.enrich(av2c,site,id);
            String state=optional(av2c,"av2cEvidenceStatus");
            if(state.startsWith("BLOCKED")||"SERVER_UNAVAILABLE".equals(state)) {
                say(src,"[AV2D] Source comparison unavailable: "+state+". No automatic recovery.");return 0;
            }
            JsonElement old=b4.get("verifiedWorkplaceVisits");
            long recent=av2c.has("av2cWorkPresenceDays")?av2c.get("av2cWorkPresenceDays").getAsLong():0;
            if(old==null) {
                say(src,"[AV2D] No B4 workplace-presence entry for "+id+
                    "; AV2C mirror count="+recent+". Missing old record is NOT proof of absence.");
            } else {
                long prior=old.getAsLong();
                String result=prior==recent?"COUNT_MATCH_NOT_ATOMIC":
                    prior>recent?"B4_AHEAD_OF_AV2C_MIRROR":"AV2C_AHEAD_OF_B4_JOURNAL";
                say(src,"[AV2D] "+id+" B4="+prior+" AV2C="+recent+" status="+result);
            }
            say(src,"[AV2D] READ-ONLY comparison. Never relabel presence as training, never delete .pending.");
            return 1;
        } catch(Exception ex) {src.sendFailure(Component.literal("[AV2D] RECONCILIATION BLOCKED: "+ex.getMessage()));return 0;}
    }
}
