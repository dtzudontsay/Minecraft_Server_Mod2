package dev.dtzudontsay.knownworld.world.geography.regions;

import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;

public final class SothoryosFeatureRegistry {

    private static final String SOURCE_NOTE =
            "Official The Known World regional map: Sothoryos";

    private static boolean bootstrapped = false;

    private SothoryosFeatureRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        // ------------------------------------------------------------
        // NORTHERN ISLANDS / BASILISK ISLES
        // ------------------------------------------------------------

        add(
                "naath",
                "Naath",
                GeographicFeatureType.ISLAND
        );

        add(
                "basilisk_isles",
                "Basilisk Isles",
                GeographicFeatureType.ISLAND_GROUP
        );

        add(
                "skull_island",
                "Skull Island",
                GeographicFeatureType.ISLAND
        );

        add(
                "ax_isle",
                "Ax Isle",
                GeographicFeatureType.ISLAND
        );

        add(
                "isle_of_toads",
                "Isle of Toads",
                GeographicFeatureType.ISLAND
        );

        add(
                "isle_of_tears",
                "Isle of Tears",
                GeographicFeatureType.ISLAND
        );

        add(
                "isle_of_elephants",
                "Isle of Elephants",
                GeographicFeatureType.ISLAND
        );

        // ------------------------------------------------------------
        // SOTHORYOS
        // ------------------------------------------------------------

        add(
                "sothoryos",
                "Sothoryos",
                GeographicFeatureType.REGION
        );

        add(
                "basilisk_point",
                "Basilisk Point",
                GeographicFeatureType.CAPE
        );

        add(
                "wyvern_point",
                "Wyvern Point",
                GeographicFeatureType.CAPE
        );

        add(
                "zamettar",
                "Zamettar",
                GeographicFeatureType.CITY
        );

        add(
                "gogossos",
                "Gogossos",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "yeen",
                "Yeen",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "gorosh",
                "Gorosh",
                GeographicFeatureType.CITY
        );

        add(
                "zabhad",
                "Zabhad",
                GeographicFeatureType.CITY
        );

        /*
         * ------------------------------------------------------------
         * OVERLAPS — DO NOT REGISTER AGAIN
         * ------------------------------------------------------------
         *
         * SouthEssosFeatureRegistry:
         * - The Summer Sea
         * - Faros
         * - Great Moraq
         * - Vahar
         * - Lesser Moraq
         * - Port Moraq
         * - Isle of Whips
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