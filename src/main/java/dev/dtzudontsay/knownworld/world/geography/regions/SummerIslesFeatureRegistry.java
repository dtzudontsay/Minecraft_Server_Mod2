package dev.dtzudontsay.knownworld.world.geography.regions;

import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;

public final class SummerIslesFeatureRegistry {

    private static final String SOURCE_NOTE =
            "Official The Known World regional map: Summer Isles";

    private static boolean bootstrapped = false;

    private SummerIslesFeatureRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        // ------------------------------------------------------------
        // SURROUNDING SEAS / ARCHIPELAGO
        // ------------------------------------------------------------

        add(
                "the_sunset_sea",
                "The Sunset Sea",
                GeographicFeatureType.SEA
        );

        add(
                "the_summer_isles",
                "The Summer Isles",
                GeographicFeatureType.ISLAND_GROUP
        );

        add(
                "the_smiling_sea",
                "The Smiling Sea",
                GeographicFeatureType.SEA
        );

        add(
                "indigo_straits",
                "Indigo Straits",
                GeographicFeatureType.STRAIT
        );

        // ------------------------------------------------------------
        // NORTHERN SUMMER ISLES
        // ------------------------------------------------------------

        add(
                "stone_head",
                "Stone Head",
                GeographicFeatureType.ISLAND
        );

        add(
                "walano",
                "Walano",
                GeographicFeatureType.ISLAND
        );

        add(
                "lotus_port",
                "Lotus Port",
                GeographicFeatureType.CITY
        );

        add(
                "isle_of_women",
                "Isle of Women",
                GeographicFeatureType.ISLAND
        );

        add(
                "koj",
                "Koj",
                GeographicFeatureType.ISLAND
        );

        add(
                "isle_of_birds",
                "Isle of Birds",
                GeographicFeatureType.ISLAND
        );

        add(
                "omboru",
                "Omboru",
                GeographicFeatureType.ISLAND
        );

        // ------------------------------------------------------------
        // WESTERN SMALL ISLAND GROUPS
        // ------------------------------------------------------------

        add(
                "the_singing_stones",
                "The Singing Stones",
                GeographicFeatureType.ISLAND_GROUP
        );

        add(
                "the_three_exiles",
                "The Three Exiles",
                GeographicFeatureType.ISLAND_GROUP
        );

        add(
                "isle_of_love",
                "Isle of Love",
                GeographicFeatureType.ISLAND
        );

        add(
                "moluu",
                "Moluu",
                GeographicFeatureType.ISLAND
        );

        // ------------------------------------------------------------
        // JHALA AND SOUTHERN ISLES
        // ------------------------------------------------------------

        add(
                "jhala",
                "Jhala",
                GeographicFeatureType.ISLAND
        );

        add(
                "sweet_lotus_vale",
                "Sweet Lotus Vale",
                GeographicFeatureType.VALLEY
        );

        add(
                "red_flower_vale",
                "Red Flower Vale",
                GeographicFeatureType.VALLEY
        );

        add(
                "parrot_bay",
                "Parrot Bay",
                GeographicFeatureType.BAY
        );

        add(
                "xon",
                "Xon",
                GeographicFeatureType.ISLAND
        );

        add(
                "doquu",
                "Doquu",
                GeographicFeatureType.ISLAND
        );

        add(
                "the_bones",
                "The Bones",
                GeographicFeatureType.ISLAND_GROUP
        );

        add(
                "golden_head",
                "Golden Head",
                GeographicFeatureType.CAPE
        );

        add(
                "lizard_head",
                "Lizard Head",
                GeographicFeatureType.ISLAND
        );
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