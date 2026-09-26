package dev.dtzudontsay.knownworld.world.geography.regions;

import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;

public final class UlthosFeatureRegistry {

    private static final String SOURCE_NOTE =
            "Official The Known World regional map: Ulthos";

    private static boolean bootstrapped = false;

    private UlthosFeatureRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        // ------------------------------------------------------------
        // ULTHOS
        // ------------------------------------------------------------

        add(
                "ulthos",
                "Ulthos",
                GeographicFeatureType.CONTINENT
        );

        add(
                "ulos",
                "Ulos",
                GeographicFeatureType.ISLAND
        );

        /*
         * ------------------------------------------------------------
         * OVERLAPS — DO NOT REGISTER AGAIN
         * ------------------------------------------------------------
         *
         * SouthEastEssosFeatureRegistry:
         * - Manticore Isles
         * - Saffron Straits
         * - Asshai
         * - Ghost Grass
         * - Stygai
         * - The Shadow
         */
    }

    private static void add(
            String id,
            String displayName,
            GeographicFeatureType type
    ) {
        GeographicFeatureRegistry.register(
                new GeographicFeature(
                        id,
                        displayName,
                        type,
                        type.defaultShowEntryTitle(),
                        SourceConfidence.CANON,
                        SOURCE_NOTE
                )
        );
    }
}