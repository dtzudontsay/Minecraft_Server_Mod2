package dev.dtzudontsay.knownworld.world.geography.regions;

import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;

public final class SouthEssosFeatureRegistry {

    private static final String SOURCE_NOTE =
            "Official The Known World regional map: South Essos";

    private static boolean bootstrapped = false;

    private SouthEssosFeatureRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        // ------------------------------------------------------------
        // SOUTHERN DOTHRAKI SEA / SKAHAZADHAN
        // ------------------------------------------------------------

        add(
                "vaes_diaf",
                "Vaes Diaf (Hazdahn No)",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "vaes_efe",
                "Vaes Efe",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "vaes_mejhah",
                "Vaes Mejhah",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "krazaaj_has",
                "Krazaaj Has (Ghardaq)",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "skahazadhan",
                "Skahazadhan",
                GeographicFeatureType.RIVER
        );

        add(
                "meereen",
                "Meereen",
                GeographicFeatureType.CITY
        );

        add(
                "khyzai_pass",
                "Khyzai Pass",
                GeographicFeatureType.PASS
        );

        // ------------------------------------------------------------
        // LHAZAR
        // ------------------------------------------------------------

        add(
                "hesh",
                "Hesh",
                GeographicFeatureType.CITY
        );

        add(
                "kosrak",
                "Kosrak",
                GeographicFeatureType.CITY
        );

        add(
                "lhazar",
                "Lhazar",
                GeographicFeatureType.REGION
        );

        add(
                "lhazosh",
                "Lhazosh",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // SLAVER CITIES / GHISCARI COAST
        // ------------------------------------------------------------

        add(
                "yunkai",
                "Yunkai",
                GeographicFeatureType.CITY
        );

        add(
                "yaros",
                "Yaros",
                GeographicFeatureType.ISLAND
        );

        add(
                "astapor",
                "Astapor",
                GeographicFeatureType.CITY
        );

        add(
                "worm_river",
                "Worm River",
                GeographicFeatureType.RIVER
        );

        // ------------------------------------------------------------
        // RED WASTE / BONE MOUNTAINS
        // ------------------------------------------------------------

        add(
                "vaes_jini",
                "Vaes Jini (Yinishar)",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "adakhakileki",
                "Adakhakileki",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "poison_sea",
                "Poison Sea",
                GeographicFeatureType.SEA
        );

        add(
                "stone_road",
                "Stone Road",
                GeographicFeatureType.ROAD
        );

        add(
                "bone_mountains",
                "Bone Mountains",
                GeographicFeatureType.MOUNTAIN_RANGE
        );

        add(
                "samyriana",
                "Samyriana",
                GeographicFeatureType.CITY
        );

        add(
                "great_sand_sea",
                "Great Sand Sea",
                GeographicFeatureType.REGION
        );

        add(
                "the_red_waste",
                "The Red Waste",
                GeographicFeatureType.REGION
        );

        add(
                "bayasabhad",
                "Bayasabhad",
                GeographicFeatureType.CITY
        );

        add(
                "sand_road",
                "Sand Road",
                GeographicFeatureType.ROAD
        );

        // ------------------------------------------------------------
        // QAATHI RUINS / QARTH
        // ------------------------------------------------------------

        add(
                "vaes_tolorro",
                "Vaes Tolorro",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "vaes_shirosi",
                "Vaes Shirosi",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "vaes_qosar",
                "Vaes Qosar (Qolahn)",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "vaes_orvik",
                "Vaes Orvik",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "port_yhos",
                "Port Yhos",
                GeographicFeatureType.CITY
        );

        add(
                "qarkash",
                "Qarkash",
                GeographicFeatureType.TOWN
        );

        add(
                "qarth",
                "Qarth",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // GHISCAR
        // ------------------------------------------------------------

        add(
                "old_ghis",
                "Old Ghis",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "ghiscar",
                "Ghiscar",
                GeographicFeatureType.PENINSULA
        );

        add(
                "ghiscari_strait",
                "Ghiscari Strait",
                GeographicFeatureType.STRAIT
        );

        add(
                "ghaen",
                "Ghaen",
                GeographicFeatureType.ISLAND
        );

        add(
                "new_ghis",
                "New Ghis",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // SUMMER SEA / JADE GATES
        // ------------------------------------------------------------

        add(
                "the_summer_sea",
                "The Summer Sea",
                GeographicFeatureType.SEA
        );

        add(
                "jade_gates",
                "Jade Gates",
                GeographicFeatureType.STRAIT
        );

        add(
                "qal",
                "Qal",
                GeographicFeatureType.ISLAND
        );

        add(
                "asabhad",
                "Asabhad",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // MORAQ ISLANDS
        // ------------------------------------------------------------

        add(
                "faros",
                "Faros",
                GeographicFeatureType.CITY
        );

        add(
                "great_moraq",
                "Great Moraq",
                GeographicFeatureType.ISLAND
        );

        add(
                "isle_of_whips",
                "Isle of Whips",
                GeographicFeatureType.ISLAND
        );

        add(
                "vahar",
                "Vahar",
                GeographicFeatureType.ISLAND
        );

        add(
                "lesser_moraq",
                "Lesser Moraq",
                GeographicFeatureType.ISLAND
        );

        add(
                "cinnamon_straits",
                "Cinnamon Straits",
                GeographicFeatureType.STRAIT
        );

        add(
                "port_moraq",
                "Port Moraq",
                GeographicFeatureType.CITY
        );

        /*
         * ------------------------------------------------------------
         * OVERLAPS — DO NOT REGISTER AGAIN
         * ------------------------------------------------------------
         *
         * SouthWestEssosFeatureRegistry:
         * - Slaver's Bay
         *
         * NorthEssosFeatureRegistry:
         * - The Dothraki Sea
         *
         * ------------------------------------------------------------
         * DEFERRED TO SOTHORYOS
         * ------------------------------------------------------------
         *
         * - Skull Island
         * - Ax Isle
         * - Zamettar
         * - Gorosh
         * - Wyvern Point
         *
         * These are visible on this crop, but belong to the later
         * Sothoryos registry.
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