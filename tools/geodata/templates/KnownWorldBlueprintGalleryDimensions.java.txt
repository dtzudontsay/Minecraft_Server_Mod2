package dev.dtzudontsay.knownworld.world.generation.settlement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Share dimensions with the actual C3 resource, never hard-code fake gallery sizes. */
public final class KnownWorldBlueprintGalleryDimensions {
    private static volatile Map<String,int[]> sizes;
    private KnownWorldBlueprintGalleryDimensions() { }
    public static int[] dimension(String id) {
        Map<String,int[]> current=sizes;
        if(current==null) synchronized(KnownWorldBlueprintGalleryDimensions.class) {
            if(sizes==null) sizes=read();
            current=sizes;
        }
        int[] n=current.get(id);
        if(n==null)throw new IllegalArgumentException("No footprint metadata for "+id);
        return n.clone();
    }
    private static Map<String,int[]> read() {
        String file="data/knownworld/scenarios/agot_298_ac/architecture_templates_c3_v1.json";
        try(InputStream stream=KnownWorldBlueprintGalleryDimensions.class.getClassLoader().getResourceAsStream(file)) {
            if(stream==null)throw new IllegalStateException("Missing C3 templates for gallery");
            JsonObject data=JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String,int[]> out=new HashMap<>();
            for(JsonElement element:data.getAsJsonArray("templates")) {
                JsonObject j=element.getAsJsonObject();
                int w=j.get("c1Width").getAsInt(),d=j.get("c1Depth").getAsInt();
                if(w<5||d<5||w>64||d>64)throw new IllegalStateException("Bad footprint: "+j.get("id").getAsString());
                out.put(j.get("id").getAsString(),new int[]{w,d});
            }
            return Map.copyOf(out);
        } catch(java.io.IOException err){throw new IllegalStateException("Cannot load gallery dimensions",err);}
    }
}
