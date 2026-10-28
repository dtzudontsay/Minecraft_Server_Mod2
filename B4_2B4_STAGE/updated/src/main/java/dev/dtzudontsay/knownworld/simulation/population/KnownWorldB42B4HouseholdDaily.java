package dev.dtzudontsay.knownworld.simulation.population;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Worldwide indexed household daily economics. PLANS ONLY: no actual wallets,
 * no autonomous consumption or wages, no invented legal owner identities.
 * Actual economic mutations are restricted to InventoryManager native transfers.
 */
public final class KnownWorldB42B4HouseholdDaily {
    private static final String BASE="/data/knownworld/scenarios/agot_298_ac/world_household_daily_b4_2b4_v1/";
    private static final String SCHEMA="knownworld.b4_2b4.household_daily_plan.v1";
    private static JsonObject manifest;
    private static final Map<String,JsonObject> CACHE = new LinkedHashMap<>(64,0.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,JsonObject> entry) { return size()>48; }
    };
    private KnownWorldB42B4HouseholdDaily() { }
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(Commands.literal("kwfinance")
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("site")
                    .then(Commands.argument("siteId",StringArgumentType.word())
                        .executes(ctx -> site(ctx.getSource(),StringArgumentType.getString(ctx,"siteId")))))));
    }
    private static synchronized JsonObject manifest() {
        if(manifest!=null)return manifest;
        try(InputStream in=KnownWorldB42B4HouseholdDaily.class.getResourceAsStream(BASE+"manifest.json")) {
            if(in==null)throw new IllegalStateException("B4.2B.4 household source missing");
            JsonObject m=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            if(!SCHEMA.equals(m.get("schemaVersion").getAsString()) ||
                m.get("siteCount").getAsInt()!=248 || m.get("peopleTotal").getAsLong()!=3760684L ||
                m.get("householdsTotal").getAsLong()!=799343L || m.get("workersTotal").getAsLong()!=1880341L ||
                !m.get("publicStockNotDuplicated").getAsBoolean() ||
                m.get("dailyTransactionsActivated").getAsBoolean() ||
                m.get("actualGrainDebitedKg").getAsLong()!=0 || m.get("actualCoinDebitedUnits").getAsLong()!=0 ||
                !"HOUSEHOLD_TRANSFER_PLANNING_NATIVE_INVENTORY_ONLY".equals(m.get("authority").getAsString()))
                throw new IllegalStateException("Unsafe household economy manifest");
            if(m.getAsJsonArray("sites").size()!=248)throw new IllegalStateException("Household site manifest incomplete");
            manifest=m;return m;
        } catch(Exception e){throw new IllegalStateException("B4.2B.4 worldwide plan validation failed",e);}
    }
    private static int partition(String id) {
        try {
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8));
            return (((hash[0]&255)<<8) | (hash[1]&255))%16;
        } catch(Exception e){throw new IllegalStateException("No SHA-256 implementation",e);}
    }
    private static synchronized JsonObject shard(String site,String filename) {
        String key=site+"/"+filename;
        if(CACHE.containsKey(key))return CACHE.get(key);
        try(InputStream stream=KnownWorldB42B4HouseholdDaily.class.getResourceAsStream(BASE+key)){
            if(stream==null)return null;
            try(GZIPInputStream gzip=new GZIPInputStream(stream)) {
                JsonObject data=JsonParser.parseReader(new InputStreamReader(gzip,StandardCharsets.UTF_8)).getAsJsonObject();
                CACHE.put(key,data);return data;
            }
        }catch(Exception e){throw new IllegalStateException("Corrupt B4.2B.4 shard "+key,e);}
    }
    private static JsonObject household(String site,String id) {
        if(site==null || !site.matches("site_[a-z][a-z0-9_]{1,90}") || id==null || id.isBlank())return null;
        manifest();
        JsonObject data=shard(site,String.format(java.util.Locale.ROOT,"p%02d.json.gz",partition(id)));
        if(data==null)return null;
        JsonObject homes=data.getAsJsonObject("households");
        if(homes==null)return null;
        JsonElement row=homes.get(id);
        return row==null || !row.isJsonObject()?null:row.getAsJsonObject();
    }
    /** Adds genuinely indexed economic forecast only, never claims resources were credited. */
    public static void enrich(JsonObject sheet,String siteId,String householdId) {
        JsonObject row=household(siteId,householdId);
        if(row==null){sheet.addProperty("openingHouseholdAssetsStatus","UNRESOLVED");return;}
        sheet.addProperty("openingHouseholdAssetsStatus","PLANNED_NOT_CREDITED");
        sheet.addProperty("proposedOpeningFoodFromPublicKg",row.get("grainAllocationFromPublicKg").getAsLong());
        sheet.addProperty("proposedOpeningCoinTransferUnits",row.get("openingCoinTransferUnits").getAsLong());
        sheet.addProperty("estimatedDailyHouseholdWageUnits",row.get("proposedDailyWagesCoinUnits").getAsLong());
        sheet.addProperty("estimatedDailyGrainNeedGrams",row.get("dailyGrainNeedGrams").getAsLong());
        sheet.addProperty("nativeHouseholdOwnerVerified",false);
    }
    private static int status(CommandSourceStack src){
        try{JsonObject m=manifest();send(src,"[B4.2B.4] 248 site economic plans; "+m.get("indexedHouseholds").getAsInt()+
            " indexed households; "+m.get("unresolvedHouseholds").getAsInt()+" unresolved/deferred households.");
            send(src,"[B4.2B.4] Opening household grain TRANSFERS planned: "+m.get("proposedGrainTransfersKg").getAsLong()+
                " kg; coin TRANSFERS planned: "+m.get("proposedCoinTransfersUnits").getAsLong()+
                ". Actual transactions: 0. Auto wages/food: OFF.");return 1;
        }catch(Exception e){src.sendFailure(Component.literal("[B4.2B.4] "+e.getMessage()));return 0;}
    }
    private static int site(CommandSourceStack src,String id){
        try{
            if(!id.matches("site_[a-z][a-z0-9_]{1,90}"))throw new IllegalArgumentException("Unsafe site ID");
            for(JsonElement e:manifest().getAsJsonArray("sites")){
                JsonObject s=e.getAsJsonObject();if(!id.equals(s.get("siteId").getAsString()))continue;
                send(src,"[B4.2B.4] "+id+": "+s.get("people").getAsInt()+" people, "+s.get("indexedHouseholds").getAsInt()+
                  " indexed households, grain transfer "+s.get("householdGrainFromPublicKg").getAsLong()+" kg (remains public "+
                  s.get("publicGrainRemainingKg").getAsLong()+" kg), treasury transfer "+
                  s.get("householdCoinFromPublicUnits").getAsLong()+" (remains public "+s.get("publicCoinRemainingUnits").getAsLong()+"). NO TRANSACTIONS EXECUTED.");return 1;
            }
            throw new IllegalArgumentException("Unknown site ID");
        }catch(Exception e){src.sendFailure(Component.literal("[B4.2B.4] "+e.getMessage()));return 0;}
    }
    private static void send(CommandSourceStack src,String s){src.sendSuccess(()->Component.literal(s),false);}
}
