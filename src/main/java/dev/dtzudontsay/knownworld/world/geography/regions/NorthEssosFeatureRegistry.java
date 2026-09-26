package dev.dtzudontsay.knownworld.world.geography.regions;

import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;

public final class NorthEssosFeatureRegistry {

    private static final String SOURCE_NOTE =
            "Official The Known World regional map: North Essos";

    private static boolean bootstrapped = false;

    private NorthEssosFeatureRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        // ------------------------------------------------------------
        // IBBEN / NORTHERN ISLANDS
        // ------------------------------------------------------------

        add(
                "ib_nor",
                "Ib Nor",
                GeographicFeatureType.CITY
        );

        add(
                "ib",
                "Ib",
                GeographicFeatureType.ISLAND
        );

        add(
                "port_of_ibben",
                "Port of Ibben",
                GeographicFeatureType.CITY
        );

        add(
                "bay_of_whales",
                "Bay of Whales",
                GeographicFeatureType.BAY
        );

        add(
                "far_ib",
                "Far Ib",
                GeographicFeatureType.ISLAND
        );

        add(
                "ib_sar",
                "Ib Sar",
                GeographicFeatureType.CITY
        );

        add(
                "new_ibbish",
                "New Ibbish",
                GeographicFeatureType.CITY
        );

        add(
                "vaes_aresak",
                "Vaes Aresak (Ibbish)",
                GeographicFeatureType.RUINED_CITY
        );

        // ------------------------------------------------------------
        // IFEQUEVRON / WESTERN NORTH ESSOS
        // ------------------------------------------------------------

        add(
                "vaes_leisi",
                "Vaes Leisi",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "kingdoms_of_the_ifequevron",
                "Kingdoms of the Ifequevron",
                GeographicFeatureType.REGION
        );

        // ------------------------------------------------------------
        // JHOGWIN / BONE MOUNTAINS APPROACH
        // ------------------------------------------------------------

        add(
                "realm_of_the_jhogwin",
                "Realm of the Jhogwin",
                GeographicFeatureType.REGION
        );

        add(
                "krazaaj_zasqa",
                "Krazaaj Zasqa",
                GeographicFeatureType.MOUNTAIN_RANGE
        );

        add(
                "howling_hills",
                "Howling Hills",
                GeographicFeatureType.HILLS
        );

        add(
                "kayakayanaya",
                "Kayakayanaya",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // VAES DOTHRAK / DOTHRAKI HEARTLAND
        // ------------------------------------------------------------

        add(
                "womb_of_the_world",
                "Womb of the World",
                GeographicFeatureType.LAKE
        );

        add(
                "mother_of_mountains",
                "Mother of Mountains",
                GeographicFeatureType.MOUNTAIN
        );

        add(
                "vaes_dothrak",
                "Vaes Dothrak",
                GeographicFeatureType.CITY
        );

        add(
                "steel_road",
                "Steel Road",
                GeographicFeatureType.ROAD
        );

        add(
                "the_dothraki_sea",
                "The Dothraki Sea",
                GeographicFeatureType.REGION
        );

        /*
         * ------------------------------------------------------------
         * OVERLAPS — DO NOT REGISTER AGAIN
         * ------------------------------------------------------------
         *
         * NorthWestEssosFeatureRegistry:
         * - Vaes Athjikhari (Sallosh)
         * - Vaes Leqse (Gornath)
         * - Yalli Qamayi (Sathar)
         * - Bay of Tusks
         * - The Shivering Sea
         *
         * Westeros registries:
         * - cut-off western landmasses
         * - Skagos
         *
         * ------------------------------------------------------------
         * DEFERRED
         * ------------------------------------------------------------
         *
         * - Great Sand Sea
         *
         * It belongs to the later southern/eastern Essos crop.
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