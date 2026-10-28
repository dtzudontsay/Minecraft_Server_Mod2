package dev.dtzudontsay.knownworld.world.generation.settlement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.data.GeographicDataManager;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * C4.10B opt-in, site-scoped fresh-chunk preview of the C4.10A city layouts.
 *
 * Disabled when the selected-site resource is absent. The resource is written
 * ONLY by the operator's explicit Python --activate step. No retro-generation,
 * no persistent NPC creation, no Winterfell/C2 pilot overwrites. The runtime
 * preflights central dry terrain before indexing; every ordinary lot is also
 * surveyed before painting. Rejected sites/lots remain unmet demand.
 *
 * This is NOT worldwide rollout. It deliberately does not implement complex
 * coastal/canal/rock-specific sites, economy, housing ownership, or an endless
 * terrain solver. Use the C4.11 stage for full worldwide activation.
 */
public final class KnownWorldC410BPreviewWorldgen {
    private static final String RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/universal_construction_c4_10b_preview_v1.json";
    private static final int PITCH = 56;
    private static final int MAX_PREVIEW_SITES = 3;
    private static final int MAX_PREVIEW_LOTS = 12000;
    private static final Set<String> PROTECTED = Set.of("site_nw_029", "site_nw_031", "site_sw_023");
    private static volatile Index shared;
    private static volatile List<ManifestSite> selectedManifest;
    private static final ConcurrentMap<String, Survey> SURVEYS = new ConcurrentHashMap<>();
    private static final AtomicInteger PAINTED_CHUNKS = new AtomicInteger();
    private static final AtomicInteger REJECTED_LOTS = new AtomicInteger();

    private record ManifestSite(String id, String name, String tier, String palette,
                                List<String> markerIds, int x, int z, int population,
                                int homes, int workplaces, int minDx, int maxDx, int minDz, int maxDz,
                                List<RequestedLot> lots) { }
    private record RequestedLot(String id, String kind, String template, int dx, int dz,
                                int width, int depth, int staff, String face, String region,
                                String physicalPermission) { }
    private record Lot(String id, String siteId, String kind, String template, int x, int z,
                       int width, int depth, int staff, String face, String tier, String palette,
                       boolean permitted) { }
    private record Survey(boolean safe, int floor, String reason) { }
    private record SitePlan(String id, String name, String tier, String palette,
                            int x, int z, List<Lot> lots) { }
    private static final class Piece {
        String palette;
        final BitSet roads = new BitSet(256);
        final List<Lot> lots = new ArrayList<>();
        final List<SitePlan> cores = new ArrayList<>();
    }
    private record Index(Map<Long, Piece> chunks, Map<String, String> failedSites,
                         int selectedSites, int approvedSites, int plannedLots,
                         int deferredTemplates) { }

    private KnownWorldC410BPreviewWorldgen() { }
    private static long key(int cx, int cz) { return ((long)cx << 32) ^ (cz & 0xffffffffL); }
    private static boolean dry(int x, int z) {
        var g = KnownWorldGeoSampler.sampleMinecraft(x + 0.5, z + 0.5);
        return g.insideKnownWorldMap() && g.land();
    }
    private static int top(int x, int z) {
        double h = GeographicDataManager.getInstance()
                .sample(WorldProjection.fromMinecraft(x + .5, z + .5)).elevationMetres();
        return (int)Math.max(-63, Math.min(2030, Math.round(h)));
    }

    private static Index data() {
        Index value = shared;
        if (value != null) return value;
        synchronized (KnownWorldC410BPreviewWorldgen.class) {
            if (shared == null) shared = build();
            return shared;
        }
    }

    private static List<ManifestSite> selected() {
        List<ManifestSite> v=selectedManifest;
        if(v!=null)return v;
        synchronized(KnownWorldC410BPreviewWorldgen.class){
            if(selectedManifest==null)selectedManifest=readManifest();
            return selectedManifest;
        }
    }

