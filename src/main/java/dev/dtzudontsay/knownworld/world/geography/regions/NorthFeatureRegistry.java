package dev.dtzudontsay.knownworld.world.geography.regions;

import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;

public final class NorthFeatureRegistry {

    private static final String SOURCE_NOTE =
            "Official The Known World regional map: North";

    private static boolean bootstrapped = false;

    private NorthFeatureRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        add(
                "lands_of_always_winter",
                "The Lands of Always Winter",
                GeographicFeatureType.REGION
        );

        add(
                "frostfangs",
                "The Frostfangs",
                GeographicFeatureType.MOUNTAIN_RANGE
        );

        add(
                "haunted_forest",
                "The Haunted Forest",
                GeographicFeatureType.FOREST
        );

        add(
                "milkwater",
                "The Milkwater",
                GeographicFeatureType.RIVER
        );

        add(
                "frozen_shore",
                "The Frozen Shore",
                GeographicFeatureType.COAST
        );

        add(
                "bay_of_ice",
                "Bay of Ice",
                GeographicFeatureType.BAY
        );

        add(
                "bear_island",
                "Bear Island",
                GeographicFeatureType.ISLAND
        );

        add(
                "the_wall",
                "The Wall",
                GeographicFeatureType.WALL
        );

        add(
                "castle_black",
                "Castle Black",
                GeographicFeatureType.CASTLE
        );

        add(
                "brandons_gift",
                "Brandon's Gift",
                GeographicFeatureType.REGION
        );

        add(
                "the_new_gift",
                "The New Gift",
                GeographicFeatureType.REGION
        );

        add(
                "skane",
                "Skane",
                GeographicFeatureType.ISLAND
        );

        add(
                "skagos",
                "Skagos",
                GeographicFeatureType.ISLAND
        );

        add(
                "bay_of_seals",
                "Bay of Seals",
                GeographicFeatureType.BAY
        );

        add(
                "sea_dragon_point",
                "Sea Dragon Point",
                GeographicFeatureType.PENINSULA
        );

        add(
                "wolfswood",
                "Wolfswood",
                GeographicFeatureType.FOREST
        );

        add(
                "stony_shore",
                "Stony Shore",
                GeographicFeatureType.COAST
        );

        add(
                "the_rills",
                "The Rills",
                GeographicFeatureType.REGION
        );

        add(
                "barrowtown",
                "Barrowtown",
                GeographicFeatureType.SETTLEMENT
        );

        add(
                "blazewater_bay",
                "Blazewater Bay",
                GeographicFeatureType.BAY
        );

        add(
                "saltspear",
                "Saltspear",
                GeographicFeatureType.BAY
        );

        add(
                "cape_kraken",
                "Cape Kraken",
                GeographicFeatureType.PENINSULA
        );

        add(
                "barrowlands",
                "Barrowlands",
                GeographicFeatureType.REGION
        );

        add(
                "kingsroad",
                "The Kingsroad",
                GeographicFeatureType.ROAD
        );

        add(
                "the_neck",
                "The Neck",
                GeographicFeatureType.REGION
        );

        add(
                "winterfell",
                "Winterfell",
                GeographicFeatureType.CASTLE
        );

        add(
                "lonely_hills",
                "Lonely Hills",
                GeographicFeatureType.HILLS
        );

        add(
                "last_river",
                "The Last River",
                GeographicFeatureType.RIVER
        );

        add(
                "dreadfort",
                "The Dreadfort",
                GeographicFeatureType.CASTLE
        );

        add(
                "weeping_water",
                "Weeping Water",
                GeographicFeatureType.RIVER
        );

        add(
                "grey_cliffs",
                "Grey Cliffs",
                GeographicFeatureType.CLIFFS
        );

        add(
                "sheepshead_hills",
                "Sheepshead Hills",
                GeographicFeatureType.HILLS
        );

        add(
                "broken_branch",
                "Broken Branch",
                GeographicFeatureType.RIVER
        );

        add(
                "white_knife",
                "White Knife",
                GeographicFeatureType.RIVER
        );

        add(
                "white_harbor",
                "White Harbor",
                GeographicFeatureType.CITY
        );

        add(
                "moat_cailin",
                "Moat Cailin",
                GeographicFeatureType.CASTLE
        );

        add(
                "the_bite",
                "The Bite",
                GeographicFeatureType.BAY
        );

        add(
                "littlesister",
                "Littlesister",
                GeographicFeatureType.ISLAND
        );

        add(
                "longsister",
                "Longsister",
                GeographicFeatureType.ISLAND
        );

        add(
                "sweetsister",
                "Sweetsister",
                GeographicFeatureType.ISLAND
        );

        add(
                "pebble",
                "Pebble",
                GeographicFeatureType.ISLAND
        );

        add(
                "the_paps",
                "The Paps",
                GeographicFeatureType.ISLAND_GROUP
        );

        add(
                "three_sisters",
                "Three Sisters",
                GeographicFeatureType.ISLAND_GROUP
        );

        add(
                "the_fingers",
                "The Fingers",
                GeographicFeatureType.PENINSULA
        );

        add(
                "the_twins",
                "The Twins",
                GeographicFeatureType.CASTLE
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