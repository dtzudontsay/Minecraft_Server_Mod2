package dev.dtzudontsay.knownworld.simulation.population;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Worldwide household/social-economic baseline over the existing B4.0 IDs.
 * INFERRED CLASS and PROVISIONAL coordinator are intentionally distinct from
 * EconomyRuntime money, inventory, legal titles and verified property owners.
 * Small LRU of immutable, compressed per-site partitions; no world scan at runtime.
 */
public final class KnownWorldB42B2EconomicStanding {
    private static final String BASE = "/data/knownworld/scenarios/agot_298_ac/world_household_economy_b4_2b2_v1/";
    private static final String SCHEMA = "knownworld.b4_2b2.worldwide_household_standing.v1";
    private static final int CACHE_LIMIT = 48;
    private static final Map<String,JsonObject> CACHE = new LinkedHashMap<>(32,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,JsonObject> entry) {
            return size()>CACHE_LIMIT;
        }
    };
    private static JsonObject manifest;
    private static int cacheMisses;

    private KnownWorldB42B2EconomicStanding() { }

    public static void register() {
        KnownWorldB42B3StartingStock.register();
        KnownWorldB42B4HouseholdDaily.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(Commands.literal("kwstanding")
                    .then(Commands.literal("status").executes(c -> status(c.getSource())))));
    }
    private static synchronized JsonObject manifest() {
        if (manifest!=null) return manifest;
        try(InputStream in=KnownWorldB42B2EconomicStanding.class.getResourceAsStream(BASE+"manifest.json")) {
            if(in==null)throw new IllegalStateException("Missing B4.2B.2 economic baseline; regenerate source resources");
            JsonObject m=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            if (!SCHEMA.equals(m.get("schemaVersion").getAsString())
                || m.get("siteCount").getAsInt()!=248
                || m.get("populationTotal").getAsInt()!=3760684
                || m.get("householdsTotal").getAsInt()!=799343
                || m.get("workersTotal").getAsInt()!=1880341
                || m.get("actualCoinCredited").getAsLong()!=0
                || m.get("actualGrainCreditedKg").getAsLong()!=0
                || !"READ_ONLY_BASELINE_NOT_ECONOMY_RUNTIME_STOCK".equals(m.get("authority").getAsString()))
                throw new IllegalStateException("Unverified economic manifest or illegal monetary activation");
            manifest=m;
            return m;
        }catch(Exception e){throw new IllegalStateException("Economic manifest validation failed",e);}
    }
    private static String text(JsonObject o,String k) {
        if(o==null)return null;
        JsonElement e=o.get(k);
        return e==null || e.isJsonNull()?null:e.getAsString();
    }
    private static int partition(String id) {return id.hashCode() & 15;}
    private static synchronized JsonObject shard(String site,String partitionId) {
        if(site==null || !site.matches("site_[a-z][a-z0-9_]{1,90}"))return null;
        manifest();
        String key=site+"/"+partitionId;
        if(CACHE.containsKey(key))return CACHE.get(key);
        String filename=BASE+site+"/"+partitionId;
        try(InputStream in=KnownWorldB42B2EconomicStanding.class.getResourceAsStream(filename)) {
            if(in==null)return null; // A genuinely empty partition.
            try(GZIPInputStream gzip=new GZIPInputStream(in)) {
                JsonObject result=JsonParser.parseReader(new InputStreamReader(gzip,StandardCharsets.UTF_8)).getAsJsonObject();
                CACHE.put(key,result);cacheMisses++;
                return result;
            }
        } catch(Exception e) {throw new IllegalStateException("Corrupt economic shard "+key,e);}
    }
    private static JsonObject entry(String site,String id,String group) {
        if(id==null || id.isBlank())return null;
        JsonObject partitionData=shard(site,String.format(java.util.Locale.ROOT,"p%02d.json.gz",partition(id)));
        if(partitionData==null)return null;
        JsonObject records=partitionData.getAsJsonObject(group);
        if(records==null)return null;
        JsonElement row=records.get(id);
        return row!=null && row.isJsonObject()?row.getAsJsonObject():null;
    }
    /**
     * One lookup for a live NPC. Never inserts a new profile or mutates the
     * real economic ledger. Safe to use in right-click and dialogue contexts.
     */
    public static void enrich(JsonObject sheet,String site,String household,String workplace,String personId) {
        JsonObject home=entry(site,household,"households");
        JsonObject job=entry(site,workplace,"workplaces");
        if(home==null) {
            sheet.addProperty("economicStanding","UNRESOLVED");
            sheet.addProperty("economicBasis","NO_VERIFIED_HOUSEHOLD_RECORD");
        } else {
            sheet.addProperty("economicStanding",text(home,"standing"));
            sheet.addProperty("householdProsperityIndex",home.get("prosperityIndex").getAsInt());
            sheet.addProperty("economicBasis","PROVISIONAL_298AC_NOT_REAL_CURRENCY");
        }
        String role=workplace==null || workplace.isBlank()?"NO_WORKPLACE_ASSIGNED":"ASSIGNED_WORKER";
        if(job!=null && personId!=null && personId.equals(text(job,"coordinatorPersonId")))
            role="PROVISIONAL_SHIFT_COORDINATOR";
        sheet.addProperty("employmentRole",role);
        sheet.addProperty("employerStatus",job==null?"NOT_VERIFIED":text(job,"employerStatus"));
        sheet.addProperty("workplaceLegalOwnerStatus",job==null?"NOT_VERIFIED":text(job,"legalOwnerStatus"));
        sheet.addProperty("economyAuthority","EXISTING_ECONOMY_RUNTIME_UNTOUCHED");
        KnownWorldB42B4HouseholdDaily.enrich(sheet,site,household);
    }
    public static String shortDescription(JsonObject sheet) {
        return "Economic standing: "+text(sheet,"economicStanding")+" (provisional) | " +
                "Work role: "+text(sheet,"employmentRole")+" | Legal owner: "+
                text(sheet,"workplaceLegalOwnerStatus")+
                " | Initial household assets: "+text(sheet,"openingHouseholdAssetsStatus");
    }
    private static int status(CommandSourceStack src) {
        try {
            JsonObject m=manifest();
            int workplaces=m.get("workplaceGroupsTotal").getAsInt();
            int cacheSize;
            int misses;
            synchronized(KnownWorldB42B2EconomicStanding.class){cacheSize=CACHE.size();misses=cacheMisses;}
            src.sendSuccess(() -> Component.literal("[B4.2B.2] 248 sites; 3,760,684 people; 799,343 household baselines; "
                +workplaces+" workplace staffing groups; "+cacheSize+" cached partitions ("+misses+" loads)."),false);
            src.sendSuccess(() -> Component.literal("[B4.2B.2] Economic classes and coordinators: provisional. "
                +"ACTUAL coin credited=0, grain credited=0, wages paid=0. InventoryManager remains the sole authority."),false);
            return 1;
        }catch(Exception ex) {
            src.sendFailure(Component.literal("[B4.2B.2] BLOCKED: "+ex.getMessage()));
            return 0;
        }
    }
}
