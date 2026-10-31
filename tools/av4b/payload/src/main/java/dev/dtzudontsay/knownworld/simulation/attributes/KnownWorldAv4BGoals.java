package dev.dtzudontsay.knownworld.simulation.attributes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB41Activation;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AV4B: visible B4 identity -> effective AV3/AV2 profile -> feasible goal -> B4.2 intent.
 * No new NPC registry, global scan, memory, wages, social events or save files.
 */
public final class KnownWorldAv4BGoals {
    private static final int MAX_CACHE=768;
    private record CachedGoal(KnownWorldAv4BGoalRules.Context context,
                         KnownWorldAv4BGoalRules.Decision plan) { }
    private static final LinkedHashMap<String,CachedGoal> CACHE=new LinkedHashMap<>(768,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,CachedGoal> entry) {
            return size()>MAX_CACHE;
        }
    };
    private static long evaluated,blocked;
    private KnownWorldAv4BGoals() { }
    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{CACHE.clear();evaluated=blocked=0;});
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->
            dispatcher.register(Commands.literal("kwgoals")
                .then(Commands.literal("status").executes(ctx->{
                    msg(ctx.getSource(),"[AV4B] Goal scoring active; evaluated="+evaluated+
                        " blocked="+blocked+" cached="+CACHE.size()+"/"+MAX_CACHE+
                        ". Visible B4 actors only; no economic/social event writes.");
                    return 1;
                }))
                .then(Commands.literal("person")
                    .then(Commands.argument("personId",StringArgumentType.string())
                        .executes(ctx->show(ctx.getSource(),
                            StringArgumentType.getString(ctx,"personId"),false))))
                .then(Commands.literal("explain")
                    .then(Commands.argument("personId",StringArgumentType.string())
                        .executes(ctx->show(ctx.getSource(),
                            StringArgumentType.getString(ctx,"personId"),true))))));
    }
    private static void msg(net.minecraft.commands.CommandSourceStack src,String value) {
        src.sendSuccess(()->Component.literal(value),false);
    }
    private static int show(net.minecraft.commands.CommandSourceStack src,String id,boolean verbose) {
        CachedGoal entry=CACHE.get(id);
        if(entry==null){msg(src,"[AV4B] No current goal for "+id+
            ". Find a projected B4 actor and allow 2 seconds for its routine scan.");return 0;}
        var d=entry.plan();
        msg(src,"[AV4B] "+id+" day="+d.campaignDay()+" phase="+d.activity()+
            " goal="+d.goal()+" authority="+d.authority());
        if(verbose)for(var g:KnownWorldAv4BGoalRules.Goal.values()) {
            Double score=d.scores().get(g);
            msg(src,"[AV4B] "+g+" -> "+(score==null?
                "EXCLUDED "+d.exclusions().getOrDefault(g,"NOT_AVAILABLE"):"score="+score));
        }
        return 1;
    }
    private static String text(JsonObject obj,String field) {
        JsonElement value=obj.get(field);
        return value!=null&&value.isJsonPrimitive()?value.getAsString():"";
    }
    private static JsonObject group(JsonObject root,String field) {
        JsonObject value=root.getAsJsonObject(field);
        if(value==null)throw new IllegalArgumentException("Missing AV4B profile field "+field);
        return value;
    }
    private static double number(JsonObject obj,String key,double min,double max) {
        JsonElement e=obj.get(key);
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Missing AV4B attribute "+key);
        double n=e.getAsDouble();
        if(!Double.isFinite(n)||n<min||n>max)throw new IllegalArgumentException("AV4B attribute out of range: "+key);
        return n;
    }
    private static double signed(JsonObject obj,String key) {
        return (number(obj,key,-1,1)+1.0)/2.0;
    }
    public static KnownWorldAv4BGoalRules.Decision choose(String id,String site,long day,
                                                           KnownWorldAv4BGoalRules.Context context) {
        if(id==null||site==null||context==null||day<0)return null;
        CachedGoal old=CACHE.get(id);
        if(old!=null&&old.plan().campaignDay()==day&&old.context().equals(context))return old.plan();
        try {
            if(!site.equals(KnownWorldAv2DChangePolicy.siteOf(id)) ||
               !site.equals(KnownWorldB41Activation.av2VisibleSite(id)))return null;
            JsonObject profile=KnownWorldB41Activation.av4ProfileFor(id,site);
            if(profile==null || !"SHARED_DERIVED_MODEL_READ_ONLY".equals(text(profile,"av2bPsychology")) ||
               !"APPLIED_STARTING_PRIORS".equals(text(profile,"av3State")) ||
               "BLOCKED_UNVERIFIED_OR_PENDING_CHANGE_SAVE".equals(text(profile,"av2dChangeState"))) {
                blocked++;return null;
            }
            JsonObject p=group(profile,"personality"),d=group(profile,"dispositions"),
                v=group(profile,"characterValues"),t=group(profile,"derivedTendencies");
            var in=new KnownWorldAv4BGoalRules.Inputs(
                number(p,"ambition",0,1),number(p,"sociability",0,1),number(p,"courage",0,1),
                signed(d,"discipline"),signed(d,"empathy"),signed(v,"duty"),
                signed(v,"wealth_seeking"),signed(v,"family_loyalty"),
                signed(v,"friendship_loyalty"),signed(v,"personal_freedom"),
                number(t,"risk_taking",0,1));
            var plan=KnownWorldAv4BGoalRules.evaluate(in,context,id,day);
            CACHE.put(id,new CachedGoal(context,plan));evaluated++;
            return plan;
        }catch(RuntimeException invalid){blocked++;return null;}
    }
    public static void enrich(JsonObject sheet,String id) {
        if(sheet==null)return;
        CachedGoal entry=CACHE.get(id);
        if(entry==null){sheet.addProperty("av4bGoalStatus","NOT_EVALUATED_ON_VISIBLE_PROJECTION");return;}
        var d=entry.plan();
        JsonObject obj=new JsonObject();
        obj.addProperty("selection",d.goal().name());
        obj.addProperty("campaignDay",d.campaignDay());
        obj.addProperty("schedulePhase",d.activity());
        obj.addProperty("authority",d.authority());
        JsonObject scores=new JsonObject(),excluded=new JsonObject();
        for(var g:KnownWorldAv4BGoalRules.Goal.values()) {
            Double n=d.scores().get(g);
            if(n==null)excluded.addProperty(g.name(),d.exclusions().getOrDefault(g,"NOT_AVAILABLE"));
            else scores.addProperty(g.name(),n);
        }
        obj.add("eligibleScores",scores);obj.add("excludedGoals",excluded);
        sheet.add("av4bGoal",obj);
        sheet.addProperty("av4bGoalStatus","BOUNDED_LIVE_INTENT_CACHE");
    }
    public static String promptFor(String id) {
        CachedGoal e=CACHE.get(id);
        if(e==null)return "\nNo verified AV4B autonomous goal has been evaluated for this resident yet.\n";
        var d=e.plan();
        return "\nSERVER AV4B CURRENT INTENTION (not an observed outcome): "+d.goal()+
            " during "+d.activity()+"; schedule day "+d.campaignDay()+
            ". This is only a scored choice or guarded travel intention. Do not claim the NPC actually traveled, "+
            "worked, earned wages, visited anyone, formed a relationship or gained memories based on this alone.\n";
    }
}
