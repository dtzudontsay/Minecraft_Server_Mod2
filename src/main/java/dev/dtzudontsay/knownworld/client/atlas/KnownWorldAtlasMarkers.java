package dev.dtzudontsay.knownworld.client.atlas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Immutable source data. The map does not create a settlement or alter terrain. */
public final class KnownWorldAtlasMarkers {
    public record Marker(String id, String name, boolean named,
                         String sources, double masterX, double masterY,
                         String placeId, String status) { }

    private static final String RESOURCE =
            "/assets/knownworld/maps/atlas_markers.json";
    private static final List<Marker> MARKERS = load();

    private KnownWorldAtlasMarkers() { }

    public static List<Marker> all() { return MARKERS; }

    private static List<Marker> load() {
        try (InputStream data = KnownWorldAtlasMarkers.class.getResourceAsStream(RESOURCE)) {
            if (data == null) {
                throw new IllegalStateException("Missing generated " + RESOURCE
                        + ". Run tools/geodata/build_known_world_atlas.py first.");
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(data, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray items = root.getAsJsonArray("markers");
            List<Marker> result = new ArrayList<>(items.size());
            for (JsonElement entry : items) {
                JsonObject obj = entry.getAsJsonObject();
                result.add(new Marker(
                        str(obj, "id"), str(obj, "name"),
                        obj.get("named").getAsBoolean(),
                        str(obj, "sources"),
                        obj.get("masterX").getAsDouble(),
                        obj.get("masterY").getAsDouble(),
                        str(obj, "placeId"), str(obj, "status")));
            }
            return List.copyOf(result);
        } catch (Exception problem) {
            throw new IllegalStateException("Could not load Known World atlas markers", problem);
        }
    }

    private static String str(JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }
}
