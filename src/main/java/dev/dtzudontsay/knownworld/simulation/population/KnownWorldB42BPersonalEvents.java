package dev.dtzudontsay.knownworld.simulation.population;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * B4.2B verified PERSONAL work-presence event journal. The existing economy's
 * InventoryManager remains the sole authority over real goods and coins.
 * This journal NEVER claims modeled offscreen attendance means completed work,
 * completed production, wages, illness, conversation, or skill mastery.
 *
 * One accepted event per person per campaign day; persisted independently of
 * physical entity loading. Fail-closed on corruption/write failures.
 */
public final class KnownWorldB42BPersonalEvents {
    private static final int MAX_PEOPLE=20000;
    private static final int VERSION=1;
    private record Person(String id,String workplace,String skill,long latestDay,long visits) { }
    private static final Map<String,Person> people=new HashMap<>();
    private static MinecraftServer active;
    private static Path file;
    private static boolean loaded,failed,dirty;
    private static String error="";
    private static long eventCount;
    private static int ticks;

    private KnownWorldB42BPersonalEvents() { }
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(KnownWorldB42BPersonalEvents::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(s==active)flush();});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{if(s==active)reset();});
        CommandRegistrationCallback.EVENT.register((d,r,e)->d.register(Commands.literal("kwprogress")
                .then(Commands.literal("status").executes(c->status(c.getSource())))));
    }
    private static void reset() {people.clear();active=null;file=null;loaded=failed=dirty=false;error="";eventCount=0;ticks=0;}
    private static void fail(String reason,Exception exception) {
        failed=true;error=reason+(exception==null?"":": "+exception);
        KnownWorld.LOGGER.error("B4.2B personal event journal disabled safely: {}",error,exception);
    }
    private static void bind(MinecraftServer server) {
        if(active==server&&(loaded||failed))return;
        if(active!=null&&active!=server)reset();
        active=server;
        file=server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("knownworld")
                .resolve("personal_work_b4_2b_v1.json");
        try {
            if(Files.isRegularFile(file)) {
                JsonObject data=JsonParser.parseString(Files.readString(file,StandardCharsets.UTF_8)).getAsJsonObject();
                if(data.get("version").getAsInt()!=VERSION ||
                        !"OBSERVED_WORKPLACE_PRESENCE_ONLY".equals(data.get("authority").getAsString()))
                    throw new IOException("Unknown journal version/authority");
                eventCount=data.get("events").getAsLong();
                if(eventCount<0)throw new IOException("Invalid event count");
                for(JsonElement e:data.getAsJsonArray("people")) {
                    JsonObject o=e.getAsJsonObject();String id=o.get("id").getAsString(),
                            job=o.get("workplace").getAsString(),skill=o.get("skill").getAsString();
                    long last=o.get("lastDay").getAsLong(),visits=o.get("visits").getAsLong();
                    if(!id.startsWith("b4:site_")||id.length()>175||last<0||visits<=0||visits>eventCount||
                            people.putIfAbsent(id,new Person(id,job,skill,last,visits))!=null)
                        throw new IOException("Invalid or duplicated person in event journal");
                }
                if(people.size()>MAX_PEOPLE)throw new IOException("Event journal capacity exceeded");
            }
            loaded=true;
        }catch(Exception ex){people.clear();fail("Existing work journal unreadable; will not overwrite",ex);}
    }
    private static void tick(MinecraftServer server) {
        if(server!=active||!loaded||failed)return;
        if(++ticks%200==0 && dirty)flush();
    }
    /** Called ONLY by observed server-side physical arrival within a work shift. */
    public static void observedAtWork(MinecraftServer server,String id,String workplace,String occupation,long day) {
        if(server==null||day<0||workplace==null||workplace.isBlank()||id==null||!id.startsWith("b4:site_"))return;
        bind(server);if(failed||!loaded)return;
        Person previous=people.get(id);
        if(previous!=null && day<=previous.latestDay())return;
        if(previous==null && people.size()>=MAX_PEOPLE) {
            fail("Journal capacity reached; no new event accepted",null);return;
        }
        String skill=KnownWorldB42Schedule.primarySkill(occupation);
        if(skill==null)skill="none";
        people.put(id,new Person(id,workplace,skill,day,previous==null?1:previous.visits()+1));
        eventCount++;dirty=true;
    }
    public static void enrich(JsonObject sheet,String id) {
        Person p=people.get(id);
        if(p==null)return;
        sheet.addProperty("verifiedWorkplaceVisits",p.visits());
        sheet.addProperty("verifiedPracticeSkill",p.skill());
        sheet.addProperty("recordedPracticeOpportunities",p.visits());
        sheet.addProperty("lastVerifiedWorkDay",p.latestDay());
        sheet.addProperty("verifiedEventAuthority","PHYSICAL_WORKPLACE_PRESENCE_NOT_PRODUCTION");
    }
    /** The LLM may discuss real observed workplace presence, NOT invented outputs. */
    public static String promptFor(String id) {
        Person p=people.get(id);
        if(p==null)return "";
        return "\nVERIFIED PERSONAL EXPERIENCE: observed at assigned workplace on "+p.visits()+
                " distinct campaign days (last day "+p.latestDay()+"). This is evidence of presence " +
                "only; do not invent goods produced, wages paid, injuries, or coworkers encountered.";
    }
    private static void flush() {
        if(!dirty||failed||file==null)return;
        try {
            Files.createDirectories(file.getParent());
            JsonObject data=new JsonObject();data.addProperty("version",VERSION);
            data.addProperty("authority","OBSERVED_WORKPLACE_PRESENCE_ONLY");
            data.addProperty("events",eventCount);
            JsonArray array=new JsonArray();
            for(Person p:people.values().stream().sorted(java.util.Comparator.comparing(Person::id)).toList()) {
                JsonObject o=new JsonObject();o.addProperty("id",p.id());o.addProperty("workplace",p.workplace());
                o.addProperty("skill",p.skill());o.addProperty("lastDay",p.latestDay());o.addProperty("visits",p.visits());
                array.add(o);
            }
            data.add("people",array);
            Path temp=Files.createTempFile(file.getParent(),"b42b-person-",".tmp");
            try {
                Files.writeString(temp,data.toString(),StandardCharsets.UTF_8);
                try {Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException ignored) {Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
            }finally{Files.deleteIfExists(temp);}
            dirty=false;
        }catch(Exception ex){fail("Unable to save events",ex);}
    }
    private static int status(CommandSourceStack src) {
        src.sendSuccess(()->Component.literal("[B4.2B] Observed workplace days="+eventCount+
                " | residents with verified presence="+people.size()+" | journal "+
                (failed?"BLOCKED: "+error:loaded?"HEALTHY":"WAITING")+
                ". No economic transfers or fictitious offscreen work are recorded."),false);
        return failed?0:1;
    }
}
