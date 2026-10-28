package dev.dtzudontsay.knownworld.simulation.population;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.casestudy.WinterfellConversationPackets;
import dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerator;
import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldC411GlobalWorldgen;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * B4.2 — world-clock-authoritative, lazy offscreen daily routine foundation.
 * Reads ONLY the existing B4.0/C4.15 identities and planned workplaces.
 * Does not spawn anonymous NPCs, change jobs, create world facts or duplicate
 * Winterfell's authored 5,000 living NPCs. NEVER calls an LLM per resident.
 *
 * The 3.76m starting identities are virtual scheduled people. When asked for
 * their activity, it is derived from a durable campaign-day boundary, not
 * replayed as millions of mutable entities. Work output & observed memories
 * remain unimplemented pending the event/economy authoritative bridge.
 */
public final class KnownWorldB42DailyLife {
    private static final String ROOT="/data/knownworld/scenarios/agot_298_ac/world_daily_b4_2_v1/";
    private static final String VERSION="knownworld.b4_2.daily_worksites.v1";
    private static final int SCAN_EVERY_TICKS=40;
    private static final int MAX_CACHED_SITES=6;
    private record Site(String id, String file, int people, int workers, int worklots) { }
    private record Work(String id,String kind,int x,int z,int w,int d) { }
    private static Map<String,Site> sites;
    private static final Map<String, Map<String,Work>> workplaceCache=new LinkedHashMap<>(16,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,Map<String,Work>> e) {
            return size()>MAX_CACHED_SITES;
        }
    };
    private static final Map<String,java.util.List<Work>> publicPlaces = new HashMap<>();
    private static MinecraftServer activeServer;
    private static long firstDay=-1, lastDay=-1, cycle=0;
    private static int ticks;
    private static boolean loaded,unhealthy;
    private static String lastFailure="";
    private KnownWorldB42DailyLife() { }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(KnownWorldB42DailyLife::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{if(server==activeServer)reset();});
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->
            dispatcher.register(Commands.literal("kwdaily")
                .then(Commands.literal("status").executes(c->status(c.getSource())))
                .then(Commands.literal("nearby").executes(c->nearby(c.getSource())))
                .then(Commands.literal("attendance").executes(c->attendance(c.getSource())))));
    }

    private static void reset() {
        KnownWorldB42PhysicalRoutine.clear();publicPlaces.clear();
        sites=null;workplaceCache.clear();activeServer=null;firstDay=-1;lastDay=-1;
        cycle=0;ticks=0;loaded=false;unhealthy=false;lastFailure="";localMoveErrors=0;
        routeAttempts=atWork=unloadedWaypoints=noWalkableLanding=unassignedWorksites=routeOutsideBudget=0;
    }
    private static String str(JsonObject obj,String key) {
        JsonElement e=obj.get(key);return e==null||e.isJsonNull()?null:e.getAsString();
    }
    private static Map<String,Site> sites() {
        if(sites!=null)return sites;
        try(InputStream in=KnownWorldB42DailyLife.class.getResourceAsStream(ROOT+"manifest.json")) {
            if(in==null)throw new IllegalStateException("Missing B4.2 worksite manifest; run installer BAT");
            JsonObject data=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            if(!VERSION.equals(str(data,"schemaVersion")) || data.get("siteCount").getAsInt()!=248 ||
               data.get("peopleTotal").getAsInt()!=3760684 ||
               data.get("workersTotal").getAsInt()!=1880341 ||
               !data.get("fixedSourceIdentity").getAsBoolean() ||
               data.get("commutesArePathVerified").getAsBoolean() ||
               data.get("jobProductionImplemented").getAsBoolean())
                throw new IllegalStateException("B4.2 resource version/census/authority mismatch");
            Map<String,Site> found=new HashMap<>();
            for(JsonElement element:data.getAsJsonArray("sites")) {
                JsonObject j=element.getAsJsonObject();String id=str(j,"siteId"),file=str(j,"file");
                if(id==null||!id.matches("site_[a-z][a-z0-9_]{1,90}") ||
                   (file!=null && !file.equals(id+".json.gz")))
                    throw new IllegalStateException("Unsafe B4.2 site/file: "+id);
                Site site=new Site(id,file,j.get("people").getAsInt(),j.get("workers").getAsInt(),
                                   j.get("workplaceCount").getAsInt());
                if(found.putIfAbsent(id,site)!=null)throw new IllegalStateException("Duplicate site "+id);
            }
            if(found.size()!=248)throw new IllegalStateException("Incomplete B4.2 site manifest");
            sites=Map.copyOf(found);return sites;
        }catch(IOException e){throw new IllegalStateException("B4.2 manifest unreadable",e);}
    }
    private static Map<String,Work> works(String siteId) {
        Map<String,Work> prior=workplaceCache.get(siteId);if(prior!=null)return prior;
        Site site=sites().get(siteId);
        if(site==null||site.file==null)return Map.of();
        Map<String,Work> loadedWorks=new HashMap<>();
        try(InputStream in=KnownWorldB42DailyLife.class.getResourceAsStream(ROOT+site.file)) {
            if(in==null)throw new IllegalStateException("Missing worksite shard "+site.file);
            try(Reader reader=new InputStreamReader(new GZIPInputStream(in),StandardCharsets.UTF_8)) {
                JsonObject obj=JsonParser.parseReader(reader).getAsJsonObject();
                if(!siteId.equals(str(obj,"siteId")))throw new IllegalStateException("Wrong worksite ID");
                for(JsonElement e:obj.getAsJsonArray("workplaces")) {
                    JsonObject w=e.getAsJsonObject();String id=str(w,"id");
                    if(id==null||!id.startsWith(siteId+":"))throw new IllegalStateException("Foreign workplace");
                    Work item=new Work(id,str(w,"kind"),w.get("x").getAsInt(),w.get("z").getAsInt(),
                                       w.get("w").getAsInt(),w.get("d").getAsInt());
                    if(loadedWorks.putIfAbsent(id,item)!=null)throw new IllegalStateException("Duplicate workplace");
                }
                if(loadedWorks.size()!=site.worklots)throw new IllegalStateException("Workplace count mismatch");
            }
        }catch(IOException e){throw new IllegalStateException("Worksites cannot be read: "+siteId,e);}
        Map<String,Work> ready=Map.copyOf(loadedWorks);workplaceCache.put(siteId,ready);return ready;
    }
    private static long day() {
        NpcSimulation sim=NpcSimulation.getNullable();return sim==null?-1:sim.campaignCalendar().absoluteDay();
    }
    private static int year() {
        NpcSimulation sim=NpcSimulation.getNullable();return sim==null?298:sim.campaignCalendar().year();
    }
    private static int phase(ServerLevel level) {
        NpcSimulation sim = NpcSimulation.getNullable();
        if (sim == null) return 0;

        return Math.floorMod(
                sim.campaignCalendar().tickRemainder(),
                KnownWorldB42Schedule.DAY
        );
    }
    private static Path checkpoint(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("knownworld")
                     .resolve("daily_life_b4_2_v1.json");
    }
    private static void persist(MinecraftServer server) throws IOException {
        Path file=checkpoint(server);Files.createDirectories(file.getParent());
        JsonObject j=new JsonObject();j.addProperty("version",1);
        j.addProperty("firstDay",firstDay);j.addProperty("lastDay",lastDay);
        j.addProperty("daysAdvanced",cycle);j.addProperty("populationBaseline",3760684);
        j.addProperty("authoritativeClock","CAMPAIGN_CALENDAR");
        j.addProperty("simulationAuthority","SCHEDULE_ONLY_NO_AUTO_ECONOMY_OR_OBSERVED_MEMORIES");
        Path stage=Files.createTempFile(file.getParent(),"b42day-",".tmp");
        try {
            Files.writeString(stage,j.toString(),StandardCharsets.UTF_8);
            try { Files.move(stage,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException noAtomic) { Files.move(stage,file,StandardCopyOption.REPLACE_EXISTING); }
        } finally {Files.deleteIfExists(stage);}
    }
    private static void attach(MinecraftServer server,long current) throws IOException {
        Path file=checkpoint(server);
        if(Files.exists(file)) {
            JsonObject j=JsonParser.parseString(Files.readString(file,StandardCharsets.UTF_8)).getAsJsonObject();
            if(j.get("version").getAsInt()!=1||j.get("populationBaseline").getAsInt()!=3760684||
               !"CAMPAIGN_CALENDAR".equals(str(j,"authoritativeClock")))
               throw new IOException("Invalid B4.2 saved checkpoint; will not overwrite");
            firstDay=j.get("firstDay").getAsLong();lastDay=j.get("lastDay").getAsLong();
            cycle=j.get("daysAdvanced").getAsLong();
            if(firstDay<0||lastDay<firstDay||cycle<0||current<lastDay)
                throw new IOException("Corrupt/future daily checkpoint; will not overwrite");
        } else {
            firstDay=current;lastDay=current;cycle=0;persist(server);
        }
        loaded=true;
    }
    private static void tick(MinecraftServer server) {
        if(activeServer!=server){reset();activeServer=server;}
        if(unhealthy||++ticks%SCAN_EVERY_TICKS!=0)return;
        ServerLevel level=server.getLevel(Level.OVERWORLD);
        if(level==null||!(level.getChunkSource().getGenerator() instanceof KnownWorldChunkGenerator))return;
        long current=day();if(current<0)return;
        try {
            sites();
            if(!loaded)attach(server,current);
            if(current<lastDay)throw new IOException("Campaign day moved backward; daily life frozen");
            KnownWorldB42PhysicalRoutine.prune(current*KnownWorldB42Schedule.DAY+phase(level));
            if(current!=lastDay) {
                // Checkpoint once per day. No O(3.76 million) per-day tick scan.
                long previous=lastDay, oldCycle=cycle;
                lastDay=current;cycle+=(current-previous);
                try{persist(server);}catch(IOException ex){lastDay=previous;cycle=oldCycle;throw ex;}
            }
        }catch(Exception error){unhealthy=true;lastFailure=error.toString();KnownWorld.LOGGER.error("B4.2 daily-life checkpoint disabled (fail closed)",error);}
    }
    private static String siteFromId(String id) {
        if(id==null||!id.startsWith("b4:"))return null;
        int end=id.indexOf(':',3);
        if(end<0)return null;
        String result=id.substring(3,end);
        return result.matches("site_[a-z][a-z0-9_]{1,90}")?result:null;
    }
    private static KnownWorldB42Schedule.Snapshot routine(String personId,String occupation,int birthYear,
                                                              String siteId,String workId,int x,int z,int offsetX,int offsetZ) {
        if(unhealthy||!loaded)return null;
        Site site=sites().get(siteId);
        if(site==null||!Objects.equals(siteFromId(personId),siteId))return null;
        Work work=workId==null?null:works(siteId).get(workId);
        int length=work==null?0:Math.abs(work.x+work.w/2-x+offsetX)+Math.abs(work.z+work.d+2-z+offsetZ);
        ServerLevel world=activeServer.getLevel(Level.OVERWORLD);
        if(world==null)return null;
        return KnownWorldB42Schedule.evaluate(personId,occupation,birthYear,year(),day(),phase(world),
                                               firstDay,work!=null,Math.min(6000,length));
    }
    /** Stable personal frontage, never the same home doorway for seven residents. */
    private static int[] homeActivityPoint(String id,int homeX,int homeZ) {
        int h=id.hashCode()&0x7fffffff;
        return new int[]{homeX+((h%11)-5),homeZ+2+((h/11)%5)};
    }
    private static java.util.List<Work> publicPlaces(String site) {
        java.util.List<Work> old=publicPlaces.get(site);if(old!=null)return old;
        java.util.List<Work> found=new java.util.ArrayList<>();
        for(Work w:works(site).values()) {
            if(w.kind()==null)continue;
            String k=w.kind().toUpperCase(java.util.Locale.ROOT);
            if(k.contains("TAVERN")||k.contains("INN")||k.contains("MARKET")||
               k.contains("SCHOOL")||k.contains("TEMPLE")||k.contains("COURTYARD"))found.add(w);
        }
        // Cache only a bounded set of site references; these are shared geometry,
        // not new identity objects or offscreen per-NPC simulation state.
        publicPlaces.put(site,java.util.List.copyOf(found));
        return publicPlaces.get(site);
    }
    private static Work leisurePlace(String id,String site,int homeX,int homeZ,
                                    int ox,int oz,boolean child) {
        int hash=id.hashCode()&0x7fffffff;
        if(hash%4==0)return null; // Some residents genuinely stay home.
        Work selected=null;long best=Long.MAX_VALUE;
        for(Work place:publicPlaces(site)) {
            String kind=place.kind().toUpperCase(java.util.Locale.ROOT);
            if(child && !(kind.contains("SCHOOL")||kind.contains("MARKET")||
                           kind.contains("COURTYARD")))continue;
            int xx=place.x()+place.w()/2+ox,zz=place.z()+place.d()+2+oz;
            long distance=Math.abs(xx-homeX)+Math.abs(zz-homeZ);
            if(distance>120)continue;
            long weighted=distance+(Math.floorMod(hash+place.id().hashCode(),29));
            if(weighted<best){best=weighted;selected=place;}
        }
        return selected;
    }
    /**
     * Lazily inferred activity location on re-entry. The inference is NOT a
     * saved traversal log and cannot prove that an actual route was walked.
     */
    public static int[] virtualArrival(String id,String site,String workId,String job,
                                       int birthYear,int homeX,int homeZ,int ox,int oz) {
        try {
            var r=routine(id,job,birthYear,site,workId,homeX,homeZ,ox,oz);
            if(r==null)return homeActivityPoint(id,homeX,homeZ);
            Work work=workId==null?null:works(site).get(workId);
            if(work!=null && (r.activity().equals("MODELED_WORK_SHIFT") ||
                    r.activity().equals("COMMUTING_TO_WORK") ||
                    r.activity().equals("COMMUTING_HOME"))) {
                if(!r.activity().equals("MODELED_WORK_SHIFT")) {
                    int[] fort=KnownWorldC411GlobalWorldgen.b42FortificationForInspection(site);
                    int hx=homeActivityPoint(id,homeX,homeZ)[0];
                    int hz=homeActivityPoint(id,homeX,homeZ)[1];
                    KnownWorldB43TravelModel.Point workPoint=
                            new KnownWorldB43TravelModel.Point(work.x()+ox+work.w()/2,
                                    work.z()+oz+work.d()+2);
                    KnownWorldB43TravelModel.Point position=KnownWorldB43TravelModel.commute(
                            r.activity(),phase(activeServer.getLevel(Level.OVERWORLD)),r,
                            new KnownWorldB43TravelModel.Point(hx,hz),workPoint,fort);
                    return KnownWorldB43OffscreenTravel.reconcile(id,new int[]{position.x(),position.z()});
                }
                int h=id.hashCode()&0x7fffffff;
                return KnownWorldB43OffscreenTravel.reconcile(id,
                        new int[]{work.x()+ox+Math.max(2,Math.min(work.w()-2,work.w()/2+(h%7)-3)),
                        work.z()+oz+work.d()+1+((h/7)%4)});
            }
            if(r.activity().equals("FREE_TIME")||r.activity().equals("HOME_AND_FREE_TIME")||
               r.activity().equals("REST_DAY")) {
                Work leisure=leisurePlace(id,site,homeX,homeZ,ox,oz,year()-birthYear<16);
                if(leisure!=null)return KnownWorldB43OffscreenTravel.reconcile(id,
                        new int[]{leisure.x()+leisure.w()/2+ox,leisure.z()+leisure.d()+2+oz});
            }
        }catch(RuntimeException error){/* Fall back to this person's actual home. */}
        return KnownWorldB43OffscreenTravel.reconcile(id,homeActivityPoint(id,homeX,homeZ));
    }
    /**
     * Visible schedule: work, school/social visit, and separate household tasks.
     * The one-shot movement controller owns navigation. No teleport home snap,
     * no jobs applied to the underlying NpcSimulation registry.
     */
    public static void applyProjection(Villager villager,String personId,String siteId,String workId,
                                        String occupation,int birthYear,int homeX,int homeZ,
                                        int offsetX,int offsetZ) {
        if(villager==null||WinterfellConversationPackets.isConversationActor(villager.getUUID()))return;
        try {
            var r=routine(personId,occupation,birthYear,siteId,workId,
                          homeX,homeZ,offsetX,offsetZ);
            if(r==null)return;
            Work work=workId==null?null:works(siteId).get(workId);
            String activity=r.activity();
            boolean shift=activity.equals("MODELED_WORK_SHIFT")||activity.equals("COMMUTING_TO_WORK");
            int[] home=homeActivityPoint(personId,homeX,homeZ);
            int tx=home[0],tz=home[1];String role=activity;
            if(shift && work==null){unassignedWorksites++;return;}
            if(shift && work!=null) {
                int h=personId.hashCode()&0x7fffffff;
                int offset=Math.max(2,Math.min(work.w()-2,work.w()/2+(h%7)-3));
                tx=work.x()+offsetX+offset;
                // For open-air fields, people work *on* the plot; enclosed
                // work buildings use distributed doorway/inside positions.
                String kind=work.kind()==null?"":work.kind().toUpperCase(java.util.Locale.ROOT);
                boolean open=kind.contains("FARM")||kind.contains("GARDEN")||kind.contains("PASTURE")||
                             kind.contains("FOREST_CAMP");
                // Use a doorway approach until nearby, then pass through the
                // real C3 south-facing door to a staggered interior position.
                boolean closeToDoor=Math.hypot(villager.getX()-(work.x()+offsetX+work.w()/2),
                                  villager.getZ()-(work.z()+offsetZ+work.d()))<8;
                tz=work.z()+offsetZ+(open?Math.max(3,work.d()/2):
                             closeToDoor?Math.max(2,work.d()-3):work.d()+2);
                role="WORK:"+work.id();
            }else if(activity.equals("FREE_TIME")||activity.equals("HOME_AND_FREE_TIME")||
                     activity.equals("REST_DAY")) {
                Work leisure=leisurePlace(personId,siteId,homeX,homeZ,offsetX,offsetZ,
                                          year()-birthYear<16);
                if(leisure!=null){tx=leisure.x()+leisure.w()/2+offsetX;
                    tz=leisure.z()+leisure.d()+2+offsetZ;role="VISIT:"+leisure.id();}
                else {
                    role="HOUSEHOLD:"+personId;
                    // A small deterministic share of free-time actors physically
                    // visits another currently visible resident. This is a social
                    // *meeting intent*, not a witnessed friendship or memory.
                    if(Math.floorMod(personId.hashCode()+(int)day(),7)==0 &&
                       villager.level() instanceof ServerLevel local) {
                        Villager friend=null;double nearest=18*18;
                        for(Villager candidate:local.getEntitiesOfClass(Villager.class,
                                villager.getBoundingBox().inflate(18.0))) {
                            if(candidate==villager||!candidate.entityTags().contains("kwb41_actor"))continue;
                            double dist=villager.distanceToSqr(candidate);
                            if(dist>9 && dist<nearest){friend=candidate;nearest=dist;}
                        }
                        if(friend!=null) {
                            tx=(int)Math.floor(friend.getX())+2;
                            tz=(int)Math.floor(friend.getZ())+1;
                            role="VISIT_NEIGHBOR:"+friend.getUUID();
                        }
                    }
                }
            }
            int[] fort=KnownWorldC411GlobalWorldgen.b42FortificationForInspection(siteId);
            long when=day()*KnownWorldB42Schedule.DAY+phase((ServerLevel)villager.level());
            KnownWorldB42PhysicalRoutine.Result result=
                KnownWorldB42PhysicalRoutine.steer(villager,role,tx,tz,fort,when);
            if(result.requested())routeAttempts++;
            if(result.nearGoal() && shift)atWork++;
            if(result.nearGoal() && activity.equals("MODELED_WORK_SHIFT") && work!=null)
                KnownWorldB42BPersonalEvents.observedAtWork(activeServer,personId,work.id(),occupation,day());
            if(result.reason().equals("BLOCKED_OR_UNLOADED_PATH"))noWalkableLanding++;
            if(shift && Math.hypot(villager.getX()-tx,villager.getZ()-tz)>20)routeOutsideBudget++;
        }catch(RuntimeException problem){
            if(++localMoveErrors<=3)KnownWorld.LOGGER.warn("B4.2C NPC navigation: {}",problem.toString());
        }
    }
    private static int localMoveErrors;
    private static long routeAttempts,atWork,unloadedWaypoints,noWalkableLanding,unassignedWorksites,routeOutsideBudget;
    /** True only for a modeled work/commute window with a real assigned worksite. */
    public static boolean hasActiveWorkGoal(String id,String site,String work,String job,int birthYear,
                                            int x,int z,int offsetX,int offsetZ) {
        try {
            if(work==null||work.isBlank())return false;
            var r=routine(id,job,birthYear,site,work,x,z,offsetX,offsetZ);
            return r!=null && (r.activity().equals("COMMUTING_TO_WORK") ||
                               r.activity().equals("MODELED_WORK_SHIFT"));
        }catch(RuntimeException ignored){return false;}
    }
    private static int attendance(CommandSourceStack src) {
        say(src,"[B4.2A] Live path requests="+routeAttempts+" near work="+atWork+
            " unloaded steps="+unloadedWaypoints+" blocked landings="+noWalkableLanding+
            " no assigned worksite="+unassignedWorksites+" long-distance routes="+routeOutsideBudget);
        say(src,"[B4.2A] Counted projected attempts only; this is not proof of 3.76m physical attendance. No chunk forcing, teleport commute, or authoritative production.");
        return 1;
    }
    public static void enrichSheet(JsonObject sheet,String id,String siteId,String workplace,
                                   String occupation,int birthYear,int x,int z,int offsetX,int offsetZ) {
        try {
            KnownWorldB42Schedule.Snapshot r=routine(id,occupation,birthYear,siteId,workplace,x,z,offsetX,offsetZ);
            if(r==null)return;
            sheet.addProperty("dailyActivity",r.activity().replace('_',' '));
            sheet.addProperty("dailyWorkSchedule","start "+r.start()+" / end "+r.end()+" ticks");
            sheet.addProperty("modeledWorkdays",r.modeledWorkdays());
            sheet.addProperty("commuteTicksEstimate",r.commuteTicks());
            sheet.addProperty("dailyLifeAuthority",r.authority());
            sheet.addProperty("practiceGainEstimate",String.format(java.util.Locale.ROOT,"%.4f",r.practiceGain()));
            // Visible skill progression is a lazy, deterministic overlay. It never
            // mutates the authoritative NPC registry or promotes work to a world event.
            String skill=KnownWorldB42Schedule.primarySkill(occupation);
            JsonObject scores=sheet.getAsJsonObject("skills");
            if(skill!=null && scores!=null && scores.get(skill)!=null &&
               scores.get(skill).isJsonPrimitive() && scores.get(skill).getAsJsonPrimitive().isNumber()) {
                double base=scores.get(skill).getAsDouble();
                scores.addProperty(skill,Math.min(1.0,Math.max(0.0,base+r.practiceGain())));
            }
        }catch(RuntimeException ignored) { /* diagnostic only; no world corruption */ }
    }
    /** Read-only, explicitly provisional context; NPC dialogue cannot assert completed deeds. */
    public static String promptFor(String id,String workId,String job,int birthYear) {
        try {
            String site=siteFromId(id);if(site==null||!loaded||unhealthy)return "";
            Work work=workId==null?null:works(site).get(workId);
            ServerLevel world=activeServer.getLevel(Level.OVERWORLD);
            if(world==null)return "";
            KnownWorldB42Schedule.Snapshot r=KnownWorldB42Schedule.evaluate(id,job,birthYear,year(),day(),
                phase(world),firstDay,work!=null,0);
            return "\nSCHEDULE MODEL ONLY (not an observed or completed event): Current scheduled activity: "
                +r.activity()+". Modeled workdays since B4.2 activation: "+r.modeledWorkdays()
                +". Worksite proximity not supplied to dialogue. These are abstract schedules. Never claim physical completed labor, payment, "
                +"witnessed events, or a confirmed constructed workplace from this.\n"
                +KnownWorldB42BPersonalEvents.promptFor(id);
        }catch(RuntimeException ignored){return "";}
    }
    private static void say(CommandSourceStack source,String message) {
        source.sendSuccess(()->Component.literal(message),false);
    }
    private static int status(CommandSourceStack source) {
        if(unhealthy) {
            say(source,"[B4.2] ERROR: daily-life processing halted safely: "+lastFailure);
            return 0;
        }
        if(!loaded) {say(source,"[B4.2] Waiting for Known World campaign clock; "+(unhealthy?lastFailure:"not initialized"));return 0;}
        say(source,"[B4.2] Lazy offscreen schedules active | census 3,760,684 | sites "+sites().size()
            +" | first observed day "+firstDay+" | last committed day "+lastDay
            +" | advanced days "+cycle+" | cached workplace sites "+workplaceCache.size()
            +" | saved observed offscreen handoffs "+KnownWorldB43OffscreenTravel.count());
        say(source,"[B4.2] Jobs are provisional; no automated production, verified walkable commute, or invented memories. Winterfell V3 unchanged.");
        return 1;
    }
    private static int nearby(CommandSourceStack source) {
        ServerPlayer player;
        try{player=source.getPlayerOrException();}catch(Exception e){say(source,"[B4.2] Player required");return 0;}
        if(!(player.level() instanceof ServerLevel level))return 0;
        Villager near=null;double distance=36;
        for(Villager v:level.getEntitiesOfClass(Villager.class,player.getBoundingBox().inflate(6))) {
            if(!v.entityTags().contains("kwb41_actor"))continue;
            double d=player.distanceToSqr(v);
            if(d<distance){distance=d;near=v;}
        }
        if(near==null){say(source,"[B4.2] No nearby worldwide projected villager (within 6 blocks).");return 0;}
        var npc=KnownWorldB41Activation.dialogueIdentity(player,near);
        if(npc==null){say(source,"[B4.2] Nearby projection has no valid current identity.");return 0;}
        String result=promptFor(npc.id(),npc.workplace(),npc.occupation(),298-npc.startAge());
        say(source,"[B4.2] "+npc.name()+" | "+npc.occupation()+" | "+result.replace('\n',' '));
        return 1;
    }
}