    /** Avoid expensive terrain preflights when the player explores unrelated regions. */
    private static boolean potentiallySelected(ChunkAccess chunk) {
        int x=chunk.getPos().getMinBlockX(), z=chunk.getPos().getMinBlockZ();
        for(ManifestSite s:selected()) {
            int pad=384+48; // site-anchor search radius plus decoration margin
            long minX=(long)s.x+s.minDx-pad,maxX=(long)s.x+s.maxDx+pad;
            long minZ=(long)s.z+s.minDz-pad,maxZ=(long)s.z+s.maxDz+pad;
            if(x<=maxX && (long)x+15>=minX && z<=maxZ && (long)z+15>=minZ)
                return true;
        }
        return false;
    }

    private static Index build() {
        List<ManifestSite> selections = selected();
        if (selections.isEmpty()) return new Index(Map.of(), Map.of(), 0, 0, 0, 0);
        Map<Long, Piece> chunks = new HashMap<>();
        Map<String, String> failures = new HashMap<>();
        int built = 0, demand = 0, deferred = 0;
        for (ManifestSite source : selections) {
            demand += source.lots.size();
            KnownWorldC410Fortifications.Core cfg = KnownWorldC410Fortifications.forTier(source.tier);
            int radius = cfg.outerHalfSpan();
            // Small, explicit preview search only. C4.11 can retry larger searches
            // offline. Full city demand itself is NOT subject to a radius cap.
            var policy = new KnownWorldC49TerrainPolicy.Policy(
                    384, radius + 8, radius + 264, 32, 145, 32, 12);
            var location = new KnownWorldC49TerrainPolicy.Site(source.id, source.name,
                    source.markerIds, source.x, source.z, source.tier,
                    source.population, source.homes, source.workplaces);
            var terrain = new KnownWorldC49TerrainPolicy.TerrainSampler() {
                @Override public boolean isDryLand(int x, int z) { return dry(x, z); }
                @Override public int surfaceHeight(int x, int z) { return top(x, z); }
            };
            KnownWorldC49TerrainPolicy.Result result =
                    KnownWorldC49TerrainPolicy.find(location, terrain, policy);
            if (!result.successful()) {
                var f = result.failure();
                String reason = f.reason() + " attempted=" + f.attemptedX() + "," + f.attemptedZ();
                failures.put(source.id, reason);
                KnownWorld.LOGGER.warn("C4.10B site={} markerIds={} original={},{}: {} | homes deferred={} workplaces deferred={}",
                        source.id, source.markerIds, source.x, source.z, reason,
                        source.homes, source.workplaces);
                continue;
            }
            int centerX = result.placement().constructionX();
            int centerZ = result.placement().constructionZ();
            if (!coreDryCheck(centerX,centerZ,cfg)) {
                String reason="FULL_CURTAIN_OR_KEEP_LAND_SURVEY_REJECTED";
                failures.put(source.id,reason);
                KnownWorld.LOGGER.warn("C4.10B site={} original={},{} attempt={},{} markerIds={} reason={} homes deferred={} workplaces deferred={}",
                        source.id,source.x,source.z,centerX,centerZ,source.markerIds,reason,
                        source.homes,source.workplaces);
                continue;
            }
            List<Lot> lots = new ArrayList<>();
            SitePlan approved = new SitePlan(source.id,source.name,source.tier,source.palette,
                    centerX,centerZ,lots);
            // Index core walls/castle by intersecting chunk; do not index huge
            // landscape squares outside the fortification itself.
            for (int cx=(centerX-radius-4)>>4;cx<=(centerX+radius+4)>>4;cx++)
                for (int cz=(centerZ-radius-4)>>4;cz<=(centerZ+radius+4)>>4;cz++)
                    cell(chunks,cx,cz).cores.add(approved);
            for (RequestedLot r : source.lots) {
                int x = centerX+r.dx, z = centerZ+r.dz;
                boolean allowed = "C3_MODULE_AVAILABLE".equals(r.physicalPermission);
                if (!allowed) deferred++;
                Lot lot = new Lot(r.id,source.id,r.kind,r.template,x,z,r.width,r.depth,
                        r.staff,r.face,source.tier,source.palette,allowed);
                lots.add(lot);
                if (!allowed) continue;
                for (int cx=x>>4;cx<=(x+lot.width-1)>>4;cx++)
                    for (int cz=z>>4;cz<=(z+lot.depth-1)>>4;cz++)
                        cell(chunks,cx,cz).lots.add(lot);
                // Street edges around each occupied plot tile: deterministic
                // connected ring lattice, with no roads through plot interiors.
                int mx=x+lot.width/2, mz=z+lot.depth/2;
                if ("INNER_WARD".equals(r.region)) continue;
                int left=mx-PITCH/2, right=mx+PITCH/2;
                int north=mz-PITCH/2, south=mz+PITCH/2;
                for (int xx=left;xx<=right;xx++) {
                    for(int d=-1;d<=1;d++){
                        markRoad(chunks,xx,north+d,source.palette);markRoad(chunks,xx,south+d,source.palette);
                    }
                }
                for (int zz=north;zz<=south;zz++) {
                    for(int d=-1;d<=1;d++){
                        markRoad(chunks,left+d,zz,source.palette);markRoad(chunks,right+d,zz,source.palette);
                    }
                }
            }
            // Four gates receive connecting roads. Each street pixel still
            // needs the C4.2 land/grade acceptance in the chunk painter.
            for (int d=-radius-PITCH;d<=radius+PITCH;d++)
                for(int w=-2;w<=2;w++) {
                    markRoad(chunks,centerX+w,centerZ+d,source.palette);
                    markRoad(chunks,centerX+d,centerZ+w,source.palette);
                }
            built++;
            KnownWorld.LOGGER.info("C4.10B preview APPROVED {} {} map={},{} shifted={},{} lots={} (physical templates may still defer lots)",
                    source.id,source.name,source.x,source.z,centerX,centerZ,lots.size());
        }
        KnownWorld.LOGGER.info("C4.10B opt-in manifest selected={} approved={} lotsDemand={} deferredTemplates={} indexedChunks={} NPCs=0",selections.size(),built,demand,deferred,chunks.size());
        return new Index(Map.copyOf(chunks),Map.copyOf(failures),selections.size(),built,demand,deferred);
    }

