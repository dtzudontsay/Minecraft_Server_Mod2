package dev.dtzudontsay.knownworld.world.generation.settlement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.data.GeographicDataManager;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * C4.11: global, demand-conserving, fresh-chunk settlement construction.
 *
 * The 248-site index is metadata only. Actual structures are in 512x512 block
 * tile resources, resolved on demand (bounded tile cache); this avoids
 * materializing King's Landing + every other city into one massive global
 * roads/chunks HashMap. The authoritative C4.9 population/jobs are NOT NPCs.
 *
 * Source coordinates are respected; coast/mountain central sites get a bounded
 * C4.9 relocation search. All unsafe terrain and unavailable templates are
 * explicit deferred construction, never a silent reduction of source demand.
 */
public final class KnownWorldC411GlobalWorldgen {
    private static final String INDEX_RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/universal_construction_c4_11_global_v1.json";
    private static final String TILE_ROOT =
            "data/knownworld/scenarios/agot_298_ac/c4_11_global_tiles/";
    private static final int TILE_SIZE=512;
    private static final int MAX_MOVE=512;
    private static final int PITCH=31;
    private static final int TILE_CACHE_LIMIT=96;
    private static final Set<String> EXISTING=Set.of("site_nw_029","site_nw_031","site_sw_023");
    private static volatile List<Site> metadata;
    private static final ConcurrentMap<String,Placement> placements=new ConcurrentHashMap<>();
    private static final ConcurrentMap<String,Boolean> reportedLotFailures=new ConcurrentHashMap<>();
    private static final AtomicInteger generatedChunks=new AtomicInteger();
    private static final AtomicInteger failedTerrainLots=new AtomicInteger();
    private static final AtomicInteger startedSites=new AtomicInteger();

    private record Lot(String id,String template,String kind,int x,int z,int w,int d,
                       int staff,String face,String region,int gx,int gz,int sx,int sz) { }
    private record Site(String id,String name,String tier,String palette,String mode,
                        int originX,int originZ,int[] bounds,List<String> markers,
                        int people,int homes,int workplaces,int totalLots,
                        Set<Long> tiles) { }
    private record Placement(boolean approved,int x,int z,String reason) { }
    private record Survey(boolean approved,int floor,String reason) { }

