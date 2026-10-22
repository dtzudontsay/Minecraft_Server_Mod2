package dev.dtzudontsay.knownworld.client.atlas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Optional read-only atlas labels, sourced from C1 physical site presets. */
public final class KnownWorldAtlasSiteTags {
    private static final String RESOURCE = "/assets/knownworld/maps/atlas_site_tiers_c48.json";
    private static final Map<String, String> TAGS = load();

    private KnownWorldAtlasSiteTags() { }

    public static String suffix(String markerId) {
        String tag = TAGS.get(markerId);
        return tag == null || tag.isEmpty() ? "" : "  [" + tag + "]";
    }

    private static Map<String, String> load() {
        try (InputStream in = KnownWorldAtlasSiteTags.class.getResourceAsStream(RESOURCE)) {
            if (in == null) return Map.of(); // Fail open: an atlas must work without optional labels.
            JsonObject doc = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            if (doc.get("schemaVersion").getAsInt() != 1) return Map.of();
            JsonArray markers = doc.getAsJsonArray("markers");
            Map<String, String> result = new HashMap<>();
            for (JsonElement entry : markers) {
                JsonObject item = entry.getAsJsonObject();
                String id = item.get("id").getAsString();
                String tag = item.get("tag").getAsString();
                if (!(tag.equals("SMALL") || tag.equals("MEDIUM") || tag.equals("MAJOR")
                        || tag.equals("SPECIAL") || tag.equals("RUIN"))) continue;
                result.put(id, tag);
            }
            return Map.copyOf(result);
        } catch (Exception ignored) {
            return Map.of(); // Display remains usable even with a malformed optional tier resource.
        }
    }
}