    private static boolean coreDryCheck(int cx,int cz,KnownWorldC410Fortifications.Core cfg) {
        int r=cfg.outerHalfSpan();
        // Fine-sample actual wall line and central keep after the coarse C4.9
        // search. Never paint the core over a narrow harbor or lake inlet.
        for (int p=-r;p<=r;p+=4) {
            if (!dry(cx-r,cz+p) || !dry(cx+r,cz+p) ||
                !dry(cx+p,cz-r) || !dry(cx+p,cz+r)) return false;
        }
        for (int x=cfg.keepWest();x<=cfg.keepEast();x+=3)
            for (int z=cfg.keepNorth();z<=cfg.keepSouth();z+=3)
                if (!dry(cx+x,cz+z)) return false;
        return true;
    }

    private static Piece cell(Map<Long,Piece> m,int cx,int cz) {
        return m.computeIfAbsent(key(cx,cz),ignored -> new Piece());
    }
    private static void markRoad(Map<Long,Piece> m,int x,int z,String palette) {
        int cx=x>>4,cz=z>>4;
        Piece piece=cell(m,cx,cz);
        if(piece.palette!=null&&!piece.palette.equals(palette))
            throw new IllegalStateException("C4.10B preview road palette collision");
        piece.palette=palette;
        piece.roads.set(((z&15)<<4)|(x&15));
    }

