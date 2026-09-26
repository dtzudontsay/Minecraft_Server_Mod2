package dev.dtzudontsay.knownworld.world.geography.regions;

import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;

public final class SouthFeatureRegistry {

    private static final String SOURCE_NOTE =
            "Official The Known World regional map: South";

    private static boolean bootstrapped = false;

    private SouthFeatureRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        // ------------------------------------------------------------
        // IRON ISLANDS / WESTERN COAST
        // ------------------------------------------------------------

        add(
                "iron_islands",
                "Iron Islands",
                GeographicFeatureType.ISLAND_GROUP
        );

        add(
                "lonely_light",
                "Lonely Light",
                GeographicFeatureType.ISLAND
        );

        add(
                "great_wyk",
                "Great Wyk",
                GeographicFeatureType.ISLAND
        );

        add(
                "old_wyk",
                "Old Wyk",
                GeographicFeatureType.ISLAND
        );

        add(
                "blacktyde",
                "Blacktyde",
                GeographicFeatureType.ISLAND
        );

        add(
                "orkmont",
                "Orkmont",
                GeographicFeatureType.ISLAND
        );

        add(
                "harlaw",
                "Harlaw",
                GeographicFeatureType.ISLAND
        );

        add(
                "saltcliffe",
                "Saltcliffe",
                GeographicFeatureType.ISLAND
        );

        add(
                "pyke",
                "Pyke",
                GeographicFeatureType.CASTLE
        );

        add(
                "ironmans_bay",
                "Ironman's Bay",
                GeographicFeatureType.BAY
        );

        add(
                "the_crag",
                "The Crag",
                GeographicFeatureType.CASTLE
        );

        add(
                "fair_isle",
                "Fair Isle",
                GeographicFeatureType.ISLAND
        );

        add(
                "faircastle",
                "Faircastle",
                GeographicFeatureType.CASTLE
        );

        add(
                "casterly_rock",
                "Casterly Rock",
                GeographicFeatureType.CASTLE
        );

