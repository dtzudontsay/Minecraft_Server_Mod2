package dev.dtzudontsay.knownworld.simulation.attributes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB41Activation;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * B4.0-only AV4A adapter. Read-only character profiles, bounded per-day cache,
 * no new NpcId, save file, claims of observed meetings, skill awards or costs.
 * Only free-time navigation in B4.2 consumes this decision.
 */
public final class KnownWorldAv4Decisions {
    private static final int MAX_CACHE=768;
    private static final LinkedHashMap<String,KnownWorldAv4DecisionRules.Decision> CACHE=
            new LinkedHashMap<>(MAX_CACHE,0.75f,true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String,KnownWorldAv4DecisionRules.Decision> entry){
                    return size()>MAX_CACHE;
                }
            };
    private static long evaluated,blocked,household,publicLeisure,neighbor;
    private KnownWorldAv4Decisions() { }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{CACHE.clear();evaluated=blocked=household=publicLeisure=neighbor=0;});
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment)->
            dispatcher.register(Commands.literal("kwdecisions")
                .then(Commands.literal("status").executes(ctx->{
                    ctx.getSource().sendSuccess(()->Component.literal(
                        "[AV4] FREE-TIME DECISIONS ACTIVE; evaluated="+evaluated+
                        " blocked="+blocked+" home="+household+" public="+publicLeisure+
                        " neighbor-intent="+neighbor+" cache="+CACHE.size()+
                        ". Work schedules preserved; no relationship, wage, combat or memory mutations."),false);
                    return 1;
                }))
                .then(Commands.literal("preview")
                    .then(Commands.argument("personId",StringArgumentType.string())
                        .executes(ctx->{
                            String id=StringArgumentType.getString(ctx,"personId");
                            var sim=NpcSimulation.getNullable();
                            long day=sim==null?-1:sim.campaignCalendar().absoluteDay();
                            String site;
                            try{site=KnownWorldAv2DChangePolicy.siteOf(id);}catch(RuntimeException ex){site="";}
                            var result=choose(id,site,day);
                            ctx.getSource().sendSuccess(()->Component.literal(result==null?
                                "[AV4] Decision unavailable: requires visible B4 actor, healthy profile and campaign day.":
                                "[AV4] Evaluated preference for "+id+" day="+result.campaignDay()+
                                " => "+result.action()+" scores "+result.homeScore()+"/"+
                                result.publicScore()+"/"+result.neighborScore()+
                                ". Physical movement changes only in FREE_TIME."),false);
                            return result==null?0:1;
                        })))
                .then(Commands.literal("person")
                    .then(Commands.argument("personId",StringArgumentType.string())
                        .executes(ctx->{
                            String id=StringArgumentType.getString(ctx,"personId");
                            var d=CACHE.get(id);
                            ctx.getSource().sendSuccess(()->Component.literal(d==null?
                                "[AV4] No evaluated FREE_TIME decision for "+id+" yet. Wait for a projected resident's free-time period.":
                                "[AV4] "+id+" campaignDay="+d.campaignDay()+" preference="+d.action()+
                                " home="+d.homeScore()+" public="+d.publicScore()+
                                " neighbor="+d.neighborScore()+" (intent only, not completed encounter)"),false);
                            return d==null?0:1;
                        })))));
    }
    /** Called only during a genuine free-time phase, not on every simulation tick. */
    public static KnownWorldAv4DecisionRules.Decision choose(String personId,String siteId,long day) {
        if(day<0 || siteId==null || personId==null)return null;
        KnownWorldAv4DecisionRules.Decision existing=CACHE.get(personId);
        if(existing!=null && existing.campaignDay()==day)return existing;
        try {
            if(!siteId.equals(KnownWorldAv2DChangePolicy.siteOf(personId)))return null;
            JsonObject profile=KnownWorldB41Activation.av4ProfileFor(personId,siteId);
            if(profile==null || !"SHARED_DERIVED_MODEL_READ_ONLY".equals(text(profile,"av2bPsychology"))
                    || !"APPLIED_STARTING_PRIORS".equals(text(profile,"av3State"))
                    || "BLOCKED_UNVERIFIED_OR_PENDING_CHANGE_SAVE".equals(text(profile,"av2dChangeState"))) {
                blocked++;return null;
            }
            JsonObject personality=group(profile,"personality");
            JsonObject dispositions=group(profile,"dispositions");
            JsonObject values=group(profile,"characterValues");
            JsonObject norms=group(profile,"socialNorms");
            JsonObject tendencies=group(profile,"derivedTendencies");
            Map<String,Double> fields=new HashMap<>();
            fields.put("sociability",number(personality,"sociability",0,1));
            fields.put("empathy",(number(dispositions,"empathy",-1,1)+1)/2);
            fields.put("friendship_loyalty",(number(values,"friendship_loyalty",-1,1)+1)/2);
            fields.put("urban_norm",(number(norms,"urban",-1,1)+1)/2);
            fields.put("isolationist_norm",(number(norms,"isolationist",-1,1)+1)/2);
            for(String key:new String[]{"intervention","cultural_openness","peer_conformity","risk_taking"})
                fields.put(key,number(tendencies,key,0,1));
            var result=KnownWorldAv4DecisionRules.evaluate(
                KnownWorldAv4DecisionRules.read(fields),personId,day);
            CACHE.put(personId,result);evaluated++;
            switch(result.action()) {
                case HOUSEHOLD_ACTIVITY -> household++;
                case PUBLIC_LEISURE -> publicLeisure++;
                case NEIGHBOR_VISIT -> neighbor++;
            }
            return result;
        }catch(RuntimeException invalid){blocked++;return null;}
    }
    public static void enrich(JsonObject sheet,String personId) {
        if(sheet==null)return;
        var result=CACHE.get(personId);
        if(result==null){sheet.addProperty("av4DecisionStatus","NOT_EVALUATED_OUTSIDE_FREE_TIME");return;}
        JsonObject obj=new JsonObject();
        obj.addProperty("selection",result.action().name());
        obj.addProperty("campaignDay",result.campaignDay());
        obj.addProperty("homeScore",result.homeScore());
        obj.addProperty("publicScore",result.publicScore());
        obj.addProperty("neighborScore",result.neighborScore());
        obj.addProperty("authority","MOVEMENT_INTENT_ONLY_NOT_AN_OBSERVED_ENCOUNTER");
        sheet.add("av4FreeTimeDecision",obj);
        sheet.addProperty("av4DecisionStatus","CACHE_OF_LATEST_EVALUATED_FREE_TIME_DECISION");
    }
    private static String text(JsonObject obj,String name){
        JsonElement e=obj.get(name);return e!=null&&e.isJsonPrimitive()?e.getAsString():"";
    }
    private static JsonObject group(JsonObject root,String name){
        JsonObject result=root.getAsJsonObject(name);
        if(result==null)throw new IllegalArgumentException("Missing "+name);
        return result;
    }
    private static double number(JsonObject root,String name,double min,double max){
        JsonElement e=root.get(name);
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Missing "+name);
        double x=e.getAsDouble();
        if(!Double.isFinite(x)||x<min||x>max)throw new IllegalArgumentException("Invalid "+name);
        return x;
    }
}
