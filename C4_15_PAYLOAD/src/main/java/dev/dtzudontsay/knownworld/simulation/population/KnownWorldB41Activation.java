package dev.dtzudontsay.knownworld.simulation.population;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.simulation.casestudy.KnownWorldUiPackets;
import dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerator;
import dev.dtzudontsay.knownworld.world.generation.settlement.KnownWorldC411GlobalWorldgen;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/**
 * B4.1A.1: nearby physical avatars of the existing B4.0 identities.
 * Every C4.11-house resident is eligible, not just the first 1-2 per home.
 * Spatially tiled compressed resources keep King's Landing out of JVM memory
 * unless its nearby streets are loaded. Winterfell V3 remains independent.
 *
 * Gameplay NPC intelligence and dialogue are explicitly not claimed here:
 * this is a bounded local physical projection and deterministic rich profile
 * view of B4.0 provisional identities, not a second NpcRegistry population.
 */
public final class KnownWorldB41Activation {
    private static final String ROOT = "/data/knownworld/scenarios/agot_298_ac/world_projection_b4_1a1_v1/";
    private static final String VERSION = "knownworld.b4_1a1.household_projection.v1";
    private static final String TAG = "kwb41_actor";
    private static final String ID_PREFIX = "kwb41_id_";
    private static final int MAX_ACTORS = 128;
    private static final int PLAYER_RADIUS = 180;
    private static final int TILE_SIZE = 512;
    private static final int INTERVAL_TICKS = 40;
    private static final int TILE_CACHE_LIMIT = 24;
    private static final int SEARCH_RADIUS = 10;
    private static final int HOME_WANDER_LIMIT = 48;

    private record Site(String id, String name, int anchorX, int anchorZ,
                        int[] bounds, Map<String,Integer> tiles,
                        Map<String,Double> regionalValues, Map<String,Double> regionalNorms) { }
    private record Person(String id, String site, String name, String sex, int birthYear,
                          String household, String home, String householdRole, String mother,
                          String father, String spouse, String occupation, String culture,
                          String religion, String language, String workplace, String profileSeed,
                          String region, int x, int z, int width, int depth) { }
    private record Candidate(Person person, Site site, int homeX, int homeZ, double distance) { }

