package dev.dtzudontsay.knownworld.simulation.population;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * B4.2B position-based discovery broadphase. References point exclusively at
 * existing B4.0 persons in B4.1A.1 source tiles. This is NOT a new registry.
 *
 * For a player at a remote workplace or on a commute path, the mobility index
 * makes the worker eligible even when the home is many source tiles away.
 * Final position, loaded-chunk, actor-budget and person-ID checks still belong
 * to KnownWorldB41Activation.
 */
public final class KnownWorldB42BSpatialDiscovery {
    private static final String ROOT = "/data/knownworld/scenarios/agot_298_ac/world_mobility_b4_2b_v1/";
    private static final String VERSION = "knownworld.b4_2b.spatial_discovery.v1";
    private static final int GRID = 256;
    private static final int PARTS = 16;
    private static final int CACHE_LIMIT = 20;
    private static final int QUERY_LIMIT = 12000;
    public record Ref(String homeTile, String personId) { }
    private record Site(String id, Set<Integer> parts) { }
    private static Map<String, Site> sites;
    private static final Map<String, Map<String,List<Ref>>> cache =
            new LinkedHashMap<>(32,.75f,true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String,Map<String,List<Ref>>> e) {
                    return size() > CACHE_LIMIT;
                }
            };
    private static long indexedQueries, examinedRefs;
    private KnownWorldB42BSpatialDiscovery() { }

    public static void reset() {sites=null;cache.clear();indexedQueries=examinedRefs=0;}

    private static Map<String,Site> sites() {
        if (sites!=null)return sites;
        try(InputStream in=KnownWorldB42BSpatialDiscovery.class.getResourceAsStream(ROOT+"manifest.json")) {
            if(in==null)throw new IllegalStateException("Missing B4.2B position discovery index");
            JsonObject doc=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            if(!VERSION.equals(doc.get("schemaVersion").getAsString()) ||
                    doc.get("cellSize").getAsInt()!=GRID || doc.get("partitionCount").getAsInt()!=PARTS ||
                    doc.get("sourceProjectionPopulation").getAsInt()!=3760684 ||
                    doc.get("sourceWorkforce").getAsInt()!=1880341 ||
                    !doc.get("sourceIsUnchanged").getAsBoolean() ||
                    doc.getAsJsonArray("sites").size()!=248)
                throw new IllegalStateException("B4.2B mobility index provenance mismatch");
            Map<String,Site> known=new HashMap<>();
            for(JsonElement e:doc.getAsJsonArray("sites")) {
                JsonObject j=e.getAsJsonObject(); String id=j.get("siteId").getAsString();
                if(!id.matches("site_[a-z][a-z0-9_]{1,90}"))throw new IllegalStateException("Invalid mobility site");
                Set<Integer> parts=new HashSet<>();
                for(JsonElement part:j.getAsJsonArray("parts")) {
                    int i=part.getAsInt();if(i<0||i>=PARTS||!parts.add(i))throw new IllegalStateException("Invalid mobility part");
                }
                if(known.putIfAbsent(id,new Site(id,Set.copyOf(parts)))!=null)
                    throw new IllegalStateException("Duplicate mobility site");
            }
            sites=Map.copyOf(known); return sites;
        } catch(Exception ex) {throw new IllegalStateException("B4.2B discovery index failed to load",ex);}
    }
    private static int part(int x,int z){return ((x*73856093)^(z*19349663))&(PARTS-1);}
    private static Map<String,List<Ref>> bucket(Site site,int bucketId) {
        if(!site.parts().contains(bucketId))return Map.of();
        String key=site.id()+"/"+bucketId;
        Map<String,List<Ref>> stored=cache.get(key);if(stored!=null)return stored;
        String suffix=String.format(java.util.Locale.ROOT,"%02d",bucketId);
        try(InputStream in=KnownWorldB42BSpatialDiscovery.class.getResourceAsStream(ROOT+site.id()+"/p"+suffix+".json.gz")) {
            if(in==null)throw new IllegalStateException("Missing mobility part "+key);
            JsonObject root=JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(in),StandardCharsets.UTF_8)).getAsJsonObject();
            if(!site.id().equals(root.get("siteId").getAsString()) || root.get("part").getAsInt()!=bucketId)
                throw new IllegalStateException("Foreign mobility bucket");
            Map<String,List<Ref>> found=new HashMap<>();
            for(var c:root.getAsJsonObject("cells").entrySet()) {
                String cell=c.getKey();
                if(!cell.matches("-?[0-9]+_-?[0-9]+"))throw new IllegalStateException("Invalid mobility cell");
                String[] xy=cell.split("_");
                if(part(Integer.parseInt(xy[0]),Integer.parseInt(xy[1]))!=bucketId)
                    throw new IllegalStateException("Mobility cell in wrong bucket");
                List<Ref> refs=new ArrayList<>();
                for(JsonElement tuple:c.getValue().getAsJsonArray()) {
                    JsonArray row=tuple.getAsJsonArray();if(row.size()!=2)throw new IllegalStateException("Bad person reference");
                    String home=row.get(0).getAsString(),pid=row.get(1).getAsString();
                    if(!home.matches("-?[0-9]+_-?[0-9]+")||!pid.startsWith("b4:"+site.id()+":"))
                        throw new IllegalStateException("Mobility reference crosses site/identity");
                    refs.add(new Ref(home,pid));
                }
                found.put(cell,List.copyOf(refs));
            }
            Map<String,List<Ref>> result=Map.copyOf(found);cache.put(key,result);return result;
        }catch(Exception ex){throw new IllegalStateException("Cannot load mobility references for "+key,ex);}
    }
    /** Supply local/unshifted x/z. Caller applies terrain-approved site offset. */
    public static List<Ref> nearby(String siteId,int x,int z,long scan) {
        Site site=sites().get(siteId);if(site==null||site.parts().isEmpty())return List.of();
        indexedQueries++;
        int tileX=Math.floorDiv(x,GRID),tileZ=Math.floorDiv(z,GRID);
        List<Ref> found=new ArrayList<>();Set<String> seen=new HashSet<>();
        // Radius=180 < cell width=256; immediate neighboring 3x3 cells suffice.
        for(int tx=tileX-1;tx<=tileX+1;tx++)for(int tz=tileZ-1;tz<=tileZ+1;tz++) {
            String key=tx+"_"+tz;
            for(Ref ref:bucket(site,part(tx,tz)).getOrDefault(key,List.of())) {
                examinedRefs++;
                if(seen.add(ref.personId()))found.add(ref);
            }
        }
        if(found.size()<=QUERY_LIMIT)return found;
        // Rotate a bounded window through a crowded public square. Every
        // individual retains a chance of discovery over successive scans.
        int start=(int)Math.floorMod(scan*QUERY_LIMIT,(long)found.size());
        List<Ref> selected=new ArrayList<>(QUERY_LIMIT);
        for(int i=0;i<QUERY_LIMIT;i++)selected.add(found.get((start+i)%found.size()));
        return selected;
    }
    public static long queries(){return indexedQueries;}
    public static long examined(){return examinedRefs;}
}
