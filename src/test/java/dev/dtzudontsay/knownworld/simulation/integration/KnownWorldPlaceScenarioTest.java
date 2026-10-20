
package dev.dtzudontsay.knownworld.simulation.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class KnownWorldPlaceScenarioTest {

    private static final String RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/settlements.json";

    private static JsonArray settlements() throws Exception {

        try (InputStream stream =
                     KnownWorldPlaceScenarioTest.class
                             .getClassLoader()
                             .getResourceAsStream(RESOURCE)) {

            assertNotNull(
                    stream,
                    "Scenario settlement resource must exist"
            );

            return JsonParser.parseReader(
                    new InputStreamReader(
                            stream,
                            StandardCharsets.UTF_8
                    )
            ).getAsJsonArray();
        }
    }

    @Test
    void winterfellHasExactlyOneScenarioRecord()
            throws Exception {

        int count = 0;

        for (var entry : settlements()) {

            JsonObject settlement = entry.getAsJsonObject();

            if ("winterfell".equals(
                    settlement.get("id").getAsString()
            )) {
                count++;
            }
        }

        assertEquals(
                1,
                count,
                "Winterfell must have one authored settlement"
        );
    }

    @Test
    void winterfellUsesPhysicalV3Region()
            throws Exception {

        JsonObject winterfell = null;

        for (var entry : settlements()) {

            JsonObject candidate =
                    entry.getAsJsonObject();

            if ("winterfell".equals(
                    candidate.get("id").getAsString()
            )) {

                winterfell = candidate;
                break;
            }
        }

        assertNotNull(winterfell);

        JsonObject position =
                winterfell.getAsJsonObject("position");

        double x = position.get("x").getAsDouble();
        double y = position.get("y").getAsDouble();
        double z = position.get("z").getAsDouble();

        assertEquals(
                "minecraft:overworld",
                position.get("dimension").getAsString()
        );

        assertTrue(Double.isFinite(x));
        assertTrue(Double.isFinite(y));
        assertTrue(Double.isFinite(z));

        assertTrue(
                Math.abs(x - (-4_050_173)) <= 256,
                "Winterfell X must remain near the V3 origin"
        );

        assertTrue(
                Math.abs(z - (-1_976_496)) <= 256,
                "Winterfell Z must remain near the V3 origin"
        );

        assertFalse(
                Math.abs(x - 3.5) < 0.01
                        && Math.abs(z - 3.5) < 0.01,
                "The old world-origin placeholder is forbidden"
        );
    }

    @Test
    void scenarioStillClassifiesWinterfellAsCastle()
            throws Exception {

        for (var element : settlements()) {

            JsonObject settlement =
                    element.getAsJsonObject();

            if ("winterfell".equals(
                    settlement.get("id").getAsString()
            )) {

                assertEquals(
                        "CASTLE",
                        settlement.get("type").getAsString()
                );

                assertEquals(
                        "Winterfell",
                        settlement.get("name").getAsString()
                );

                return;
            }
        }

        fail("Winterfell scenario definition is missing");
    }

    @Test
    void domainsAreNotAssumedToBePhysicalSeats() {

        assertFalse(
                KnownWorldPlacesRuntime.mayHavePhysicalSeat(
                        dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType.REGION
                )
        );

        assertFalse(
                KnownWorldPlacesRuntime.mayHavePhysicalSeat(
                        dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType.TERRITORY
                )
        );

        assertTrue(
                KnownWorldPlacesRuntime.mayHavePhysicalSeat(
                        dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType.CASTLE
                )
        );

        assertTrue(
                KnownWorldPlacesRuntime.mayHavePhysicalSeat(
                        dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType.VILLAGE
                )
        );

        assertTrue(
                KnownWorldPlacesRuntime.mayHavePhysicalSeat(
                        dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType.FORTRESS
                )
        );
    }
}
