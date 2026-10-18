package dev.dtzudontsay.knownworld.simulation.casestudy;

import com.google.gson.*;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.*;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;
import dev.dtzudontsay.knownworld.simulation.npc.profile.*;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * OPT-IN / TEST-WORLD-ONLY physical projection of 5000 existing/candidate person records.
 * The NpcRegistry owns person identity. Entities are disposable projections, never population.
 * The geographic marker coordinates are not written: layout is relative to the player's TEST origin.
 * Basic road/house terrain fitting uses WORLD_SURFACE, not worldgen elevation assumptions.
 */
public final class WinterfellV2 {
    public static final boolean ENABLED = true;
    public static final int FULL_ACTIVE_BUDGET = 128;
    public static final int REDUCED_ACTIVE_BUDGET = 128;
    public static final int FULL_RADIUS = 140;
    public static final int REDUCED_RADIUS = 260;
    public static final int BUILDER_RADIUS = 120;
    private static final String DATA_PATH = "/data/knownworld/case_study/winterfell_v2.json";
    private static final String STATE_NAME = "knownworld_winterfell_v2_test.json";
    private static final String ENTITY_MARKER = "kwf2_actor";
    private static final String ID_PREFIX = "kwf2_";

    private static Plan plan;
    private static State state;
    private static MinecraftServer currentServer;
    private static int ticks;
    private static final Map<String, UUID> physical = new HashMap<>();
    private static final Set<UUID> permittedLoads = new HashSet<>();
    private static final Map<String, Integer> homeGroundY = new HashMap<>();
    private static int recentFull, recentReduced;
    private static long lastTickDurationMs;

    private record Person(String id, String name, String sex, int birthYear,
                          String home, String household, String site, String job,
                          String religion, String culture, String status,
                          JsonObject raw, int index) { }
    private record Home(String id, String site, String template, int x, int z, int w, int d, int capacity, int occupants) { }
    private record Road(int x, int z) { }

    private static final class Plan {
        List<Person> people = new ArrayList<>();
        List<Home> homes = new ArrayList<>();
        List<Home> buildings = new ArrayList<>();
        List<Home> workplaces = new ArrayList<>();
        List<Road> roads = new ArrayList<>();
        Map<String, Person> peopleById = new HashMap<>();
        Map<String, Home> homesById = new HashMap<>();
        Map<String, String> householdByHome = new HashMap<>();
        List<String> householdIds = new ArrayList<>();
    }
    private static final class State {
        int anchorX, anchorZ;
        String dimension;
        boolean active, building;
        int importedCursor;
        Set<String> built = new HashSet<>();
        Set<Integer> roadsDone = new HashSet<>();
        Set<Integer> wallsDone = new HashSet<>();
        int castleWallStep;
        boolean keepBuilt;
        boolean parentageApplied;
        int keepSlice;
        int keepBaseY;
    }

    private WinterfellV2() { }