        add(
                "lannisport",
                "Lannisport",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // RIVERLANDS
        // ------------------------------------------------------------

        add(
                "tumblestone",
                "Tumblestone",
                GeographicFeatureType.RIVER
        );

        add(
                "riverrun",
                "Riverrun",
                GeographicFeatureType.CASTLE
        );

        add(
                "river_road",
                "River Road",
                GeographicFeatureType.ROAD
        );

        add(
                "green_fork",
                "Green Fork",
                GeographicFeatureType.RIVER
        );

        add(
                "blue_fork",
                "Blue Fork",
                GeographicFeatureType.RIVER
        );

        add(
                "red_fork",
                "Red Fork",
                GeographicFeatureType.RIVER
        );

        add(
                "the_trident",
                "The Trident",
                GeographicFeatureType.RIVER
        );

        add(
                "harrenhal",
                "Harrenhal",
                GeographicFeatureType.CASTLE
        );

        add(
                "gods_eye",
                "God's Eye",
                GeographicFeatureType.LAKE
        );

        add(
                "isle_of_faces",
                "Isle of Faces",
                GeographicFeatureType.ISLAND
        );

        // ------------------------------------------------------------
        // VALE
        // ------------------------------------------------------------

        add(
                "mountains_of_the_moon",
                "Mountains of the Moon",
                GeographicFeatureType.MOUNTAIN_RANGE
        );

        add(
                "the_eyrie",
                "The Eyrie",
                GeographicFeatureType.CASTLE
        );

        add(
                "vale_of_arryn",
                "Vale of Arryn",
                GeographicFeatureType.REGION
        );

        add(
                "gulltown",
                "Gulltown",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // CROWNLANDS
        // ------------------------------------------------------------

        add(
                "bay_of_crabs",
                "Bay of Crabs",
                GeographicFeatureType.BAY
        );

        add(
                "crackclaw_point",
                "Crackclaw Point",
                GeographicFeatureType.PENINSULA
        );

        add(
                "claw_isle",
                "Claw Isle",
                GeographicFeatureType.ISLAND
        );

        add(
                "dragonstone",
                "Dragonstone",
                GeographicFeatureType.CASTLE
        );

        add(
                "blackwater_rush",
                "Blackwater Rush",
                GeographicFeatureType.RIVER
        );

        add(
                "blackwater_bay",
                "Blackwater Bay",
                GeographicFeatureType.BAY
        );

        add(
                "kings_landing",
                "King's Landing",
                GeographicFeatureType.CITY
        );

        /*
         * The Kingsroad is visible on this regional map too,
         * but it is already registered by NorthFeatureRegistry.
         *
         * We intentionally do NOT register it twice.
         */

        // ------------------------------------------------------------
        // THE REACH
        // ------------------------------------------------------------

        add(
                "the_goldroad",
                "The Goldroad",
                GeographicFeatureType.ROAD
        );

        add(
                "the_reach",
                "The Reach",
                GeographicFeatureType.REGION
        );

        add(
                "the_roseroad",
                "The Roseroad",
                GeographicFeatureType.ROAD
        );

        add(
                "blueburn",
                "Blueburn",
                GeographicFeatureType.RIVER
        );

        add(
                "mander",
                "Mander",
                GeographicFeatureType.RIVER
        );

        add(
                "cockleswent",
                "Cockleswent",
                GeographicFeatureType.RIVER
        );

        add(
                "ocean_road",
                "Ocean Road",
                GeographicFeatureType.ROAD
        );

        add(
                "shield_islands",
                "Shield Islands",
                GeographicFeatureType.ISLAND_GROUP
        );

        add(
                "green_shield",
                "Green Shield",
                GeographicFeatureType.ISLAND
        );

        add(
                "grey_shield",
                "Grey Shield",
                GeographicFeatureType.ISLAND
        );

        add(
                "oakenshield",
                "Oakenshield",
                GeographicFeatureType.ISLAND
        );

        add(
                "south_shield",
                "South Shield",
                GeographicFeatureType.ISLAND
        );

        add(
                "highgarden",
                "Highgarden",
                GeographicFeatureType.CASTLE
        );

        add(
                "honeywine",
                "Honeywine",
                GeographicFeatureType.RIVER
        );

        add(
                "oldtown",
                "Oldtown",
                GeographicFeatureType.CITY
        );

        add(
                "the_arbor",
                "The Arbor",
                GeographicFeatureType.ISLAND
        );

        add(
                "redwyne_strait",
                "Redwyne Strait",
                GeographicFeatureType.STRAIT
        );

        // ------------------------------------------------------------
        // STORMLANDS
        // ------------------------------------------------------------

        add(
                "kingswood",
                "Kingswood",
                GeographicFeatureType.FOREST
        );

        add(
                "wendwater",
                "Wendwater",
                GeographicFeatureType.RIVER
        );

        add(
                "storms_end",
                "Storm's End",
                GeographicFeatureType.CASTLE
        );

        add(
                "shipbreaker_bay",
                "Shipbreaker Bay",
                GeographicFeatureType.BAY
        );

        add(
                "tarth",
                "Tarth",
                GeographicFeatureType.ISLAND
        );

        add(
                "rainwood",
                "Rainwood",
                GeographicFeatureType.FOREST
        );

        add(
                "cape_wrath",
                "Cape Wrath",
                GeographicFeatureType.CAPE
        );

        add(
                "estermont",
                "Estermont",
                GeographicFeatureType.ISLAND
        );

        // ------------------------------------------------------------
        // DORNISH MARCHES / DORNE
        // ------------------------------------------------------------

        add(
                "dornish_marches",
                "Dornish Marches",
                GeographicFeatureType.REGION
        );

        add(
                "the_red_mountains",
                "The Red Mountains",
                GeographicFeatureType.MOUNTAIN_RANGE
        );

        add(
                "the_princes_pass",
                "The Prince's Pass",
                GeographicFeatureType.PASS
        );

        add(
                "boneway",
                "Boneway",
                GeographicFeatureType.PASS
        );

        add(
                "dorne",
                "Dorne",
                GeographicFeatureType.REGION
        );

        add(
                "brimstone",
                "Brimstone",
                GeographicFeatureType.RIVER
        );

        add(
                "sea_of_dorne",
                "Sea of Dorne",
                GeographicFeatureType.SEA
        );

        add(
                "scourge",
                "Scourge",
                GeographicFeatureType.RIVER
        );

        add(
                "vaith",
                "Vaith",
                GeographicFeatureType.CASTLE
        );

        add(
                "greenblood",
                "Greenblood",
                GeographicFeatureType.RIVER
        );

        add(
                "the_broken_arm",
                "The Broken Arm",
                GeographicFeatureType.PENINSULA
        );

        add(
                "sunspear",
                "Sunspear",
                GeographicFeatureType.CASTLE
        );

        /*
         * Intentionally excluded for now:
         *
         * - Stepstones
         * - labels clipped by the eastern edge
         *
         * Those will be registered from a later regional map where
         * they are fully visible.
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