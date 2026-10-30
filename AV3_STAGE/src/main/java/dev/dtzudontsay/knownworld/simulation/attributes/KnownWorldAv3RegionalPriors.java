package dev.dtzudontsay.knownworld.simulation.attributes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AV3: read-only cultural/religious *regional correlations*, NOT doctrines or
 * hereditary facts. All numeric source values already belong to the authored
 * regional-influence resources; this class never invents a new culture entry.
 * The extension file overlays a base location rather than double-counting it.
 */
public final class KnownWorldAv3RegionalPriors {
    private static final String ROOT = "/data/knownworld/reference/";
    private static volatile Index cached;

    public record Prior(Map<String,Double> values, Map<String,Double> norms, int locations) {
        public Prior {
            values=Map.copyOf(values);norms=Map.copyOf(norms);
        }
        public boolean hasEvidence() {return !values.isEmpty() || !norms.isEmpty();}
    }
    public record Index(Map<String,Prior> cultures, Map<String,Prior> religions,
                        int locations, int cultureCount, int religionCount, String sourceDigest) {
        public Index {cultures=Map.copyOf(cultures);religions=Map.copyOf(religions);}
        public Prior culture(String key){return cultures.get(key);}
        public Prior religion(String key){return religions.get(key);}
    }
    private static final class Accumulator {
        final Map<String,Double> valueSum=new HashMap<>(), valueWeight=new HashMap<>();
        final Map<String,Double> normSum=new HashMap<>(), normWeight=new HashMap<>();
        int locations;
        void include(JsonObject region,double exposure) {
            locations++;
            contribute(region.getAsJsonObject("values"),exposure,valueSum,valueWeight);
            contribute(region.getAsJsonObject("socialNorms"),exposure,normSum,normWeight);
        }
        Prior freeze() {return new Prior(avg(valueSum,valueWeight),avg(normSum,normWeight),locations);}
    }
    private KnownWorldAv3RegionalPriors() { }