    public static void register() {
        if (!ENABLED) return;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("kwinterfell2")
                .then(Commands.literal("begin")
                    .then(Commands.literal("confirm")
                        .executes(c -> begin(c.getSource(),false)))
                    .then(Commands.literal("terrain")
                        .then(Commands.literal("confirm")
                            .executes(c -> begin(c.getSource(),true)))))
                .then(Commands.literal("pause").executes(c -> toggle(c.getSource(), false)))
                .then(Commands.literal("resume").executes(c -> toggle(c.getSource(), true)))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("build").executes(c -> toggleBuild(c.getSource(), true)))
                .then(Commands.literal("stopbuild").executes(c -> toggleBuild(c.getSource(), false)))
            );
        });
        ServerTickEvents.END_SERVER_TICK.register(WinterfellV2::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            if (currentServer == server) {
                physical.clear(); permittedLoads.clear(); homeGroundY.clear();
                plan = null; state = null; currentServer = null; ticks = 0;
            }
        });
        // Stale villagers from a preceding save/restart are discarded on load.
        // Newly created villagers are explicitly whitelisted BEFORE addFreshEntity.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!entity.entityTags().contains(ENTITY_MARKER)) return;
            if (!permittedLoads.remove(entity.getUUID())) {
                entity.discard();
            }
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND || !entity.entityTags().contains(ENTITY_MARKER))
                return InteractionResult.PASS;
            if (!level.isClientSide()) inspect(player, entity);
            return InteractionResult.SUCCESS;
        });
    }

    private static void loadPlan() {
        if (plan != null) return;
        Plan p = new Plan();
        try (InputStream stream = WinterfellV2.class.getResourceAsStream(DATA_PATH)) {
            if (stream == null) throw new IllegalStateException("Missing 5000-person V2 data. Run build_winterfell_v2.py");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray people = root.getAsJsonArray("residents");
            if (people.size() != 5000) throw new IllegalStateException("V2 population must equal 5000, not " + people.size());
            int idx = 0;
            for (JsonElement element : people) {
                JsonObject v = element.getAsJsonObject();
                Person x = new Person(get(v,"id"),get(v,"name"),get(v,"sex"),integer(v,"birthYear"),
                        get(v,"homeId"),get(v,"householdId"),get(v,"site"),get(v,"job"),
                        get(v,"religion"),get(v,"culture"),get(v,"status"),v,idx++);
                if (p.peopleById.putIfAbsent(x.id,x) != null) throw new IllegalStateException("Duplicated person " + x.id);
                p.people.add(x);
            }
            for (JsonElement element : root.getAsJsonArray("homes")) {
                JsonObject v=element.getAsJsonObject();
                Home h = new Home(get(v,"id"),get(v,"site"),get(v,"template"),integer(v,"x"),
                    integer(v,"z"),integer(v,"w"),integer(v,"d"),integer(v,"capacity"),integer(v,"occupants"));
                p.homes.add(h);p.buildings.add(h);p.homesById.put(h.id,h);
            }
            for (JsonElement element : root.getAsJsonArray("workplaces")) {
                JsonObject v=element.getAsJsonObject();
                Home h=new Home(get(v,"id"),get(v,"site"),get(v,"template"),integer(v,"x"),
                    integer(v,"z"),integer(v,"w"),integer(v,"d"),0,0);
                p.workplaces.add(h);p.buildings.add(h);
            }
            for (JsonElement element : root.getAsJsonArray("visitors")) {
                JsonObject v=element.getAsJsonObject();String id=get(v,"id");
                String name=switch(id) {
                    case "robert_baratheon"->"Robert Baratheon";
                    case "cersei_lannister"->"Cersei Lannister";
                    case "jaime_lannister"->"Jaime Lannister";
                    case "tyrion_lannister"->"Tyrion Lannister";
                    case "benjen_stark"->"Benjen Stark";
                    default->id;
                };
                // Visitors use castle guest location, but remain excluded from the 5000-person census.
                Person guest=new Person(id,name,"UNKNOWN",298,"castle_home_0001","visiting_royal_party",
                    "castle","VISITOR","authored","authored","SCENARIO_VISITOR_NOT_RESIDENT",v,-1);
                p.peopleById.put(id,guest);
            }
            for (JsonElement element : root.getAsJsonArray("roads")) {
                JsonArray row = element.getAsJsonArray();
                p.roads.add(new Road(row.get(0).getAsInt(),row.get(1).getAsInt()));
            }
            for (JsonElement element : root.getAsJsonArray("households")) {
                JsonObject v=element.getAsJsonObject();
                p.householdIds.add(get(v,"id"));
                p.householdByHome.put(get(v,"residence"),get(v,"id"));
            }
            if (p.homes.size()!=p.householdIds.size()) throw new IllegalStateException("Housing/household mismatch");
            plan = p;
            KnownWorld.LOGGER.info("Winterfell V2 plan loaded: {} residents / {} homes / {} road tiles",
                    p.people.size(),p.homes.size(),p.roads.size());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read Winterfell V2 plan",e);
        }
    }
    private static String get(JsonObject v, String key) {
        JsonElement value=v.get(key);return value==null||value.isJsonNull()?"":value.getAsString();
    }
    private static int integer(JsonObject v,String key){return v.get(key).getAsInt();}
    private static Path statePath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(STATE_NAME);
    }
    private static void loadState(MinecraftServer server) {
        if (currentServer == server) return;
        currentServer=server;physical.clear();permittedLoads.clear();homeGroundY.clear();
        plan=null;state=null;ticks=0;
        Path path=statePath(server);
        if (!Files.isRegularFile(path)) return;
        try {
            JsonObject v=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            State s=new State();
            s.anchorX=integer(v,"anchorX");s.anchorZ=integer(v,"anchorZ");
            s.dimension=get(v,"dimension");s.active=v.get("active").getAsBoolean();
            s.building=v.get("building").getAsBoolean();
            s.importedCursor=integer(v,"importedCursor");
            s.castleWallStep=integer(v,"castleWallStep");s.keepBuilt=v.get("keepBuilt").getAsBoolean();
            s.parentageApplied=v.has("parentageApplied")&&v.get("parentageApplied").getAsBoolean();
            s.keepSlice=v.has("keepSlice")?v.get("keepSlice").getAsInt():0;
            s.keepBaseY=v.has("keepBaseY")?v.get("keepBaseY").getAsInt():0;
            for (JsonElement e:v.getAsJsonArray("built"))s.built.add(e.getAsString());
            for (JsonElement e:v.getAsJsonArray("roadsDone"))s.roadsDone.add(e.getAsInt());
            if(v.has("wallsDone"))for(JsonElement e:v.getAsJsonArray("wallsDone"))s.wallsDone.add(e.getAsInt());
            state=s;
        } catch (Exception e) {
            KnownWorld.LOGGER.error("Winterfell V2 state file malformed; DO NOT regenerate a conflicting site.", e);
            state=null;
        }
    }
    private static void saveState() {
        if (state==null||currentServer==null)return;
        JsonObject v=new JsonObject();
        v.addProperty("anchorX",state.anchorX);v.addProperty("anchorZ",state.anchorZ);
        v.addProperty("dimension",state.dimension);v.addProperty("active",state.active);
        v.addProperty("building",state.building);v.addProperty("importedCursor",state.importedCursor);
        v.addProperty("castleWallStep",state.castleWallStep);v.addProperty("keepBuilt",state.keepBuilt);
        v.addProperty("parentageApplied",state.parentageApplied);
        v.addProperty("keepSlice",state.keepSlice);v.addProperty("keepBaseY",state.keepBaseY);
        JsonArray built=new JsonArray();for(String id:state.built)built.add(id);v.add("built",built);
        JsonArray roads=new JsonArray();for(int id:state.roadsDone)roads.add(id);v.add("roadsDone",roads);
        JsonArray walls=new JsonArray();for(int id:state.wallsDone)walls.add(id);v.add("wallsDone",walls);
        try {
            Path path=statePath(currentServer);Files.createDirectories(path.getParent());
            Path tmp=path.resolveSibling(path.getFileName()+".tmp");
            Files.writeString(tmp,v.toString(),StandardCharsets.UTF_8);
            try {Files.move(tmp,path,java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);} catch(java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(tmp,path,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch(IOException e){KnownWorld.LOGGER.error("Winterfell V2 state save failed",e);}
    }
    private static int begin(CommandSourceStack source, boolean permitNonFlat) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        if (!player.isCreative()) {message(source,"Creative mode required. Use a disposable TEST WORLD only.");return 0;}
        if (!player.level().isFlat() && !permitNonFlat) {
            message(source,"Not Superflat. For disposable terrain test ONLY: /kwinterfell2 begin terrain confirm");return 0;
        }
        MinecraftServer server=source.getServer();loadState(server);loadPlan();
        if (state!=null) {message(source,"Already initialized at "+state.anchorX+","+state.anchorZ+"; refusing to overwrite location.");return 0;}
        if (Files.exists(statePath(server))) {
            message(source,"Existing V2 state file is unreadable. Resolve it manually; refusing to overwrite a saved world layout.");return 0;
        }
        State s=new State();s.anchorX=player.blockPosition().getX();s.anchorZ=player.blockPosition().getZ();
        s.dimension=player.level().dimension().identifier().toString();s.active=true;s.building=true;
        state=s;saveState();
        message(source,"Winterfell V2 started. TEST WORLD ONLY. Importing 5000 persons incrementally; nearby houses/roads build as you explore. /kwinterfell2 status");
        return 1;
    }
    private static int toggle(CommandSourceStack source,boolean desired) {
        loadState(source.getServer());if(state==null){message(source,"No test started. Use /kwinterfell2 begin confirm in Creative.");return 0;}
        state.active=desired;saveState();message(source,"NPC projections "+(desired?"enabled":"paused"));return 1;
    }
    private static int toggleBuild(CommandSourceStack source,boolean desired) {
        loadState(source.getServer());if(state==null){message(source,"No test started");return 0;}
        state.building=desired;saveState();message(source,"Physical construction "+(desired?"enabled":"paused"));return 1;
    }
    private static int status(CommandSourceStack source) {
        loadState(source.getServer());loadPlan();if(state==null){message(source,"Winterfell V2 not initialized. Plan: 5000 / 1136 households. Run /kwinterfell2 begin confirm in a disposable Creative world.");return 0;}
        message(source,"Winterfell V2: "+state.importedCursor+"/5000 imported; built "+state.built.size()+"/"+plan.buildings.size()+" home/work plots, "+state.roadsDone.size()+"/"+plan.roads.size()+" road tiles; full "+recentFull+"/128, reduced "+recentReduced+"/128; last controller "+lastTickDurationMs+"ms.");
        return 1;
    }
    private static void message(CommandSourceStack s,String msg) {
        s.sendSuccess(()->Component.literal("[Winterfell V2] "+msg),false);
    }

    private static void tick(MinecraftServer server) {
        loadState(server);
        if(state==null)return;
        if(++ticks%20!=0)return;
        try {
            long t=System.nanoTime();loadPlan();
            if(state.importedCursor<plan.people.size()) importPeople(75);
            else {
                if(!state.parentageApplied) {
                    NpcSimulation sim=NpcSimulation.getNullable();
                    if(sim!=null){applyModeledParentage(sim);sim.save();state.parentageApplied=true;saveState();}
                }
                if(state.building) buildNearby(server);
                if(state.active) projectNearby(server);else clearProjections(server);
            }
            lastTickDurationMs=(System.nanoTime()-t)/1_000_000;
            if(ticks%200==0) saveState();
        } catch(Throwable t) {
            KnownWorld.LOGGER.error("Winterfell V2 tick failed; stopping projection/build to protect test world",t);
            state.active=false;state.building=false;saveState();
        }
    }
    private static void importPeople(int limit) {
        NpcSimulation sim=NpcSimulation.getNullable();if(sim==null)return;
        int processed=0;
        while(state.importedCursor<plan.people.size() && processed++<limit) {
            Person p=plan.people.get(state.importedCursor);
            NpcId id=sim.authoredIds().findNpc(p.id).orElse(null);
            if(id==null) {
                Home h=plan.homesById.get(p.home);
                SimulationPosition pos=SimulationPosition.overworld(state.anchorX+h.x+h.w/2.0,100,state.anchorZ+h.z+h.d+1);
                JsonObject pers=p.raw.getAsJsonObject("personality");
                NpcPersonality personality=new NpcPersonality(
                    field(pers,"courage"),field(pers,"ambition"),field(pers,"compassion"),
                    field(pers,"honor"),field(pers,"patience"),field(pers,"sociability"));
                String[] parts=p.name.split(" ",2);
                if("BOOK_NAMED_PROPOSAL".equals(p.status)){
                    if(p.name.equals("Maester Luwin"))parts=new String[]{"Luwin",""};
                    if(p.name.equals("Old Nan"))parts=new String[]{"Nan",""};
                }
                NpcState created=sim.registry().create(parts[0],parts.length==2?parts[1]:"",
                    "FEMALE".equals(p.sex)?NpcSex.FEMALE:NpcSex.MALE,p.birthYear,pos,personality);
                id=created.id();sim.authoredIds().registerNpc(p.id,id);
                CharacterProfile profile=sim.profiles().getOrCreate(id);
                profile.setCulture(p.culture);profile.setReligion(p.religion);
                profile.setEducation("northern_household");
                JsonArray traits=p.raw.getAsJsonArray("traits");
                if(traits!=null)for(JsonElement e:traits)profile.addTrait(e.getAsString());
                if(!"CHILD".equals(p.job)&&!"DEPENDENT".equals(p.job))profile.addOccupation(p.job.toLowerCase(Locale.ROOT));
                JsonObject skills=p.raw.getAsJsonObject("skills");
                if(skills!=null)for(Map.Entry<String,JsonElement> e:skills.entrySet()) {
                    try {profile.setSkill(CharacterSkill.valueOf(e.getKey().toUpperCase(Locale.ROOT)),e.getValue().getAsDouble());}
                    catch(IllegalArgumentException ignored) { }
                }
                Random seeded=new Random(2985000L ^ p.id.hashCode());
                for(CharacterAptitude aptitude:CharacterAptitude.values())profile.setAptitude(aptitude, .25+seeded.nextDouble()*.52);
                for(CharacterDisposition d:CharacterDisposition.values())profile.setDisposition(d,seeded.nextDouble()*1.2-.6);
                for(CharacterValue v:CharacterValue.values())profile.setCharacterValue(v,seeded.nextDouble()*1.1-.55);
                for(CharacterSocialNorm v:CharacterSocialNorm.values())profile.setSocialNorm(v,seeded.nextDouble()*1.1-.55);
            }
            // Move only TEST-world authored named residents into the explicitly chosen test castle.
            // This does not touch production world saves because /begin is opt-in.
            if("AUTHORED_EXISTING".equals(p.status)) {
                Home h=plan.homesById.get(p.home);
                sim.registry().move(id,SimulationPosition.overworld(state.anchorX+h.x+h.w/2.0,
                    100,state.anchorZ+h.z+h.d+1));
            }
            // Existing named characters retain their authored profiles and existing household.
            if(!"AUTHORED_EXISTING".equals(p.status)) {
                String hid=p.household;
                OrganizationId household=sim.authoredIds().findOrganization(hid).orElse(null);
                if(household==null) {
                    Organization org=sim.organizations().create("Winterfell Test " + hid,OrganizationType.HOUSEHOLD,null);
                    household=org.id();sim.authoredIds().registerOrganization(hid,household);
                }
                sim.affiliations().setHousehold(id,household);
                sim.authoredIds().findSettlement("winterfell").ifPresent(sid->sim.affiliations().setResidence(id,sid));
            }
            state.importedCursor++;
        }
        if(state.importedCursor%300==0||state.importedCursor==plan.people.size()) {
            sim.save();saveState();
            if(state.importedCursor==plan.people.size()) {
                applyModeledParentage(sim);
                sim.save();state.parentageApplied=true;saveState();
                KnownWorld.LOGGER.info("Winterfell V2 import completed: 5000 unique roster people; 9 reused; 4991 created if absent.");
            }
        }
    }
    private static void applyModeledParentage(NpcSimulation sim) {
        for(Person child:plan.people) {
            if(!"GENERATED_MOD_CALIBRATION".equals(child.status))continue;
            JsonObject raw=child.raw;
            String mother=get(raw,"motherId"),father=get(raw,"fatherId");
            if(mother.isBlank()&&father.isBlank())continue;
            NpcId childId=sim.authoredIds().findNpc(child.id).orElse(null);
            if(childId==null||sim.genealogy().parentsOf(childId).isPresent())continue;
            NpcId motherId=mother.isBlank()?null:sim.authoredIds().findNpc(mother).orElse(null);
            NpcId fatherId=father.isBlank()?null:sim.authoredIds().findNpc(father).orElse(null);
            if(motherId==null&&fatherId==null)continue;
            sim.genealogy().registerBirth(childId,motherId,fatherId,0L);
        }
    }
        private static double field(JsonObject p,String key) {if(p==null)return 0;JsonElement v=p.get(key);return v==null?0:v.getAsDouble();}

    private static ServerLevel dimension(MinecraftServer server) {
        if(state==null)return null;
        for(ServerLevel level:server.getAllLevels()) {
            if(level.dimension().identifier().toString().equals(state.dimension))return level;
        }
        return null;
    }
    private static boolean nearAnyPlayer(MinecraftServer server,int x,int z,double radius) {
        for(ServerPlayer p:server.getPlayerList().getPlayers()) {
            if(!p.level().dimension().identifier().toString().equals(state.dimension))continue;
            double dx=p.getX()-x,dz=p.getZ()-z;
            if(dx*dx+dz*dz<=radius*radius)return true;
        }
        return false;
    }
    private static void projectNearby(MinecraftServer server) {
        ServerLevel level=dimension(server);if(level==null)return;
        List<Person> nearest=new ArrayList<>();
        List<Person> candidates=new ArrayList<>(plan.people);
        for(String visitorId:List.of("robert_baratheon","cersei_lannister","jaime_lannister","tyrion_lannister","benjen_stark")) {
            Person guest=plan.peopleById.get(visitorId);if(guest!=null)candidates.add(guest);
        }
        for(Person person:candidates) {
            Home h=plan.homesById.get(person.home);
            int x=state.anchorX+h.x+h.w/2,z=state.anchorZ+h.z+h.d+1;
            if(!nearAnyPlayer(server,x,z,REDUCED_RADIUS))continue;
            if(!level.isLoaded(new BlockPos(x,64,z)))continue;
            nearest.add(person);
        }
        // Only nearest 256 among geographically eligible residents.
        nearest.sort(Comparator.comparingDouble(p -> distanceToPlayers(server,p)));
        Set<String> wants=new HashSet<>();int full=0,reduced=0;
        for(Person person:nearest) {
            // Only count people whose assigned physical residence already exists.
            // Otherwise unbuilt plots could consume all 128 slots and leave
            // a fully built neighboring street inexplicably empty.
            Home residence=plan.homesById.get(person.home);
            if(!state.built.contains(residence.id))continue;
            double dist=distanceToPlayers(server,person);
            boolean detailed=dist<=FULL_RADIUS*FULL_RADIUS && full<FULL_ACTIVE_BUDGET;
            boolean simplified=!detailed && reduced<REDUCED_ACTIVE_BUDGET;
            if(!detailed&&!simplified)continue;
            wants.add(person.id);
            if(detailed)full++;else reduced++;
            UUID uuid=physical.get(person.id);
            Entity ent=uuid==null?null:level.getEntity(uuid);
            if(ent instanceof Villager villager) {villager.setNoAi(!detailed);continue;}
            if(uuid!=null)physical.remove(person.id);
            Home h=plan.homesById.get(person.home);
            // Only built homes reach this part of the activation loop.
            int x=state.anchorX+h.x+h.w/2,z=state.anchorZ+h.z+h.d+1;
            int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
            Villager mob=new Villager(EntityTypes.VILLAGER,level);
            mob.snapTo(x+.5,y,z+.5,0.0F,0.0F);
            mob.setCustomName(Component.literal(person.name));mob.setCustomNameVisible(false);
            // Display minors as baby villagers in the visual-only projection.
            mob.setBaby(298-person.birthYear<15);
            mob.setPersistenceRequired();mob.setNoAi(!detailed);
            mob.addTag(ENTITY_MARKER);mob.addTag(ID_PREFIX+person.id);
            permittedLoads.add(mob.getUUID());physical.put(person.id,mob.getUUID());
            if(!level.addFreshEntity(mob)) {physical.remove(person.id);permittedLoads.remove(mob.getUUID());}
        }
        // One physical entity per ID. Destroy distant projections, retain NpcRegistry person.
        for(Iterator<Map.Entry<String,UUID>> it=physical.entrySet().iterator();it.hasNext();) {
            Map.Entry<String,UUID> e=it.next();
            if(wants.contains(e.getKey()))continue;
            Entity entity=level.getEntity(e.getValue());if(entity!=null)entity.discard();
            it.remove();
        }
        // Report actually spawned, live physical villagers, NOT theoretical slots.
        int liveFull=0,liveReduced=0;
        for(Map.Entry<String,UUID> e:physical.entrySet()) {
            Entity mob=level.getEntity(e.getValue());
            if(mob instanceof Villager v){if(v.isNoAi())liveReduced++;else liveFull++;}
        }
        recentFull=liveFull;recentReduced=liveReduced;
    }
    private static double distanceToPlayers(MinecraftServer server,Person person) {
        Home h=plan.homesById.get(person.home);
        double x=state.anchorX+h.x+h.w/2.0,z=state.anchorZ+h.z+h.d+1.0;
        double best=Double.POSITIVE_INFINITY;
        for(ServerPlayer p:server.getPlayerList().getPlayers()) {
            if(!p.level().dimension().identifier().toString().equals(state.dimension))continue;
            double dx=p.getX()-x,dz=p.getZ()-z;best=Math.min(best,dx*dx+dz*dz);
        }
        return best;
    }
    private static void clearProjections(MinecraftServer server) {
        ServerLevel level=dimension(server);if(level==null)return;
        for(UUID id:physical.values()){Entity e=level.getEntity(id);if(e!=null)e.discard();}
        physical.clear();recentFull=0;recentReduced=0;
    }
    private static void inspect(net.minecraft.world.entity.player.Player player,Entity entity) {
        if(plan==null)return;
        for(String tag:entity.entityTags())if(tag.startsWith(ID_PREFIX)){
            Person p=plan.peopleById.get(tag.substring(ID_PREFIX.length()));if(p==null)return;
            NpcSimulation sim=NpcSimulation.getNullable();
            NpcId id=sim==null?null:sim.authoredIds().findNpc(p.id).orElse(null);
            if(id==null)return;
            CharacterProfile cp=sim.profiles().getOrCreate(id);
            player.sendSystemMessage(Component.literal("§6[Winterfell] " + p.name + " §7#"+id+" / "+p.status));
            NpcState real=sim.registry().find(id).orElse(null);
            int age=real==null?Math.max(0,298-p.birthYear):Math.max(0,298-real.identity().birthYear());
            player.sendSystemMessage(Component.literal("Age "+age+" | "+p.job+" | " +cp.culture()+" | "+cp.religion()));
            player.sendSystemMessage(Component.literal("Home "+p.home+" | Household "+p.household+" | Lord: Eddard Stark"));
            player.sendSystemMessage(Component.literal("Traits: "+cp.traits().stream().limit(5).toList()));
            player.sendSystemMessage(Component.literal("Skills: "+cp.skills().entrySet().stream()
                .sorted((a,b)->Double.compare(b.getValue(),a.getValue())).limit(4).toList()));
            player.sendSystemMessage(Component.literal("Persona persists in authoritative NpcRegistry; body is a proximity projection."));
            return;
        }
    }

    private static void buildNearby(MinecraftServer server) {
        ServerLevel level=dimension(server);if(level==null)return;
        int homes=0;
        // Terrain-aware, incremental placement. Maximum one plot per controller step.
        for(Home home:plan.buildings) {
            if(state.built.contains(home.id))continue;
            int x=state.anchorX+home.x,z=state.anchorZ+home.z;
            if(!nearAnyPlayer(server,x+home.w/2,z+home.d/2,BUILDER_RADIUS))continue;
            if(!level.isLoaded(new BlockPos(x,64,z))||!level.isLoaded(new BlockPos(x+home.w,64,z+home.d)))continue;
            if(buildHome(level,home)) {state.built.add(home.id);homes++;}
            if(homes>=1)break;
        }
        // Build visible road tiles on demand. Always follow surveyed surface.
        int roads=0;
        for(int i=0;i<plan.roads.size();i++) {
            if(state.roadsDone.contains(i))continue;
            Road r=plan.roads.get(i);int x=state.anchorX+r.x,z=state.anchorZ+r.z;
            if(!nearAnyPlayer(server,x,z,BUILDER_RADIUS))continue;
            if(!level.isLoaded(new BlockPos(x,64,z)))continue;
            int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)-1;
            if(y>level.getMinY()+1) {
                level.setBlock(new BlockPos(x,y,z),Blocks.GRAVEL.defaultBlockState(),3);
                state.roadsDone.add(i);roads++;
            }
            if(roads>=150)break;
        }
        // Castle walls are built separately; order matters for entrance alignment.
        buildCastlePass(level);
    }
    private static int safeHeight(ServerLevel l,int x,int z) {return l.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);}
    private static boolean buildHome(ServerLevel level,Home h) {
        int x=state.anchorX+h.x,z=state.anchorZ+h.z;
        int a=safeHeight(level,x,z),b=safeHeight(level,x+h.w-1,z),
            c=safeHeight(level,x,z+h.d-1),d=safeHeight(level,x+h.w-1,z+h.d-1);
        int low=Math.min(Math.min(a,b),Math.min(c,d)),high=Math.max(Math.max(a,b),Math.max(c,d));
        if(high-low>7) return false; // do NOT bury houses in cliffs; candidate stays unbuilt.
        int ground=high;
        // Room for foundation columns; never assume all corners have same elevation.
        BlockState foot=Blocks.COBBLESTONE.defaultBlockState();
        BlockState wall=Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState roof=Blocks.DARK_OAK_PLANKS.defaultBlockState();
        if(h.template.equals("MERCHANT")||h.template.equals("CASTLE_QUARTER")||h.template.equals("GUARD_QUARTER")) {
            wall=Blocks.STONE_BRICKS.defaultBlockState();roof=Blocks.DEEPSLATE_TILES.defaultBlockState();
        } else if(h.template.equals("WORKSHOP"))wall=Blocks.OAK_PLANKS.defaultBlockState();
        for(int xx=x;xx<x+h.w;xx++)for(int zz=z;zz<z+h.d;zz++){
            int original=safeHeight(level,xx,zz);
            for(int y=Math.max(ground-8,original-1);y<=ground;y++)set(level,xx,y,zz,foot);
            for(int y=ground+1;y<=ground+8;y++)set(level,xx,y,zz,Blocks.AIR.defaultBlockState());
            for(int y=ground+1;y<=ground+5;y++) {
                if(xx==x||xx==x+h.w-1||zz==z||zz==z+h.d-1)set(level,xx,y,zz,wall);
            }
            for(int y=ground+6;y<=ground+8;y++) {
                int inward=y-ground-6;
                if(xx>=x+inward && xx<x+h.w-inward)set(level,xx,y,zz,roof);
            }
        }
        for(int xx=x+1;xx<x+h.w-1;xx++)for(int zz=z+1;zz<z+h.d-1;zz++){
            set(level,xx,ground+1,zz,Blocks.SPRUCE_PLANKS.defaultBlockState());
            for(int yy=ground+2;yy<=ground+5;yy++)set(level,xx,yy,zz,Blocks.AIR.defaultBlockState());
        }
        // Proper 2-block high door facing the connected front-row lane.
        int dx=x+h.w/2,dz=z+h.d-1;
        BlockState door=Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.SOUTH);
        // Suppress neighbor updates until both halves exist.
        level.setBlock(new BlockPos(dx,ground+1,dz),door.setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER),2);
        level.setBlock(new BlockPos(dx,ground+2,dz),door.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),2);
        for(int windowX:new int[]{x+2,x+h.w-3}){
            set(level,windowX,ground+3,z,Blocks.GLASS_PANE.defaultBlockState());
            set(level,windowX,ground+3,z+h.d-1,Blocks.GLASS_PANE.defaultBlockState());
        }
        // Real bed pairs; each residence has multiple physical beds, and a larger
        // household capacity ledger. These are not separate independent people.
        int bedsPlaced=0;
        if(h.capacity>0)for(int bz:new int[]{z+2,z+5,z+7})for(int bx=x+2;bx<=x+h.w-3;bx+=3){
            if(bedsPlaced++>=h.capacity)break;
            BlockState bed=Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
            level.setBlock(new BlockPos(bx,ground+2,bz),bed.setValue(BedBlock.PART,BedPart.FOOT),2);
            level.setBlock(new BlockPos(bx,ground+2,bz+1),bed.setValue(BedBlock.PART,BedPart.HEAD),2);
        }
        switch(h.template){
            case "FARMHOUSE" -> {set(level,x+2,ground+2,z+h.d-2,Blocks.HAY_BLOCK.defaultBlockState());
                set(level,x+3,ground+2,z+h.d-2,Blocks.COMPOSTER.defaultBlockState());}
            case "WORKSHOP" -> {set(level,x+2,ground+2,z+h.d-2,Blocks.CRAFTING_TABLE.defaultBlockState());
                set(level,x+3,ground+2,z+h.d-2,Blocks.FURNACE.defaultBlockState());}
            case "MERCHANT" -> {set(level,x+2,ground+2,z+h.d-2,Blocks.BARREL.defaultBlockState());
                set(level,x+3,ground+2,z+h.d-2,Blocks.CHEST.defaultBlockState());}
            case "GUARD_QUARTER" -> set(level,x+2,ground+2,z+h.d-2,Blocks.SMITHING_TABLE.defaultBlockState());
            default -> set(level,x+2,ground+2,z+h.d-2,Blocks.CRAFTING_TABLE.defaultBlockState());
        }
        homeGroundY.put(h.id,ground);return true;
    }
    private static void set(ServerLevel l,int x,int y,int z,BlockState state) {
        if(y<l.getMinY()||y>=l.getMaxY())return;
        l.setBlock(new BlockPos(x,y,z),state,3);
    }
    private static void buildCastlePass(ServerLevel level) {
        // Castle is one major stronghold with two wall rings, gate and main keep.
        // Work is opportunistic by LOADED chunks; missing terrain segments remain pending.
        final int outer=171,inner=148,outerSteps=outer*8,total=outerSteps+inner*8;
        int count=0;
        for(int pos=0;pos<total && count<24;pos++) {
            if(state.wallsDone.contains(pos))continue;
            int rad=pos<outerSteps?outer:inner;
            int idx=pos<outerSteps?pos:pos-outerSteps;
            int seg=idx/rad,u=idx%rad;
            int rx=0,rz=0;
            switch(seg) {
                case 0->{rx=-rad+u;rz=-rad;}
                case 1->{rx=u;rz=-rad;}
                case 2->{rx=rad;rz=-rad+u;}
                case 3->{rx=rad;rz=u;}
                case 4->{rx=rad-u;rz=rad;}
                case 5->{rx=-u;rz=rad;}
                case 6->{rx=-rad;rz=rad-u;}
                default->{rx=-rad;rz=-u;}
            }
            int x=state.anchorX+rx,z=state.anchorZ+rz;
            if(!level.isLoaded(new BlockPos(x,64,z)))continue;
            if(!nearAnyPlayer(currentServer,x,z,BUILDER_RADIUS))continue;
            state.wallsDone.add(pos);
            if(rx>=rad-2&&rz>=10&&rz<=21)continue; // Gate to winter town.
            if(rad==inner && rx>=rad-2 &&
                ((rz>=90&&rz<=98)||(rz>=114&&rz<=122)||(rz>=138&&rz<=146)))continue;
            // Survey 8m OUTSIDE the ring: sampling an adjacent completed wall
            // would otherwise raise the next segment by 20-29 blocks repeatedly.
            int sampleX=x+(rx==rad?8:rx==-rad?-8:0);
            int sampleZ=z+(rz==rad?8:rz==-rad?-8:0);
            if(!level.isLoaded(new BlockPos(sampleX,64,sampleZ))){state.wallsDone.remove(pos);continue;}
            int y=safeHeight(level,sampleX,sampleZ);
            for(int k=-1;k<=1;k++)for(int yy=y;yy<y+(rad==outer?20:29);yy++)
                set(level,x,yy,z+k,Blocks.STONE_BRICKS.defaultBlockState());
            if(idx%6==0)for(int yy=y+(rad==outer?20:29);yy<y+(rad==outer?22:31);yy++)
                set(level,x,yy,z,Blocks.STONE_BRICKS.defaultBlockState());
            count++;
        }
        if(!state.keepBuilt && nearAnyPlayer(currentServer,state.anchorX,state.anchorZ+95,BUILDER_RADIUS)){
            int x=state.anchorX-35,z=state.anchorZ+73;
            if(level.isLoaded(new BlockPos(x,64,z))&&level.isLoaded(new BlockPos(x+69,64,z+42))) {
                if(state.keepSlice==0)state.keepBaseY=safeHeight(level,x,z);
                int y=state.keepBaseY;
                int stop=Math.min(70,state.keepSlice+4);
                for(int deltaX=state.keepSlice;deltaX<stop;deltaX++) {
                    int xx=x+deltaX;
                    for(int zz=z;zz<z+43;zz++) {
                        set(level,xx,y,zz,Blocks.STONE_BRICKS.defaultBlockState());
                        if(xx==x||xx==x+69||zz==z||zz==z+42)for(int h=1;h<=21;h++)
                            set(level,xx,y+h,zz,Blocks.STONE_BRICKS.defaultBlockState());
                        else for(int h=1;h<=21;h++)set(level,xx,y+h,zz,Blocks.AIR.defaultBlockState());
                        set(level,xx,y+22,zz,Blocks.DEEPSLATE_TILES.defaultBlockState());
                    }
                }
                state.keepSlice=stop;
                if(stop==70) {
                    for(int yy=y+1;yy<=y+4;yy++)for(int xx=x+33;xx<=x+36;xx++)
                        set(level,xx,yy,z,Blocks.AIR.defaultBlockState());
                    for(int zz=state.anchorZ+16;zz<z;zz++)
                        set(level,state.anchorX,safeHeight(level,state.anchorX,zz)-1,zz,Blocks.GRAVEL.defaultBlockState());
                    state.keepBuilt=true;
                }
            }
        }
    }
}