    // Bounded cache holds only recently explored local tiles, not entire atlas.
    private static final Map<String,List<Lot>> tileCache =
            new LinkedHashMap<>(128,0.75f,true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String,List<Lot>> eldest) {
                    return size()>TILE_CACHE_LIMIT;
                }
            };
    private KnownWorldC411GlobalWorldgen() { }
    private static long key(int x,int z) {return ((long)x<<32)^(z&0xffffffffL);}
    private static boolean dry(int x,int z) {
        var g=KnownWorldGeoSampler.sampleMinecraft(x+.5,z+.5);
        return g.insideKnownWorldMap() && g.land();
    }
    private static int top(int x,int z) {
        double v=GeographicDataManager.getInstance()
                .sample(WorldProjection.fromMinecraft(x+.5,z+.5)).elevationMetres();
        return (int)Math.max(-63,Math.min(2030,Math.round(v)));
    }
    /** Geographical ground estimator, never a heightmap of built roofs. */
    public static int initialTerrainHeight(int x,int z) {return top(x,z);}

    private static List<Site> sites() {
        List<Site> v=metadata;
        if(v!=null)return v;
        synchronized(KnownWorldC411GlobalWorldgen.class) {
            if(metadata==null)metadata=readIndex();
            return metadata;
        }
    }
    private static List<Site> readIndex() {
        try(InputStream stream=KnownWorldC411GlobalWorldgen.class.getClassLoader()
                .getResourceAsStream(INDEX_RESOURCE)) {
            if(stream==null)return List.of();
            JsonObject root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if(root.get("schemaVersion").getAsInt()!=1 || !root.get("globalEnabled").getAsBoolean()
                    || !root.get("noNpcCreation").getAsBoolean() || root.get("siteCount").getAsInt()!=248
                    || root.get("tileSize").getAsInt()!=TILE_SIZE)
                throw new IllegalStateException("Invalid C4.11 worldwide generator manifest");
            List<Site> all=new ArrayList<>();Set<String> unique=new HashSet<>();
            int attempted=0;
            for(JsonElement entry:root.getAsJsonArray("sites")) {
                JsonObject s=entry.getAsJsonObject();String id=s.get("siteId").getAsString();
                if(!unique.add(id))throw new IllegalStateException("Duplicate global site "+id);
                String status=s.get("status").getAsString();
                if(!status.equals("GLOBAL_TERRAIN_SURVEY_PENDING") &&
                        !status.equals("PROVISIONAL_SPECIAL_GEOMETRY"))continue;
                if(EXISTING.contains(id))throw new IllegalStateException("Existing site cannot be duplicated: "+id);
                String tier=s.get("physicalTier").getAsString();
                KnownWorldC410Fortifications.forTier(tier);
                var bounds=s.getAsJsonArray("bounds");
                int[] bb=new int[4];for(int i=0;i<4;i++)bb[i]=bounds.get(i).getAsInt();
                List<String> markers=new ArrayList<>();
                for(JsonElement m:s.getAsJsonArray("markerIds"))markers.add(m.getAsString());
                Set<Long> tiles=new HashSet<>();
                for(JsonElement t:s.getAsJsonArray("tiles")) {
                    String[] coords=t.getAsString().split("_");
                    if(coords.length!=2)throw new IllegalStateException("Bad tile coordinate for "+id);
                    tiles.add(key(Integer.parseInt(coords[0]),Integer.parseInt(coords[1])));
                }
                int h=s.get("homesTarget").getAsInt(),w=s.get("workplacesTarget").getAsInt();
                int available=s.get("physicalAvailableLots").getAsInt();
                int deferred=s.get("deferredTemplateLots").getAsInt();
                if(h+w!=available+deferred)throw new IllegalStateException("Lost demand "+id);
                all.add(new Site(id,s.get("name").getAsString(),tier,s.get("palette").getAsString(),
                        s.get("blueprintMode").getAsString(),s.get("originalX").getAsInt(),
                        s.get("originalZ").getAsInt(),bb,List.copyOf(markers),
                        s.get("populationTarget").getAsInt(),h,w,h+w,Set.copyOf(tiles)));
                attempted++;
            }
            if(unique.size()!=248)throw new IllegalStateException("Global index must retain all 248 sites");
            KnownWorld.LOGGER.info("C4.11 global new-chunk worldgen ACTIVE attempts={} allSites={} existing protected={} (no NPCs)",
                    attempted,unique.size(),EXISTING.size());
            return List.copyOf(all);
        }catch(java.io.IOException err){throw new IllegalStateException("Cannot read C4.11 worldgen index",err);}
    }

    private static boolean potentiallyNear(Site site,int wx,int wz) {
        int[] b=site.bounds;
        int margin=MAX_MOVE+64;
        return (long)wx+15 >= (long)b[0]-margin && (long)wx <= (long)b[1]+margin
                && (long)wz+15 >= (long)b[2]-margin && (long)wz <= (long)b[3]+margin;
    }
    private static Placement resolve(Site site) {
        startedSites.incrementAndGet();
        var cfg=KnownWorldC410Fortifications.forTier(site.tier);
        int radius=cfg.outerHalfSpan();
        var policy=new KnownWorldC49TerrainPolicy.Policy(MAX_MOVE,radius+8,radius+200,
                32,60,32,12);
        var input=new KnownWorldC49TerrainPolicy.Site(site.id,site.name,site.markers,
                site.originX,site.originZ,site.tier,site.people,site.homes,site.workplaces);
        var sampler=new KnownWorldC49TerrainPolicy.TerrainSampler() {
            @Override public boolean isDryLand(int x,int z){return dry(x,z);}
            @Override public int surfaceHeight(int x,int z){return top(x,z);}
        };
        var choice=KnownWorldC49TerrainPolicy.find(input,sampler,policy);
        if(!choice.successful()) {
            var f=choice.failure();
            KnownWorld.LOGGER.warn("C4.11 SITE FAILURE site={} markerIds={} original={},{} attempted={},{} homesUnbuilt={} workplacesUnbuilt={} reason={}",
                    site.id,site.markers,site.originX,site.originZ,f.attemptedX(),f.attemptedZ(),
                    site.homes,site.workplaces,f.reason());
            return new Placement(false,site.originX,site.originZ,f.reason());
        }
        int x=choice.placement().constructionX(),z=choice.placement().constructionZ();
        if(!coreDryCheck(x,z,cfg)) {
            KnownWorld.LOGGER.warn("C4.11 SITE FAILURE site={} markerIds={} original={},{} attempted={},{} homesUnbuilt={} workplacesUnbuilt={} reason=CORE_WALL_DRY_SURVEY_FAILED",
                    site.id,site.markers,site.originX,site.originZ,x,z,site.homes,site.workplaces);
            return new Placement(false,x,z,"CORE_WALL_DRY_SURVEY_FAILED");
        }
        KnownWorld.LOGGER.info("C4.11 SITE APPROVED site={} markerIds={} map={},{} build={},{} tier={} mode={} homes={} workplaces={} populationTarget={} (NPCs=0)",
                site.id,site.markers,site.originX,site.originZ,x,z,site.tier,
                site.mode,site.homes,site.workplaces,site.people);
        return new Placement(true,x,z,"OK");
    }
    private static boolean coreDryCheck(int cx,int cz,KnownWorldC410Fortifications.Core cfg) {
        int r=cfg.outerHalfSpan();
        for(int a=-r;a<=r;a+=4)
            if(!dry(cx-r,cz+a)||!dry(cx+r,cz+a)||!dry(cx+a,cz-r)||!dry(cx+a,cz+r))
                return false;
        for(int x=cfg.keepWest();x<=cfg.keepEast();x+=3)
            for(int z=cfg.keepNorth();z<=cfg.keepSouth();z+=3)
                if(!dry(cx+x,cz+z))return false;
        return true;
    }

    private static List<Lot> tile(Site site,int tx,int tz) {
        if(!site.tiles.contains(key(tx,tz)))return List.of();
        String name=site.id+"/"+tx+"_"+tz;
        synchronized(tileCache) {
            List<Lot> cached=tileCache.get(name);
            if(cached!=null)return cached;
            List<Lot> read=readTile(site,name);
            tileCache.put(name,read);
            return read;
        }
    }
    private static List<Lot> readTile(Site site,String name) {
        try(InputStream stream=KnownWorldC411GlobalWorldgen.class.getClassLoader()
                .getResourceAsStream(TILE_ROOT+name+".json")) {
            if(stream==null)throw new IllegalStateException("Missing C4.11 tile "+name);
            JsonObject root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if(!site.id.equals(root.get("siteId").getAsString()))
                throw new IllegalStateException("C4.11 tile site mismatch "+name);
            List<Lot> lots=new ArrayList<>();
            for(JsonElement entry:root.getAsJsonArray("lots")) {
                JsonObject j=entry.getAsJsonObject();
                boolean green="GREEN_POCKET".equals(j.get("permission").getAsString());
                if(!green && !"C3_MODULE_AVAILABLE".equals(j.get("permission").getAsString()))
                    throw new IllegalStateException("Nonapproved template in tile "+name);
                int w=j.get("w").getAsInt(),d=j.get("d").getAsInt();
                if(w<5||d<5||w>50||d>50)
                    throw new IllegalStateException("Invalid global template footprint in "+name);
                var grid=j.getAsJsonArray("g");
                int gx=grid==null?Integer.MIN_VALUE:grid.get(0).getAsInt();
                int gz=grid==null?0:grid.get(1).getAsInt();
                int sx=grid==null?1:grid.get(2).getAsInt();
                int sz=grid==null?1:grid.get(3).getAsInt();
                if(sx<1||sx>4||sz<1||sz>4)throw new IllegalStateException("Invalid urban cell span "+name);
                lots.add(new Lot(j.get("id").getAsString(),j.get("t").getAsString(),
                        j.get("kind").getAsString(),j.get("x").getAsInt(),j.get("z").getAsInt(),
                        w,d,j.get("s").getAsInt(),j.get("f").getAsString(),j.get("r").getAsString(),
                        gx,gz,sx,sz));
            }
            return List.copyOf(lots);
        }catch(java.io.IOException err){throw new IllegalStateException("Cannot load C4.11 tile "+name,err);}
    }
    private static Survey inspect(int x,int z,Lot b) {
        int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE;
        for(int dx=0;dx<b.w;dx+=2)for(int dz=0;dz<b.d;dz+=2) {
            int wx=x+dx,wz=z+dz;
            if(!dry(wx,wz))return new Survey(false,0,"WATER_OR_COAST_INTERSECTS_LOT");
            int height=top(wx,wz);lo=Math.min(lo,height);hi=Math.max(hi,height);
        }
        int[] h=new int[9];int i=0;
        for(int dx:new int[]{1,b.w/2,b.w-2})
            for(int dz:new int[]{1,b.d/2,b.d-2})h[i++]=top(x+dx,z+dz);
        Arrays.sort(h);int floor=h[5];
        int doorX=x+b.w/2;
        int doorZ="NORTH".equals(b.face)?z-2:z+b.d+1;
        if(!dry(doorX,doorZ)||Math.abs(top(doorX,doorZ)-floor)>5)
            return new Survey(false,floor,"INACCESSIBLE_ENTRANCE");
        if(hi-lo>6 || floor<-48 || floor>1980)
            return new Survey(false,floor,"UNSAFE_SLOPE_OR_ELEVATION");
        return new Survey(true,floor,"OK");
    }
    private static boolean intersectsChunk(int x,int z,int w,int d,int wx,int wz,int margin) {
        return x<=wx+15+margin && x+w-1>=wx-margin
                && z<=wz+15+margin && z+d-1>=wz-margin;
    }
    private static List<Lot> lotsForChunk(Site site,Placement p,ChunkAccess chunk) {
        int ox=chunk.getPos().getMinBlockX()-(p.x-site.originX);
        int oz=chunk.getPos().getMinBlockZ()-(p.z-site.originZ);
        return tile(site,Math.floorDiv(ox,TILE_SIZE),Math.floorDiv(oz,TILE_SIZE));
    }
    private static boolean touchesSite(Site site,Placement p,ChunkAccess c,int margin) {
        int wx=c.getPos().getMinBlockX(),wz=c.getPos().getMinBlockZ();
        int dx=p.x-site.originX,dz=p.z-site.originZ;
        int r=KnownWorldC410Fortifications.forTier(site.tier).outerHalfSpan();
        if(intersectsChunk(p.x-r-4,p.z-r-4,2*r+9,2*r+9,wx,wz,margin))return true;
        if(intersectsChunk(p.x-2,p.z-r-PITCH-3,5,2*(r+PITCH)+7,wx,wz,margin)
                || intersectsChunk(p.x-r-PITCH-3,p.z-2,2*(r+PITCH)+7,5,wx,wz,margin))return true;
        // C4.16: connector corridors must paint even in otherwise empty chunks
        // between the urban boundary and rural production belt.
        if(touchesBeltSpine(site,p,wx,wz,margin))return true;
        for(Lot b:lotsForChunk(site,p,c))
            if(intersectsChunk(b.x+dx,b.z+dz,b.w,b.d,wx,wz,margin+32))return true;
        return false;
    }

    /** Broad-phase gate for winding access trunks between town and work belt. */
    private static boolean touchesBeltSpine(Site site,Placement p,int wx,int wz,int margin) {
        int r=KnownWorldC410Fortifications.forTier(site.tier).outerHalfSpan();
        int dx=p.x-site.originX,dz=p.z-site.originZ;
        int west=site.bounds[0]+dx,east=site.bounds[1]+dx;
        int north=site.bounds[2]+dz,south=site.bounds[3]+dz;
        int spread=30+margin;
        return (wx<=p.x+spread && wx+15>=p.x-spread &&
                  ((wz<=p.z-r-2 && wz+15>=north-2) ||
                   (wz<=south+2 && wz+15>=p.z+r+2))) ||
               (wz<=p.z+spread && wz+15>=p.z-spread &&
                  ((wx<=p.x-r-2 && wx+15>=west-2) ||
                   (wx<=east+2 && wx+15>=p.x+r+2)));
    }

    public static boolean suppressNaturalDecoration(ChunkAccess chunk) {
        int wx=chunk.getPos().getMinBlockX(),wz=chunk.getPos().getMinBlockZ();
        for(Site site:sites()) {
            if(!potentiallyNear(site,wx,wz))continue;
            Placement p=placements.computeIfAbsent(site.id,ignored->resolve(site));
            if(p.approved && touchesSite(site,p,chunk,32))return true;
        }
        return false;
    }
    public static boolean suppressVanillaStructures(ChunkAccess chunk) {
        return suppressNaturalDecoration(chunk);
    }
    private static void line(BitSet roads,int wx,int wz,int x1,int z1,int x2,int z2,int thickness) {
        if(x1!=x2 && z1!=z2)return;
        if(x1==x2) {
            for(int zz=Math.max(wz,Math.min(z1,z2));zz<=Math.min(wz+15,Math.max(z1,z2));zz++)
                for(int xx=x1-thickness/2;xx<=x1+thickness/2;xx++)
                    if(xx>=wx&&xx<wx+16)roads.set(((zz-wz)<<4)|(xx-wx));
        } else {
            for(int xx=Math.max(wx,Math.min(x1,x2));xx<=Math.min(wx+15,Math.max(x1,x2));xx++)
                for(int zz=z1-thickness/2;zz<=z1+thickness/2;zz++)
                    if(zz>=wz&&zz<wz+16)roads.set(((zz-wz)<<4)|(xx-wx));
        }
    }
    private static int warp(int x,int z,int component) {
        long n=(long)x*11939L+(long)z*32771L+(long)x*z*23L;
        long hashed=n&0x7fffffffL;
        return (int)((component==0?hashed:hashed/11)%5)-2;
    }
    private static int nodeX(Placement p,int gx,int gz) {
        return p.x+gx*PITCH-PITCH/2+warp(gx,gz,0);
    }
    private static int nodeZ(Placement p,int gx,int gz) {
        return p.z+gz*PITCH-PITCH/2+warp(gx,gz,1);
    }
    /** Two joined half-segments create low-amplitude curved streets; nodes shared by neighbors. */
    private static void street(BitSet roads,int wx,int wz,int x1,int z1,int x2,int z2) {
        int bend=(int)Math.floorMod((long)x1*31L+(long)z1*17L+(long)x2*7L+z2,9L)-4;
        int mx=(x1+x2)/2 + (Math.abs(x2-x1)<Math.abs(z2-z1)?bend:0);
        int mz=(z1+z2)/2 + (Math.abs(x2-x1)>=Math.abs(z2-z1)?bend:0);
        int width=(int)Math.floorMod((long)x1*13L+z1*7L+x2+z2,5L)==0?3:2;
        raster(roads,wx,wz,x1,z1,mx,mz,width);
        raster(roads,wx,wz,mx,mz,x2,z2,width);
        // Village-green-like rounded intersection, not modern highway roundabouts.
        if((int)Math.floorMod((long)mx*17L+mz*13L,37L)==0)
            raster(roads,wx,wz,mx,mz,mx,mz,5);
    }
    private static void raster(BitSet bits,int wx,int wz,int x0,int z0,int x1,int z1,int width) {
        int length=Math.max(Math.abs(x1-x0),Math.abs(z1-z0));
        // Bound the per-chunk work even for unusually broad civic modules.
        for(int t=0;t<=length;t++) {
            int x=length==0?x0:(int)Math.round(x0+(x1-x0)*(t/(double)length));
            int z=length==0?z0:(int)Math.round(z0+(z1-z0)*(t/(double)length));
            if(x<wx-width||x>wx+15+width||z<wz-width||z>wz+15+width)continue;
            for(int xx=x-width/2;xx<=x+width/2;xx++)
                for(int zz=z-width/2;zz<=z+width/2;zz++)
                    if(xx>=wx&&xx<wx+16&&zz>=wz&&zz<wz+16)
                        bits.set(((zz-wz)<<4)|(xx-wx));
        }
    }
    private static void streetsForLot(BitSet roads,int wx,int wz,Site site,Placement p,Lot b) {
        if("INNER_WARD".equals(b.region)) {
            int radius="SMALL".equals(site.tier)?25:"MEDIUM".equals(site.tier)?49:49;
            int centerX=b.x+(p.x-site.originX)+b.w/2;
            int doorZ=b.z+(p.z-site.originZ)+b.d;
            int laneX=p.x+(centerX>=p.x?radius:-radius);
            raster(roads,wx,wz,centerX,doorZ,laneX,doorZ,2);
            // Inner ward's compact ring is connected to the cardinal gate spines.
            line(roads,wx,wz,p.x-radius,p.z-radius,p.x+radius,p.z-radius,2);
            line(roads,wx,wz,p.x-radius,p.z+radius,p.x+radius,p.z+radius,2);
            line(roads,wx,wz,p.x-radius,p.z-radius,p.x-radius,p.z+radius,2);
            line(roads,wx,wz,p.x+radius,p.z-radius,p.x+radius,p.z+radius,2);
            return;
        }
        if(b.gx==Integer.MIN_VALUE)return;
        int gx=b.gx,gz=b.gz,sx=b.sx,sz=b.sz;
        int ax=nodeX(p,gx,gz),az=nodeZ(p,gx,gz);
        int bx=nodeX(p,gx+sx,gz),bz=nodeZ(p,gx+sx,gz);
        int cx=nodeX(p,gx+sx,gz+sz),cz=nodeZ(p,gx+sx,gz+sz);
        int dx=nodeX(p,gx,gz+sz),dz=nodeZ(p,gx,gz+sz);
        // C4.16: public frontage + staggered parallel service lanes;
        // fewer complete rectangular borders around individual gardens.
        street(roads,wx,wz,cx,cz,dx,dz);
        if(Math.floorMod(gz,3)==0 || sz>1)
            street(roads,wx,wz,ax,az,bx,bz);
        if(Math.floorMod(gx,3)==0 || sx>1)
            street(roads,wx,wz,dx,dz,ax,az);
        if(Math.floorMod(gx+sx,3)==0 || sx>1)
            street(roads,wx,wz,bx,bz,cx,cz);
        // A real paved spur connects the *actual south door* to the street;
        // the street segment is shared with adjacent plots.
        if(!"GREEN_POCKET".equals(b.kind)) {
            int x=b.x+(p.x-site.originX)+b.w/2;
            int z=b.z+(p.z-site.originZ)+b.d;
            int landingX=(cx+dx)/2,landingZ=(cz+dz)/2;
            raster(roads,wx,wz,x,z,landingX,landingZ,1);
        }
    }
    /** Chunk-clipped, region-sensitive micro-grove: trees, grass, ferns and flowers. */
    private static void tree(ChunkAccess c,Site site,Placement p,Lot b) {
        int ox=p.x-site.originX,oz=p.z-site.originZ;
        int bx=b.x+ox,bz=b.z+oz;
        int wx=c.getPos().getMinBlockX(),wz=c.getPos().getMinBlockZ();
        int[] treeX={4,b.w-5,b.w/2};
        int[] treeZ={5,b.d-6,b.d/2+3};
        String region=site.palette.toUpperCase(java.util.Locale.ROOT);
        boolean cold=region.contains("NORTHERN")||region.contains("IBBEN");
        boolean jungle=region.contains("SUMMER")||region.contains("SOTHORYOS");
        BlockState logs=(cold?net.minecraft.world.level.block.Blocks.SPRUCE_LOG:
                jungle?net.minecraft.world.level.block.Blocks.JUNGLE_LOG:
                net.minecraft.world.level.block.Blocks.OAK_LOG).defaultBlockState();
        BlockState leaves=(cold?net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES:
                jungle?net.minecraft.world.level.block.Blocks.JUNGLE_LEAVES:
                net.minecraft.world.level.block.Blocks.OAK_LEAVES).defaultBlockState();
        for(int j=0;j<treeX.length;j++) {
            int x=bx+treeX[j],z=bz+treeZ[j];
            if(!dry(x,z))continue;
            int y=top(x,z),height=4+(int)Math.floorMod((long)x*23L+z*17L,3L);
            for(int dy=1;dy<=height;dy++)putLocal(c,x,y+dy,z,logs);
            for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)
                for(int dy=height-2;dy<=height+2;dy++) {
                    if(Math.abs(dx)+Math.abs(dz)+Math.abs(dy-height)>4)continue;
                    putLocal(c,x+dx,y+dy,z+dz,leaves);
                }
        }
        for(int z=Math.max(wz,bz+1);z<=Math.min(wz+15,bz+b.d-2);z++)
            for(int x=Math.max(wx,bx+1);x<=Math.min(wx+15,bx+b.w-2);x++) {
                if(!dry(x,z))continue;
                int h=(int)Math.floorMod((long)x*198491317L+(long)z*6542989L,29L);
                if(h>15)continue;
                int y=top(x,z)+1;
                if(y<c.getMinY()+2||y>=c.getMaxY()-4)continue;
                var here=new net.minecraft.core.BlockPos(x,y,z);
                if(!c.getBlockState(here).isAir())continue;
                BlockState grass=h<3?net.minecraft.world.level.block.Blocks.FERN.defaultBlockState():
                    h<5?net.minecraft.world.level.block.Blocks.DANDELION.defaultBlockState():
                    net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState();
                putLocal(c,x,y,z,grass);
            }
    }
    private static void putLocal(ChunkAccess c,int x,int y,int z,BlockState state) {
        if(y<c.getMinY()||y>=c.getMaxY())return;
        if((x>>4)!=c.getPos().x()||(z>>4)!=c.getPos().z())return;
        c.setBlockState(new net.minecraft.core.BlockPos(x,y,z),state,0);
    }

    /** Chunk-local winding spokes reaching the outer production belt. */
    private static void paintBeltSpines(BitSet roads,int wx,int wz,Site site,Placement p) {
        int r=KnownWorldC410Fortifications.forTier(site.tier).outerHalfSpan();
        int ox=p.x-site.originX,oz=p.z-site.originZ;
        int west=site.bounds[0]+ox,east=site.bounds[1]+ox;
        int north=site.bounds[2]+oz,south=site.bounds[3]+oz;
        for(int z=wz;z<=wz+15;z++) {
            int a=Math.abs(z-p.z);
            if(a<r+3)continue;
            if((z<p.z && z<north-10)||(z>p.z && z>south+10))continue;
            double k=Math.min(1.0,(a-r-3)/35.0);
            int center=p.x+(int)Math.round(k*(-14+5*Math.sin(z/57.0)+3*Math.sin(z/109.0)));
            for(int x=Math.max(wx,center-1);x<=Math.min(wx+15,center+1);x++)
                roads.set(((z-wz)<<4)|(x-wx));
        }
        for(int x=wx;x<=wx+15;x++) {
            int a=Math.abs(x-p.x);
            if(a<r+3)continue;
            if((x<p.x && x<west-10)||(x>p.x && x>east+10))continue;
            double k=Math.min(1.0,(a-r-3)/35.0);
            int center=p.z+(int)Math.round(k*(-14+5*Math.sin(x/57.0)+3*Math.sin(x/109.0)));
            for(int z=Math.max(wz,center-1);z<=Math.min(wz+15,center+1);z++)
                roads.set(((z-wz)<<4)|(x-wx));
        }
    }

    private static void paintSite(ChunkAccess chunk,Site site,Placement p) {
        int wx=chunk.getPos().getMinBlockX(),wz=chunk.getPos().getMinBlockZ();
        int dx=p.x-site.originX,dz=p.z-site.originZ;
        int radius=KnownWorldC410Fortifications.forTier(site.tier).outerHalfSpan();
        List<Lot> relevant=lotsForChunk(site,p,chunk);
        BitSet roads=new BitSet(256);
        paintBeltSpines(roads,wx,wz,site,p);
        for(Lot b:relevant) {
            int x=b.x+dx,z=b.z+dz;
            if(!intersectsChunk(x-40,z-40,b.w+80,b.d+80,wx,wz,0))continue;
            streetsForLot(roads,wx,wz,site,p,b);
        }
        // Cardinal trunk routes lead to real wall gates; neighborhood roads branch naturally.
        line(roads,wx,wz,p.x,p.z-radius-PITCH,p.x,p.z+radius+PITCH,4);
        line(roads,wx,wz,p.x-radius-PITCH,p.z,p.x+radius+PITCH,p.z,4);
        // Neither a street nor a spur can erase a building footprint.
        for(Lot b:relevant) {
            if("GREEN_POCKET".equals(b.kind))continue;
            int x=b.x+dx,z=b.z+dz;
            int x0=Math.max(wx,x),x1=Math.min(wx+15,x+b.w-1);
            int z0=Math.max(wz,z),z1=Math.min(wz+15,z+b.d-1);
            for(int ix=x0;ix<=x1;ix++)for(int iz=z0;iz<=z1;iz++)
                roads.clear(((iz-wz)<<4)|(ix-wx));
        }
        for(int i=roads.nextSetBit(0);i>=0;i=roads.nextSetBit(i+1))
            KnownWorldC4RoadPainter.paint(chunk,wx+(i&15),wz+(i>>4),site.palette,
                    KnownWorldC411GlobalWorldgen::top,KnownWorldC411GlobalWorldgen::dry,List.of());
        for(Lot b:relevant) {
            int x=b.x+dx,z=b.z+dz;
            if("GREEN_POCKET".equals(b.kind)) {
                if(intersectsChunk(x-4,z-4,b.w+8,b.d+8,wx,wz,0))tree(chunk,site,p,b);
                continue;
            }
            if(!intersectsChunk(x,z,b.w,b.d,wx,wz,0))continue;
            Survey survey=inspect(x,z,b);
            if(!survey.approved) {
                if(reportedLotFailures.putIfAbsent(b.id,Boolean.TRUE)==null) {
                    failedTerrainLots.incrementAndGet();
                    KnownWorld.LOGGER.warn("C4.15 LOT FAILURE site={} lot={} template={} x={} z={} reason={}",
                            site.id,b.id,b.template,x,z,survey.reason);
                }
                continue;
            }
            var lot=new KnownWorldUniversalWorldgenC2.Lot(b.id,b.template,x,z,b.w,b.d,
                    b.face,0,b.staff);
            KnownWorldC3Architecture.buildLot(chunk,lot,survey.floor,site.palette,site.tier);
        }
        if(intersectsChunk(p.x-radius-4,p.z-radius-4,2*radius+9,2*radius+9,wx,wz,0))
            KnownWorldC410Fortifications.paintApprovedNewChunk(chunk,p.x,p.z,site.tier,
                    KnownWorldC411GlobalWorldgen::top);
    }
    /** Preserve B4.1A.1 NPC projection after regenerating the C4.11 worldgen class. */
    public static int[] b41ApprovedPlacementForInspection(String siteId) {
        for(Site site:sites()) {
            if(!site.id.equals(siteId))continue;
            Placement p=placements.computeIfAbsent(site.id, ignored->resolve(site));
            return p.approved?new int[]{p.x,p.z}:null;
        }
        return null;
    }
    public static void decorate(WorldGenLevel level,ChunkAccess chunk) {
        int wx=chunk.getPos().getMinBlockX(),wz=chunk.getPos().getMinBlockZ();
        if(WinterfellWorldgenV3.suppressNaturalDecoration(chunk)
                || KnownWorldUniversalWorldgenC2.suppressNaturalDecoration(chunk))return;
        boolean any=false;
        for(Site site:sites()) {
            if(!potentiallyNear(site,wx,wz))continue;
            Placement p=placements.computeIfAbsent(site.id,ignored->resolve(site));
            if(!p.approved || !touchesSite(site,p,chunk,0))continue;
            paintSite(chunk,site,p);any=true;
        }
        if(any)generatedChunks.incrementAndGet();
    }
    public static String status() {
        return "C4.11 global sites="+sites().size()+" visitedSitePreflights="+startedSites.get()+
                " freshChunksPainted="+generatedChunks.get()+" terrainRejectedLots="+failedTerrainLots.get()+
                " NPCsCreated=0";
    }
}