    private static Map<String,Site> sites;
    private static MinecraftServer activeServer;
    private static int ticks, spawned, failures;
    private static final Map<String,UUID> projected=new HashMap<>();
    private static final Set<UUID> freshlySpawned=new HashSet<>();
    private static final Map<String,Person> visiblePeople=new HashMap<>();
    private static final Map<String,Site> visibleSites=new HashMap<>();
    private static final Map<String,BlockPos> homesOfProjected=new HashMap<>();
    private static final Map<String,List<Person>> tiles=new LinkedHashMap<>(40,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String,List<Person>> e) {
            return size()>TILE_CACHE_LIMIT;
        }
    };

    private KnownWorldB41Activation() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->
                dispatcher.register(Commands.literal("kwactors")
                        .then(Commands.literal("status").executes(ctx->status(ctx.getSource())))
                        .then(Commands.literal("nearby").executes(ctx->nearby(ctx.getSource())))));
        ServerTickEvents.END_SERVER_TICK.register(KnownWorldB41Activation::tick);
        ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{
            if(entity.entityTags().contains(TAG) && !freshlySpawned.remove(entity.getUUID()))
                entity.discard(); // Saved physical avatar never becomes a second authoritative person.
        });
        UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
            if(hand!=InteractionHand.MAIN_HAND || !entity.entityTags().contains(TAG))
                return InteractionResult.PASS;
            if(world.isClientSide())return InteractionResult.SUCCESS;
            if(player instanceof ServerPlayer sp) inspect(sp,entity);
            return InteractionResult.SUCCESS;
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{if(activeServer==s)reset();});
    }

    private static void reset() {
        activeServer=null;sites=null;ticks=0;spawned=0;failures=0;
        projected.clear();freshlySpawned.clear();visiblePeople.clear();visibleSites.clear();homesOfProjected.clear();tiles.clear();
    }
    private static String str(JsonObject o,String key) {
        JsonElement e=o.get(key);return e==null||e.isJsonNull()?null:e.getAsString();
    }
    private static String show(String val) {return val==null||val.isBlank()?"Unrecorded":val;}
    private static Map<String,Double> doubles(JsonObject obj) {
        if(obj==null)return Map.of();
        Map<String,Double> vals=new HashMap<>();
        for(var e:obj.entrySet())if(e.getValue().isJsonPrimitive()&&e.getValue().getAsJsonPrimitive().isNumber())
            vals.put(e.getKey(),e.getValue().getAsDouble());
        return Map.copyOf(vals);
    }
    private static Map<String,Site> siteIndex() {
        if(sites!=null)return sites;
        try(InputStream in=KnownWorldB41Activation.class.getResourceAsStream(ROOT+"manifest.json")) {
            if(in==null){sites=Map.of();return sites;}
            JsonObject root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            if(!VERSION.equals(str(root,"schemaVersion")) ||
                    root.get("sourcePopulationCount").getAsInt()!=3_760_684 ||
                    root.get("sourceHouseholdCount").getAsInt()!=799343 ||
                    root.get("physicalProjectionLimit").getAsInt()!=MAX_ACTORS ||
                    root.getAsJsonArray("sites").size()!=248)
                throw new IllegalStateException("Wrong B4.1A.1 projection manifest/census");
            Map<String,Site> result=new HashMap<>();
            for(JsonElement elem:root.getAsJsonArray("sites")) {
                JsonObject j=elem.getAsJsonObject();String sid=str(j,"siteId");
                if(sid==null || !sid.matches("site_[a-z][a-z0-9_]{1,90}"))
                    throw new IllegalStateException("Unsafe site: "+sid);
                if(!"C4_11_HOME_CANDIDATES".equals(str(j,"status")))continue;
                JsonArray bounds=j.getAsJsonArray("bounds");int[] b=new int[4];
                for(int k=0;k<4;k++)b[k]=bounds.get(k).getAsInt();
                Map<String,Integer> tileCounts=new HashMap<>();
                for(var t:j.getAsJsonObject("tiles").entrySet()) {
                    if(!t.getKey().matches("-?[0-9]+_-?[0-9]+"))
                        throw new IllegalStateException("Invalid projection tile path");
                    tileCounts.put(t.getKey(),t.getValue().getAsInt());
                }
                Site site=new Site(sid,show(str(j,"name")),j.get("anchorX").getAsInt(),
                        j.get("anchorZ").getAsInt(),b,Map.copyOf(tileCounts),
                        doubles(j.getAsJsonObject("regionalValues")),doubles(j.getAsJsonObject("regionalNorms")));
                if(result.putIfAbsent(sid,site)!=null)throw new IllegalStateException("Duplicate site "+sid);
            }
            sites=Map.copyOf(result);
            KnownWorld.LOGGER.info("B4.1A.1 full household projection: {} eligible sites, {} live entity budget",
                    sites.size(),MAX_ACTORS);
            return sites;
        }catch(Exception e){throw new IllegalStateException("Cannot load B4.1A.1 manifest",e);}
    }
    private static List<Person> tile(Site site,String tileId) {
        Integer expected=site.tiles.get(tileId);
        if(expected==null)return List.of();
        String cacheKey=site.id+"/"+tileId;
        List<Person> cached=tiles.get(cacheKey);
        if(cached!=null)return cached;
        List<Person> people=new ArrayList<>();
        try(InputStream input=KnownWorldB41Activation.class.getResourceAsStream(ROOT+cacheKey+".jsonl.gz")) {
            if(input==null)throw new IllegalStateException("Missing B4.1A.1 tile: "+cacheKey);
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(input),StandardCharsets.UTF_8))) {
                String line;
                while((line=reader.readLine())!=null) {
                    JsonObject j=JsonParser.parseString(line).getAsJsonObject();
                    String id=str(j,"personId");
                    if(id==null||!id.startsWith("b4:"+site.id+":")||!site.id.equals(str(j,"siteId")))
                        throw new IllegalStateException("Foreign/invalid person ID in "+cacheKey);
                    people.add(new Person(id,site.id,show(str(j,"name")),str(j,"sex"),
                            j.get("birthYearAC").getAsInt(),str(j,"householdId"),str(j,"homeLotId"),
                            str(j,"householdRole"),str(j,"motherId"),str(j,"fatherId"),str(j,"spouseId"),
                            str(j,"occupationId"),str(j,"cultureId"),str(j,"religionId"),
                            str(j,"languageId"),str(j,"workplaceId"),str(j,"profileSeed"),
                            str(j,"regionalProfileId"),j.get("x").getAsInt(),j.get("z").getAsInt(),
                            j.get("w").getAsInt(),j.get("d").getAsInt()));
                }
            }
            if(people.size()!=expected)
                throw new IllegalStateException(cacheKey+" expected "+expected+" got "+people.size());
        }catch(Exception ex){throw new IllegalStateException("Cannot load projected household tile "+cacheKey,ex);}
        List<Person> result=List.copyOf(people);tiles.put(cacheKey,result);return result;
    }
    private static boolean occupied(ServerLevel level,BlockPos pos,Set<Long> taken) {
        long coord=pos.asLong();
        return taken.contains(coord);
    }
    private static BlockPos safeSpawn(ServerLevel level,int x,int z,int ordinal,Set<Long> taken) {
        // Stable per-person staging positions near doorway, not overlapping on one block.
        int start=(ordinal&0x7fffffff)%64;
        for(int n=0;n<100;n++) {
            int choice=(start+n)%100;
            // SOUTH frontage only: z is already TWO blocks beyond the house.
            // Never look behind the doorway or inside the roof projection.
            int dx=(choice%10)-5,dz=(choice/10);
            if(Math.abs(dx)>SEARCH_RADIUS||Math.abs(dz)>SEARCH_RADIUS)continue;
            int xx=x+dx,zz=z+dz;
            if(!level.isLoaded(new BlockPos(xx,64,zz)))continue;
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,xx,zz);
            if(y<=level.getMinY()+2||y>=level.getMaxY()-3)continue;
            // Heightmap alone can pick a ROOF. Only near-pristine ground beside
            // the south door is allowed. Reject elevated roofs and tower decks.
            int nativeY=KnownWorldC411GlobalWorldgen.initialTerrainHeight(xx,zz);
            if(y-nativeY>9 || nativeY-y>7)continue;
            BlockPos feet=new BlockPos(xx,y,zz),floor=feet.below();
            if(occupied(level,feet,taken)||!level.getBlockState(floor).getFluidState().isEmpty()||
                    level.getBlockState(floor).getCollisionShape(level,floor).isEmpty()||
                    !level.getBlockState(feet).getCollisionShape(level,feet).isEmpty()||
                    !level.getBlockState(feet.above()).getCollisionShape(level,feet.above()).isEmpty()||
                    !level.getBlockState(feet).getFluidState().isEmpty())continue;
            taken.add(feet.asLong());return feet;
        }
        return null;
    }
    private static void tick(MinecraftServer s) {
        if(activeServer!=s){reset();activeServer=s;}
        if(++ticks%INTERVAL_TICKS!=0)return;
        ServerLevel world=s.getLevel(Level.OVERWORLD);
        if(world==null||!(world.getChunkSource().getGenerator() instanceof KnownWorldChunkGenerator))return;
        Map<String,Site> all=siteIndex();if(all.isEmpty())return;
        List<ServerPlayer> players=s.getPlayerList().getPlayers().stream().filter(p->p.level()==world).toList();
        if(players.isEmpty()){removeMissing(world,Set.of());return;}
        List<Candidate> candidates=new ArrayList<>();Set<String> visited=new HashSet<>();
        for(ServerPlayer player:players) {
            int px=(int)Math.floor(player.getX()),pz=(int)Math.floor(player.getZ());
            for(Site site:all.values()) {
                int[] b=site.bounds;
                if((long)px<b[0]-850L||(long)px>b[1]+850L||
                        (long)pz<b[2]-850L||(long)pz>b[3]+850L)continue;
                int[] approved=KnownWorldC411GlobalWorldgen.b41ApprovedPlacementForInspection(site.id);
                if(approved==null)continue;
                int ox=approved[0]-site.anchorX,oz=approved[1]-site.anchorZ;
                int localX=px-ox,localZ=pz-oz;
                int sx=Math.floorDiv(localX,TILE_SIZE),sz=Math.floorDiv(localZ,TILE_SIZE);
                // Radius 180 + side/corner padding: at most 4x4 nearby resources.
                for(int tx=sx-1;tx<=sx+1;tx++)for(int tz=sz-1;tz<=sz+1;tz++) {
                    String tid=tx+"_"+tz;
                    if(!site.tiles.containsKey(tid))continue;
                    for(Person person:tile(site,tid)) {
                        int x=person.x+person.width/2+ox;
                        int z=person.z+person.depth+2+oz;
                        double dx=player.getX()-x-.5,dz=player.getZ()-z-.5;
                        double d=dx*dx+dz*dz;
                        if(d>PLAYER_RADIUS*PLAYER_RADIUS || !visited.add(person.id))continue;
                        candidates.add(new Candidate(person,site,x,z,d));
                    }
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(Candidate::distance)
                .thenComparing(c->c.person.id));
        Set<String> wanted=new HashSet<>();Set<Long> positions=new HashSet<>();int admitted=0;
        // Reserve already occupied projection feet before selecting additional family members.
        for(UUID uuid:projected.values()) {
            Entity existing=world.getEntity(uuid);
            if(existing!=null && existing.isAlive())
                positions.add(new BlockPos((int)Math.floor(existing.getX()),
                        (int)Math.floor(existing.getY()),(int)Math.floor(existing.getZ())).asLong());
        }
        for(Candidate c:candidates) {
            if(admitted>=MAX_ACTORS)break;
            Person p=c.person;
            if(!world.isLoaded(new BlockPos(c.homeX,64,c.homeZ)))continue;
            UUID known=projected.get(p.id);
            Entity e=known==null?null:world.getEntity(known);
            if(e instanceof Villager v && e.isAlive()) {
                // Vanilla local movement is enabled; hold residents near their real address.
                double dx=e.getX()-c.homeX,dz=e.getZ()-c.homeZ;
                if(dx*dx+dz*dz>HOME_WANDER_LIMIT*HOME_WANDER_LIMIT) {
                    BlockPos back=safeSpawn(world,c.homeX,c.homeZ,p.id.hashCode(),positions);
                    if(back!=null)v.snapTo(back.getX()+.5,back.getY(),back.getZ()+.5,0,0);
                }
                // Once conversations exist, their movement lock must remain authoritative.
                visiblePeople.put(p.id,p);visibleSites.put(p.id,c.site);
                homesOfProjected.put(p.id,new BlockPos(c.homeX,64,c.homeZ));
                wanted.add(p.id);admitted++;continue;
            }
            projected.remove(p.id);
            BlockPos dest=safeSpawn(world,c.homeX,c.homeZ,p.id.hashCode(),positions);
            if(dest==null)continue;
            Villager v=new Villager(EntityTypes.VILLAGER,world);
            v.snapTo(dest.getX()+.5,dest.getY(),dest.getZ()+.5,0,0);
            v.setCustomName(Component.literal(p.name));v.setCustomNameVisible(false);
            v.setBaby(p.birthYear>=283); // Initial 298 AC snapshot, future calendar bridge in B4.2.
            v.setPersistenceRequired();
            v.setNoAi(false); // B4.1A.1: local vanilla wandering; NOT autonomous profession work.
            v.addTag(TAG);v.addTag(ID_PREFIX+p.id);
            freshlySpawned.add(v.getUUID());
            if(!world.addFreshEntity(v)){freshlySpawned.remove(v.getUUID());failures++;continue;}
            projected.put(p.id,v.getUUID());visiblePeople.put(p.id,p);visibleSites.put(p.id,c.site);
            homesOfProjected.put(p.id,new BlockPos(c.homeX,64,c.homeZ));
            wanted.add(p.id);admitted++;spawned++;
        }
        removeMissing(world,wanted);
    }
    private static void removeMissing(ServerLevel level,Set<String> wanted) {
        var it=projected.entrySet().iterator();
        while(it.hasNext()) {
            var e=it.next();
            if(wanted.contains(e.getKey()))continue;
            Entity v=level.getEntity(e.getValue());if(v!=null)v.discard();
            visiblePeople.remove(e.getKey());visibleSites.remove(e.getKey());
            homesOfProjected.remove(e.getKey());it.remove();
        }
    }
    private static Person forActor(Entity actor) {
        for(String t:actor.entityTags())if(t.startsWith(ID_PREFIX))return visiblePeople.get(t.substring(ID_PREFIX.length()));
        return null;
    }
    private static double cap(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
    private static long seed(Person p) {
        if(p.profileSeed!=null) {
            try{return Long.parseUnsignedLong(p.profileSeed);}catch(NumberFormatException ignored) { }
        }
        return 298011L ^ ((long)p.id.hashCode()<<32) ^ p.household.hashCode();
    }
    private static JsonObject scoreMap(Random r,String[] axes,double base,double spread,Map<String,Double> bias,
                                      double biasWeight) {
        JsonObject json=new JsonObject();
        for(String key:axes){double v=base+(r.nextDouble()*2-1)*spread;
            v+=bias.getOrDefault(key.toUpperCase(Locale.ROOT),0.0)*biasWeight;
            json.addProperty(key.toLowerCase(Locale.ROOT),Math.round(cap(v,base<0?-1:0,1)*1000)/1000.0);
        }return json;
    }
    private static final String[] SKILLS={"martial","diplomacy","stewardship","intrigue","learning",
            "combat","leadership","riding","sailing","navigation","trade","crafting","agriculture",
            "hunting","medicine","religious_study","language","performance","command","logistics"};
    private static final String[] DISPOSITIONS={"independence","assertiveness","impulsivity","discipline",
            "curiosity","risk_tolerance","emotional_stability","suspiciousness","empathy","aggression",
            "adaptability","persistence","pride","humility","greed","generosity","forgiveness",
            "vindictiveness","deceitfulness","gullibility","conformity","dominance","obedience",
            "jealousy","romanticism","sensuality"};
    private static final String[] VALUES={"duty","family_loyalty","house_loyalty","dynastic_loyalty",
            "clan_loyalty","friendship_loyalty","personal_freedom","authority_respect",
            "hierarchy_acceptance","egalitarianism","traditionalism","innovation","religiosity",
            "religious_tolerance","cultural_tolerance","honor_culture","hospitality","mercy",
            "vengeance","justice","law_obedience","wealth_seeking","prestige_seeking","power_seeking",
            "knowledge_seeking","martial_glory","pacifism","family_formation","romantic_love",
            "political_stability","social_mobility","communal_solidarity","individualism"};
    private static final String[] NORMS={"martial","courtly","mercantile","naval","raiding","scholarly",
            "equestrian","clan_based","dynastic","religious","hospitality","vengeance",
            "bureaucratic","urban","rural","cosmopolitan","isolationist","collectivist",
            "individualist","social_mobility","authority","traditional","foreign_exposure",
            "political_exposure","migratory"};
    private static final Map<String,String> PROFICIENCY=new HashMap<>();
    static {
        for(String s:new String[]{"farmer","herder","shepard","grain","gardener","beekeeper","orchardist"})PROFICIENCY.put(s,"agriculture");
        for(String s:new String[]{"sailor","oarsman","fisherman","shipwright","harbor","captain|maritime"})PROFICIENCY.put(s,"sailing");
        for(String s:new String[]{"guard","soldier","spearman","archer","knight","mercenary","man-at-arms"})PROFICIENCY.put(s,"combat");
        for(String s:new String[]{"merchant","trader","banker","market","money","innkeeper"})PROFICIENCY.put(s,"trade");
        for(String s:new String[]{"blacksmith","carpenter","weaver","mason","cobbler","baker","brewer","tailor","fletcher","potter"})PROFICIENCY.put(s,"crafting");
        for(String s:new String[]{"septon","septa","priest","religious","moonsinger","godswife"})PROFICIENCY.put(s,"religious_study");
        for(String s:new String[]{"healer","maester","midwife","apothecary"})PROFICIENCY.put(s,"medicine");
        for(String s:new String[]{"steward","scribe","clerk","administrator","tax collector"})PROFICIENCY.put(s,"stewardship");
    }
    private static String occupationalSkill(String occupation) {
        if(occupation==null)return "";
        String lower=occupation.toLowerCase(Locale.ROOT);
        for(var e:PROFICIENCY.entrySet())if(lower.contains(e.getKey()))return e.getValue();
        return "";
    }
    private static void enrich(JsonObject sheet,Person p,Site site) {
        Random rng=new Random(seed(p));int age=Math.max(0,298-p.birthYear);
        JsonObject personality=scoreMap(rng,new String[]{"courage","ambition","compassion","honor",
                "patience","sociability"},0.48,0.26,Map.of(),0);
        sheet.add("personality",personality);
        JsonObject skills=scoreMap(rng,SKILLS,Math.min(.45,.08+Math.max(0,age-14)*.007),.10,Map.of(),0);
        String focus=occupationalSkill(p.occupation);
        if(!focus.isBlank()&&age>=16)skills.addProperty(focus,Math.round(cap(.28+age*.007+rng.nextDouble()*.20,0,.95)*1000)/1000.0);
        if(age<16)for(String skill:SKILLS)skills.addProperty(skill,Math.round(rng.nextDouble()*.14*1000)/1000.0);
        sheet.add("skills",skills);
        sheet.add("aptitudes",scoreMap(rng,SKILLS,.48,.22,Map.of(),0));
        sheet.add("dispositions",scoreMap(rng,DISPOSITIONS,0,.40,Map.of(),0));
        sheet.add("characterValues",scoreMap(rng,VALUES,0,.32,site.regionalValues,.70));
        sheet.add("socialNorms",scoreMap(rng,NORMS,0,.30,site.regionalNorms,.72));
        JsonArray traits=new JsonArray();
        if(personality.get("courage").getAsDouble()>.67)traits.add("brave");
        if(personality.get("compassion").getAsDouble()>.67)traits.add("compassionate");
        if(personality.get("ambition").getAsDouble()>.70)traits.add("ambitious");
        if(personality.get("patience").getAsDouble()<.33)traits.add("impatient");
        if(personality.get("honor").getAsDouble()>.65)traits.add("dutiful");
        if(traits.isEmpty())traits.add(rng.nextBoolean()?"practical":"observant");
        sheet.add("traits",traits);
        JsonObject languages=new JsonObject();if(p.language!=null)languages.addProperty(p.language,.65+rng.nextDouble()*.32);
        sheet.add("languages",languages);
        sheet.addProperty("education",age<16?"family_upbringing":focus.isBlank()?"local_upbringing":"occupational_apprenticeship");
        sheet.addProperty("birthplace",show(p.site));sheet.addProperty("upbringing",show(p.region));
        sheet.addProperty("health","HEALTHY");sheet.addProperty("legalStatus","UNSPECIFIED");
        sheet.addProperty("wealth",String.format(Locale.ROOT,"%.2f",cap(rng.nextDouble()*.35,0,1)));
        sheet.addProperty("socialStatus",String.format(Locale.ROOT,"%.2f",cap(.1+rng.nextDouble()*.35,0,1)));
        sheet.addProperty("reputation","0.00");
        JsonObject dialogue=new JsonObject();
        dialogue.addProperty("formality",Math.round((.2+rng.nextDouble()*.6)*1000)/1000.0);
        dialogue.addProperty("warmth",personality.get("compassion").getAsDouble());
        dialogue.addProperty("verbosity",Math.round((.25+rng.nextDouble()*.6)*1000)/1000.0);
        dialogue.addProperty("directness",personality.get("courage").getAsDouble());
        sheet.add("dialogueStyle",dialogue);
        JsonArray occupations=new JsonArray();if(p.occupation!=null)occupations.add(p.occupation);
        sheet.add("occupations",occupations);
        JsonArray motivations=new JsonArray();motivations.add(age<16?"learning_and_family":"household_wellbeing");
        sheet.add("motivations",motivations);
    }
    private static void inspect(ServerPlayer player,Entity actor) {
        if(player.distanceToSqr(actor)>64)return;
        Person p=forActor(actor);if(p==null)return;
        Site site=visibleSites.get(p.id);if(site==null)return;
        JsonObject sheet=new JsonObject();
        sheet.addProperty("name",p.name);sheet.addProperty("id",p.id);
        sheet.addProperty("status","B4_0_PROVISIONAL_IDENTITY_B4_1A1_DETERMINISTIC_PROFILE");
        sheet.addProperty("age",Math.max(0,298-p.birthYear));sheet.addProperty("sex",show(p.sex));
        sheet.addProperty("occupation",show(p.occupation));sheet.addProperty("culture",show(p.culture));
        sheet.addProperty("religion",show(p.religion));sheet.addProperty("household",show(p.household));
        sheet.addProperty("residence",show(p.home));sheet.addProperty("workplace",show(p.workplace));
        sheet.addProperty("householdLabel",show(p.household));sheet.addProperty("homeLabel",show(p.home));
        sheet.addProperty("workplaceLabel",show(p.workplace));
        enrich(sheet,p,site);
        boolean gm=player.isCreative()&&Commands.LEVEL_GAMEMASTERS.check(player.createCommandSourceStack().permissions());
        sheet.addProperty("gamemaster",gm);
        if(gm) {
            sheet.addProperty("motherId",show(p.mother));sheet.addProperty("fatherId",show(p.father));
            sheet.addProperty("spouseId",show(p.spouse));sheet.addProperty("householdRole",show(p.householdRole));
            sheet.addProperty("regionalProfile",show(p.region));
            sheet.addProperty("profileBasis","DETERMINISTIC_BASELINE_NOT_AUTHORED_PERSONAL_MEMORY");
        }
        ServerPlayNetworking.send(player,new KnownWorldUiPackets.CharacterSheet(sheet.toString()));
    }
    private static void say(CommandSourceStack src,String s){src.sendSuccess(()->Component.literal(s),false);}
    private static int status(CommandSourceStack src) {
        if(siteIndex().isEmpty()) {
            say(src,"[B4.1A.1] ERROR: projection manifest missing or contains no buildable sites. Run RUN_B4_1A1_WINTERFELL_PARITY.bat.");
            return 0;
        }
        say(src,"[B4.1A.1] Full-household physical activation: "+siteIndex().size()+" C4.11 sites | "
                +projected.size()+" avatars / 128 budget; spawned="+spawned+" failed="+failures);
        say(src,"[B4.1A.1] Nearby resources="+tiles.size()+" / "+TILE_CACHE_LIMIT+" cached tiles. "
                +"Winterfell V3 and 3.76m B4.0 identities untouched. K-chat/offscreen work not yet activated.");
        return 1;
    }
    private static int nearby(CommandSourceStack src) {
        say(src,"[B4.1A.1] "+visiblePeople.size()+" projected residents");
        visiblePeople.values().stream().sorted(Comparator.comparing(p->p.name)).limit(12)
                .forEach(p->say(src,"  "+p.name+" | "+show(p.occupation)+" | "+show(p.culture)+" / "+show(p.religion)
                        +" | household "+show(p.household)));
        return 1;
    }
}