    private static List<ManifestSite> readManifest() {
        try (InputStream stream = KnownWorldC410BPreviewWorldgen.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (stream == null) return List.of(); // fail-closed default
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream,
                    StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt()!=1
                    || !root.get("previewOnly").getAsBoolean()
                    || !root.get("noNpcCreation").getAsBoolean()
                    || !root.get("worldgenEnabled").getAsBoolean())
                throw new IllegalStateException("Invalid C4.10B preview guard flags");
            JsonArray sites = root.getAsJsonArray("sites");
            if (sites.size()<1 || sites.size()>MAX_PREVIEW_SITES ||
                root.get("siteCount").getAsInt()!=sites.size())
                throw new IllegalStateException("C4.10B preview selection size invalid");
            List<ManifestSite> result=new ArrayList<>();Set<String> ids=new HashSet<>();
            int total=0;
            for (JsonElement entry:sites) {
                JsonObject s=entry.getAsJsonObject();String id=s.get("siteId").getAsString();
                if (!ids.add(id)||PROTECTED.contains(id))
                    throw new IllegalStateException("C4.10B duplicate/protected site="+id);
                String tier=s.get("tier").getAsString();
                KnownWorldC410Fortifications.forTier(tier);
                List<String> markers=new ArrayList<>();
                for(JsonElement e:s.getAsJsonArray("markerIds")) markers.add(e.getAsString());
                List<RequestedLot> lots=new ArrayList<>();Set<String> lotIds=new HashSet<>();
                int half=KnownWorldC410Fortifications.forTier(tier).outerHalfSpan()+4;
                int minDx=-half,maxDx=half,minDz=-half,maxDz=half;
                for(JsonElement e:s.getAsJsonArray("lots")) {
                    JsonObject j=e.getAsJsonObject();String lid=j.get("id").getAsString();
                    if (!lotIds.add(lid))throw new IllegalStateException("Duplicate preview lot="+lid);
                    int w=j.get("width").getAsInt(),d=j.get("depth").getAsInt();
                    if(w<5||d<5||w>64||d>64)throw new IllegalStateException("Invalid preview lot size="+lid);
                    int dx=j.get("dx").getAsInt(),dz=j.get("dz").getAsInt();
                    minDx=Math.min(minDx,dx);maxDx=Math.max(maxDx,dx+w);
                    minDz=Math.min(minDz,dz);maxDz=Math.max(maxDz,dz+d);
                    lots.add(new RequestedLot(lid,j.get("kind").getAsString(),
                        j.get("template").getAsString(),dx,dz,w,d,j.get("staff").getAsInt(),
                        j.get("face").getAsString(),j.get("region").getAsString(),
                        j.get("physicalPermission").getAsString()));
                }
                total+=lots.size();
                if(total>MAX_PREVIEW_LOTS)throw new IllegalStateException("Preview safety budget exceeded");
                int homes=s.get("homeCount").getAsInt(),work=s.get("workplaceCount").getAsInt();
                if(homes+work!=lots.size())throw new IllegalStateException("Demand lost at "+id);
                result.add(new ManifestSite(id,s.get("name").getAsString(),tier,
                   s.get("palette").getAsString(),List.copyOf(markers),
                   s.get("originalX").getAsInt(),s.get("originalZ").getAsInt(),
                   s.get("populationTarget").getAsInt(),homes,work,
                   minDx,maxDx,minDz,maxDz,List.copyOf(lots)));
            }
            return List.copyOf(result);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read C4.10B preview manifest",e);
        }
    }

    /** Tree/lava/vanilla-structure reservation includes the same 2 chunk margin as C2. */
    public static boolean suppressNaturalDecoration(ChunkAccess chunk) {
        if(!potentiallySelected(chunk))return false;
        Index index=data();if(index.chunks.isEmpty())return false;
        int cx=chunk.getPos().x(),cz=chunk.getPos().z();
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)
            if(index.chunks.containsKey(key(cx+dx,cz+dz)))return true;
        return false;
    }
    public static boolean suppressVanillaStructures(ChunkAccess chunk) {
        return suppressNaturalDecoration(chunk);
    }

    private static Survey inspect(Lot lot) {
        int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE;
        for(int dx=0;dx<lot.width;dx+=2)for(int dz=0;dz<lot.depth;dz+=2){
            int x=lot.x+dx,z=lot.z+dz;
            if(!dry(x,z))return new Survey(false,0,"WATER_OR_COAST_INTERSECTS_LOT");
            int h=top(x,z);lo=Math.min(lo,h);hi=Math.max(hi,h);
        }
        int[] heights=new int[9];int i=0;
        for(int dx:new int[]{1,lot.width/2,lot.width-2})
            for(int dz:new int[]{1,lot.depth/2,lot.depth-2})
                heights[i++]=top(lot.x+dx,lot.z+dz);
        Arrays.sort(heights);int floor=heights[5];
        int doorX=lot.x+lot.width/2;
        int doorZ="NORTH".equals(lot.face)?lot.z-2:lot.z+lot.depth+1;
        if(!dry(doorX,doorZ)||Math.abs(top(doorX,doorZ)-floor)>5)
            return new Survey(false,floor,"INACCESSIBLE_DOOR");
        if(hi-lo>6 || floor<-48 || floor>1980)
            return new Survey(false,floor,"LOT_RELIEF_OR_HEIGHT_EXCEEDS_SAFE_TERRACE");
        return new Survey(true,floor,"OK");
    }

    private static String paletteFor(Piece p) {
        if(!p.cores.isEmpty())return p.cores.get(0).palette;
        if(!p.lots.isEmpty())return p.lots.get(0).palette;
        return p.palette==null?"NORTHERN":p.palette;
    }

    public static void decorate(WorldGenLevel level,ChunkAccess chunk) {
        if(!potentiallySelected(chunk))return;
        Piece p=data().chunks.get(key(chunk.getPos().x(),chunk.getPos().z()));
        if(p==null)return;
        int wx=chunk.getPos().getMinBlockX(),wz=chunk.getPos().getMinBlockZ();
        // Roads first; main castle body/gates painted last to avoid damaging
        // curtain walls or central foundation. Per-pixel road painter rejects
        // steep grade and any water rather than extending into the sea.
        for(int bit=p.roads.nextSetBit(0);bit>=0;bit=p.roads.nextSetBit(bit+1)){
            int x=wx+(bit&15),z=wz+(bit>>4);
            KnownWorldC4RoadPainter.paint(chunk,x,z,
                    paletteFor(p),
                    KnownWorldC410BPreviewWorldgen::top,
                    KnownWorldC410BPreviewWorldgen::dry,List.of());
        }
        for(Lot b:p.lots){
            Survey s=SURVEYS.computeIfAbsent(b.id,ignored->{
                Survey result=inspect(b);
                if(!result.safe){
                    REJECTED_LOTS.incrementAndGet();
                    KnownWorld.LOGGER.warn("C4.10B UNSAFE LOT site={} lot={} template={} x={} z={} reason={}",
                            b.siteId,b.id,b.template,b.x,b.z,result.reason);
                }
                return result;
            });
            if(!s.safe){
                // Per-site/lot structure failures are logged once via cache.
                continue;
            }
            String template=b.template;
            var physical=new KnownWorldUniversalWorldgenC2.Lot(b.id,template,b.x,b.z,
                    b.width,b.depth,b.face,0,b.staff);
            // Already surveyed and explicitly allowed at the selected site.
            KnownWorldC3Architecture.buildLot(chunk,physical,s.floor,b.palette,b.tier);
        }
        for(SitePlan s:p.cores)
            KnownWorldC410Fortifications.paintApprovedNewChunk(chunk,s.x,s.z,s.tier,
                    KnownWorldC410BPreviewWorldgen::top);
        PAINTED_CHUNKS.incrementAndGet();
    }

    /** Status is accurate for preview runtime only; not a settlement census. */
    public static String inspectStatus() {
        Index idx=data();
        return "C4.10B preview selected="+idx.selectedSites+" terrainApproved="+idx.approvedSites+
                " plannedLots="+idx.plannedLots+" deferredTemplates="+idx.deferredTemplates+
                " indexedChunks="+idx.chunks.size()+" generatedChunks="+PAINTED_CHUNKS.get()+
                " rejectedTerrainLots="+REJECTED_LOTS.get()+
                " terrainFailures="+idx.failedSites+" noNPCsCreated=true";
    }
}
