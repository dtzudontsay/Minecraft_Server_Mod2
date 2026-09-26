package dev.dtzudontsay.knownworld.world.geography.regions;

import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;

public final class SouthEastEssosFeatureRegistry {

    private static final String SOURCE_NOTE =
            "Official The Known World regional map: South-East Essos";

    private static boolean bootstrapped = false;

    private SouthEastEssosFeatureRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        // ------------------------------------------------------------
        // YI TI
        // ------------------------------------------------------------

        add(
                "yi_ti",
                "Yi Ti",
                GeographicFeatureType.REGION
        );

        add(
                "yin",
                "Yin",
                GeographicFeatureType.CITY
        );

        add(
                "jinqi",
                "Jinqi",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // LENG
        // ------------------------------------------------------------

        add(
                "leng",
                "Leng",
                GeographicFeatureType.ISLAND
        );

        add(
                "leng_yi",
                "Leng Yi",
                GeographicFeatureType.CITY
        );

        add(
                "leng_ma",
                "Leng Ma",
                GeographicFeatureType.CITY
        );

        add(
                "turrani",
                "Turrani",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // JADE SEA ISLANDS
        // ------------------------------------------------------------

        add(
                "jade_sea",
                "Jade Sea",
                GeographicFeatureType.SEA
        );

        add(
                "marahai",
                "Marahai",
                GeographicFeatureType.ISLAND
        );

        add(
                "manticore_isles",
                "Manticore Isles",
                GeographicFeatureType.ISLAND_GROUP
        );

        // ------------------------------------------------------------
        // FAR EAST / MOUNTAINS OF THE MORN
        // ------------------------------------------------------------

        add(
                "mountains_of_the_morn",
                "Mountains of the Morn",
                GeographicFeatureType.MOUNTAIN_RANGE
        );

        add(
                "city_of_the_winged_men",
                "City of the Winged Men",
                GeographicFeatureType.CITY
        );

        add(
                "hidden_sea",
                "Hidden Sea",
                GeographicFeatureType.SEA
        );

        add(
                "carcosa",
                "Carcosa",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // SHADOW LANDS
        // ------------------------------------------------------------

        add(
                "ghost_grass",
                "Ghost Grass",
                GeographicFeatureType.GRASSLAND
        );

        add(
                "the_shadow",
                "The Shadow",
                GeographicFeatureType.REGION
        );

        add(
                "stygai",
                "Stygai",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "asshai",
                "Asshai",
                GeographicFeatureType.CITY
        );

        add(
                "saffron_straits",
                "Saffron Straits",
                GeographicFeatureType.STRAIT
        );

        /*
         * ------------------------------------------------------------
         * OVERLAPS — DO NOT REGISTER AGAIN
         * ------------------------------------------------------------
         *
         * SouthEssosFeatureRegistry:
         * - Isle of Whips
         *
         * ------------------------------------------------------------
         * DEFERRED TO ULTHOS
         * ------------------------------------------------------------
         *
         * - Ulos
         * - Ulthos
         *
         * Both are visible at the south-east edge but will belong
         * to the future Ulthos registry.
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