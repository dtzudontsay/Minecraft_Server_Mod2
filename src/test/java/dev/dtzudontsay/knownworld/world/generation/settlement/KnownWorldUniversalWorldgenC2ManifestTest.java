package dev.dtzudontsay.knownworld.world.generation.settlement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The C2 resource is pilot-only and preserves B2/B3 + C1 reference counts. */
class KnownWorldUniversalWorldgenC2ManifestTest {
    private JsonObject manifest() throws Exception {
        String resource = "data/knownworld/scenarios/agot_298_ac/settlement_construction_c2_v1.json";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "Run py tools/geodata/build_c2_physical_pilots.py");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test void exactlyTwoNonWinterfellWorldgenPilots() throws Exception {
        JsonObject root = manifest();
        assertEquals(1, root.get("schemaVersion").getAsInt());
        assertTrue(root.getAsJsonObject("policy").get("pilotsOnly").getAsBoolean());
        assertTrue(root.getAsJsonObject("policy").get("noNpcCreation").getAsBoolean());
        assertEquals(253, root.getAsJsonObject("summary").get("allMapMarkers").getAsInt());
        assertEquals(259, root.getAsJsonObject("summary").get("allPoliticalHoldings").getAsInt());
        JsonArray pilots = root.getAsJsonArray("pilots");
        assertEquals(2, pilots.size());
        Set<String> tiers = new HashSet<>(), ids = new HashSet<>();
        for (JsonElement e : pilots) {
            JsonObject s = e.getAsJsonObject();
            assertTrue(ids.add(s.get("siteId").getAsString()));
            tiers.add(s.get("tier").getAsString());
            assertEquals("GENERAL_COMPACT_SETTLEMENT", s.get("mode").getAsString());
            assertTrue(s.getAsJsonArray("lots").size() >= 3);
            assertTrue(s.getAsJsonArray("chunks").size() >= 1);
            assertTrue(s.get("noNpcOrJobInstancesCreated").getAsBoolean());
            for (JsonElement marker : s.getAsJsonArray("markerIds"))
                assertNotEquals("NW-029", marker.getAsString());
        }
        assertEquals(Set.of("SMALL", "MEDIUM"), tiers);
    }
}