    public static Index index() {
        Index v=cached;if(v!=null)return v;
        synchronized(KnownWorldAv3RegionalPriors.class) {
            v=cached;
            if(v==null) {
                v=build(read("cultures.json"),read("religions.json"),
                    read("regional_influences.json"),read("regional_influence_extensions.json"));
                cached=v;
            }
            return v;
        }
    }
    public static Index build(JsonArray cultures,JsonArray religions,JsonArray base,JsonArray extensions) {
        Set<String> cultureIds=ids(cultures),religionIds=ids(religions);
        Map<String,JsonObject> byLocation=new LinkedHashMap<>();
        for(JsonArray source:new JsonArray[]{base,extensions}) {
            if(source==null)throw new IllegalArgumentException("Missing regional source");
            for(JsonElement item:source) {
                JsonObject row=item.getAsJsonObject();String id=required(row,"locationId");
                if(!id.matches("[a-z0-9_]{2,100}"))throw new IllegalArgumentException("Invalid regional ID");
                JsonObject prior=byLocation.get(id);
                if(prior==null)byLocation.put(id,row.deepCopy());
                else {
                    // The extensions provide refinements to the same location.
                    for(String field:new String[]{"values","socialNorms","cultures","religions"}) {
                        JsonObject addition=row.getAsJsonObject(field);
                        if(addition==null)continue;
                        JsonObject merged=prior.getAsJsonObject(field);
                        if(merged==null){merged=new JsonObject();prior.add(field,merged);}
                        for(var entry:addition.entrySet())merged.add(entry.getKey(),entry.getValue().deepCopy());
                    }
                }
            }
        }
        Map<String,Accumulator> c=new HashMap<>(),r=new HashMap<>();
        for(String key:cultureIds)c.put(key,new Accumulator());
        for(String key:religionIds)r.put(key,new Accumulator());
        for(JsonObject region:byLocation.values()) {
            validateAxes(region.getAsJsonObject("values"),true);
            validateAxes(region.getAsJsonObject("socialNorms"),false);
            attach(region,region.getAsJsonObject("cultures"),c);
            attach(region,region.getAsJsonObject("religions"),r);
        }
        Map<String,Prior> cv=new HashMap<>(),rv=new HashMap<>();
        for(var e:c.entrySet())cv.put(e.getKey(),e.getValue().freeze());
        for(var e:r.entrySet())rv.put(e.getKey(),e.getValue().freeze());
        return new Index(cv,rv,byLocation.size(),cultureIds.size(),religionIds.size(),
            fingerprint(cultures,religions,base,extensions));
    }
    private static void validateAxes(JsonObject source,boolean value) {
        if(source==null)return;
        for(var e:source.entrySet()) {
            try {if(value)CharacterValue.valueOf(e.getKey());else CharacterSocialNorm.valueOf(e.getKey());}
            catch(IllegalArgumentException ex){throw new IllegalArgumentException("Unrecognized AV1 axis in regional source: "+e.getKey(),ex);}
            double v=amount(e.getValue().getAsJsonObject(),"value");
            if(v < -1 || v > 1)throw new IllegalArgumentException("Regional influence outside signed range: "+e.getKey());
        }
    }
    private static void attach(JsonObject region,JsonObject exposure,Map<String,Accumulator> targets) {
        if(exposure==null)return;
        for(var e:exposure.entrySet()) {
            Accumulator target=targets.get(e.getKey());
            if(target==null)throw new IllegalArgumentException("Unknown culture/religion exposure ID: "+e.getKey());
            double weight=amount(e.getValue().getAsJsonObject(),"weight");
            if(weight<0 || weight>1)throw new IllegalArgumentException("Exposure weight out of bounds");
            if(weight>0)target.include(region,weight);
        }
    }
    private static void contribute(JsonObject axes,double weight,Map<String,Double> sum,Map<String,Double> w) {
        if(axes==null)return;
        for(var e:axes.entrySet()){
            double value=amount(e.getValue().getAsJsonObject(),"value");
            sum.merge(e.getKey(),value*weight,Double::sum);w.merge(e.getKey(),weight,Double::sum);
        }
    }
    private static Map<String,Double> avg(Map<String,Double> sum,Map<String,Double> w) {
        Map<String,Double> result=new HashMap<>();
        for(var e:sum.entrySet()){
            double weight=w.getOrDefault(e.getKey(),0.0);
            if(weight>0)result.put(e.getKey(),Math.max(-1,Math.min(1,e.getValue()/weight)));
        }
        return result;
    }
    private static Set<String> ids(JsonArray rows) {
        if(rows==null)throw new IllegalArgumentException("Missing reference catalog");
        Set<String> keys=new HashSet<>();
        for(JsonElement e:rows){String id=required(e.getAsJsonObject(),"id");
            if(!id.matches("[a-z0-9_]{2,100}") || !keys.add(id))throw new IllegalArgumentException("Invalid/duplicate catalog ID: "+id);
        }
        if(keys.isEmpty())throw new IllegalArgumentException("Empty reference catalog");
        return keys;
    }
    private static String required(JsonObject o,String key) {
        JsonElement e=o.get(key);
        if(e==null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Missing text: "+key);
        return e.getAsString();
    }
    private static double amount(JsonObject o,String key) {
        if(o==null)throw new IllegalArgumentException("Missing regional number: "+key);
        JsonElement e=o.get(key);
        if(e==null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Missing regional number: "+key);
        double d=e.getAsDouble();if(!Double.isFinite(d))throw new IllegalArgumentException("Nonfinite regional number");
        return d;
    }
    private static String fingerprint(JsonArray... inputs) {
        try {
            MessageDigest d=MessageDigest.getInstance("SHA-256");
            for(JsonArray input:inputs){d.update(input.toString().getBytes(StandardCharsets.UTF_8));d.update((byte)0);}
            return HexFormat.of().formatHex(d.digest());
        }catch(NoSuchAlgorithmException e){throw new IllegalStateException("Missing SHA-256",e);}
    }
    private static JsonArray read(String filename) {
        try(InputStream stream=KnownWorldAv3RegionalPriors.class.getResourceAsStream(ROOT+filename)) {
            if(stream==null)throw new IllegalStateException("AV3 reference missing: "+filename);
            return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonArray();
        } catch(Exception e) {throw new IllegalStateException("AV3 reference unavailable/corrupt: "+filename,e);}
    }
}
