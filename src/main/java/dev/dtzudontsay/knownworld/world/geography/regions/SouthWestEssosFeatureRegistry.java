package dev.dtzudontsay.knownworld.world.geography.regions;

import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;

public final class SouthWestEssosFeatureRegistry {

    private static final String SOURCE_NOTE =
            "Official The Known World regional map: South-West Essos";

    private static boolean bootstrapped = false;

    private SouthWestEssosFeatureRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        // ------------------------------------------------------------
        // STEPSTONES / WESTERN APPROACH
        // ------------------------------------------------------------

        add(
                "sea_of_myrth",
                "Sea of Myrth",
                GeographicFeatureType.SEA
        );

        add(
                "tyrosh",
                "Tyrosh",
                GeographicFeatureType.CITY
        );

        add(
                "bloodstone",
                "Bloodstone",
                GeographicFeatureType.ISLAND
        );

        add(
                "stepstones",
                "Stepstones",
                GeographicFeatureType.ISLAND_GROUP
        );

        add(
                "grey_gallows",
                "Grey Gallows",
                GeographicFeatureType.ISLAND
        );

        add(
                "the_disputed_lands",
                "The Disputed Lands",
                GeographicFeatureType.REGION
        );

        add(
                "myr",
                "Myr",
                GeographicFeatureType.CITY
        );

        add(
                "lys",
                "Lys",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // RHoyNE / VOLANTIS REGION
        // ------------------------------------------------------------

        add(
                "lhorulu",
                "Lhorulu",
                GeographicFeatureType.RIVER
        );

        add(
                "the_golden_fields",
                "The Golden Fields",
                GeographicFeatureType.FIELD
        );

        add(
                "dagger_lake",
                "Dagger Lake",
                GeographicFeatureType.LAKE
        );

        add(
                "the_sorrows",
                "The Sorrows",
                GeographicFeatureType.REGION
        );

        add(
                "rhoyne",
                "Rhoyne",
                GeographicFeatureType.RIVER
        );

        add(
                "selhoru",
                "Selhoru",
                GeographicFeatureType.RIVER
        );

        add(
                "selhorys",
                "Selhorys",
                GeographicFeatureType.TOWN
        );

        add(
                "valysar",
                "Valysar",
                GeographicFeatureType.TOWN
        );

        add(
                "volon_therys",
                "Volon Therys",
                GeographicFeatureType.TOWN
        );

        add(
                "sar_mell",
                "Sar Mell",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "volaena",
                "Volaena",
                GeographicFeatureType.RIVER
        );

        add(
                "volantis",
                "Volantis",
                GeographicFeatureType.CITY
        );

        add(
                "the_orange_shore",
                "The Orange Shore",
                GeographicFeatureType.SHORE
        );

        // ------------------------------------------------------------
        // PAINTED MOUNTAINS / EASTERN APPROACH
        // ------------------------------------------------------------

        add(
                "painted_mountains",
                "Painted Mountains",
                GeographicFeatureType.MOUNTAIN_RANGE
        );

        add(
                "the_demon_road",
                "The Demon Road",
                GeographicFeatureType.ROAD
        );

        add(
                "the_black_cliffs",
                "The Black Cliffs",
                GeographicFeatureType.CLIFFS
        );

        add(
                "bhorash",
                "Bhorash",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "mantarys",
                "Mantarys",
                GeographicFeatureType.CITY
        );

        add(
                "elyria",
                "Elyria",
                GeographicFeatureType.CITY
        );

        add(
                "tolos",
                "Tolos",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // VALYRIA
        // ------------------------------------------------------------

        add(
                "lands_of_the_long_summer",
                "Lands of the Long Summer",
                GeographicFeatureType.REGION
        );

        add(
                "sea_of_sighs",
                "The Sea of Sighs",
                GeographicFeatureType.SEA
        );

        add(
                "the_smoking_sea",
                "The Smoking Sea",
                GeographicFeatureType.SEA
        );

        add(
                "oros",
                "Oros",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "tyria",
                "Tyria",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "valyria",
                "Valyria",
                GeographicFeatureType.RUINED_CITY
        );

        // ------------------------------------------------------------
        // GULF OF GRIEF
        // ------------------------------------------------------------

        add(
                "isle_of_cedars",
                "Isle of Cedars",
                GeographicFeatureType.ISLAND
        );

        add(
                "ghozai",
                "Ghozai",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "velos",
                "Velos",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "gulf_of_grief",
                "Gulf of Grief",
                GeographicFeatureType.BAY
        );

        add(
                "slavers_bay",
                "Slaver's Bay",
                GeographicFeatureType.BAY
        );

        /*
         * ------------------------------------------------------------
         * OVERLAPS — DO NOT REGISTER AGAIN
         * ------------------------------------------------------------
         *
         * SouthFeatureRegistry:
         * - Tarth
         * - Estermont
         * - Sunspear
         * - Shipbreaker Bay (partially visible)
         * - The Broken Arm (partially visible)
         *
         * SummerIslesFeatureRegistry:
         * - Stone Head
         * - Walano
         * - Isle of Women
         * - Lotus Port
         *
         * ------------------------------------------------------------
         * DEFERRED PARTIAL LABELS
         * ------------------------------------------------------------
         *
         * - The Dothraki Sea
         * - The Summer Sea
         *
         * These will be registered from later crops where the
         * surrounding geography is more fully visible.
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