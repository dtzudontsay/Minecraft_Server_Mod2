package dev.dtzudontsay.knownworld.world.geography.regions;

import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;

public final class NorthWestEssosFeatureRegistry {

    private static final String SOURCE_NOTE =
            "Official The Known World regional map: North-West Essos";

    private static boolean bootstrapped = false;

    private NorthWestEssosFeatureRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        // ------------------------------------------------------------
        // MAJOR SEAS
        // ------------------------------------------------------------

        add(
                "the_shivering_sea",
                "The Shivering Sea",
                GeographicFeatureType.SEA
        );

        add(
                "the_narrow_sea",
                "The Narrow Sea",
                GeographicFeatureType.SEA
        );

        // ------------------------------------------------------------
        // BRAAVOS / LORATH
        // ------------------------------------------------------------

        add(
                "braavos",
                "Braavos",
                GeographicFeatureType.CITY
        );

        add(
                "braavosi_coastlands",
                "Braavosi Coastlands",
                GeographicFeatureType.REGION
        );

        add(
                "lorath",
                "Lorath",
                GeographicFeatureType.CITY
        );

        add(
                "lorathi_bay",
                "Lorathi Bay",
                GeographicFeatureType.BAY
        );

        add(
                "the_axe",
                "The Axe",
                GeographicFeatureType.PENINSULA
        );

        // ------------------------------------------------------------
        // ANDALOS / PENTOS / NORVOS
        // ------------------------------------------------------------

        add(
                "andalos",
                "Andalos",
                GeographicFeatureType.REGION
        );

        add(
                "hills_of_norvos",
                "Hills of Norvos",
                GeographicFeatureType.HILLS
        );

        add(
                "upper_rhoyne",
                "Upper Rhoyne",
                GeographicFeatureType.RIVER
        );

        add(
                "little_rhoyne",
                "Little Rhoyne",
                GeographicFeatureType.RIVER
        );

        add(
                "velvet_hills",
                "Velvet Hills",
                GeographicFeatureType.HILLS
        );

        add(
                "pentos",
                "Pentos",
                GeographicFeatureType.CITY
        );

        add(
                "bay_of_pentos",
                "Bay of Pentos",
                GeographicFeatureType.BAY
        );

        add(
                "the_flatlands",
                "The Flatlands",
                GeographicFeatureType.REGION
        );

        add(
                "ghoyan_drohe",
                "Ghoyan Drohe",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "norvos",
                "Norvos",
                GeographicFeatureType.CITY
        );

        add(
                "noyne",
                "Noyne",
                GeographicFeatureType.RIVER
        );

        add(
                "ny_sar",
                "Ny Sar",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "ar_noy",
                "Ar Noy",
                GeographicFeatureType.RUINED_CITY
        );

        // ------------------------------------------------------------
        // QOHOR REGION
        // ------------------------------------------------------------

        add(
                "darkwater",
                "Darkwater",
                GeographicFeatureType.RIVER
        );

        add(
                "forest_of_qohor",
                "Forest of Qohor",
                GeographicFeatureType.FOREST
        );

        add(
                "qohor",
                "Qohor",
                GeographicFeatureType.CITY
        );

        // ------------------------------------------------------------
        // SARNOR
        // ------------------------------------------------------------

        add(
                "saath",
                "Saath",
                GeographicFeatureType.CITY
        );

        add(
                "morosh",
                "Morosh",
                GeographicFeatureType.CITY
        );

        add(
                "vaes_graddakh",
                "Vaes Graddakh (Sarys)",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "kingdom_of_sarnor",
                "Kingdom of Sarnor",
                GeographicFeatureType.REGION
        );

        add(
                "vaes_khadokh",
                "Vaes Khadokh (Essaria)",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "vaes_khewo",
                "Vaes Khewo (Sarnath)",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "omber",
                "Omber",
                GeographicFeatureType.REGION
        );

        add(
                "bay_of_tusks",
                "Bay of Tusks",
                GeographicFeatureType.BAY
        );

        add(
                "vaes_athjikhari",
                "Vaes Athjikhari (Sallosh)",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "vaes_leqse",
                "Vaes Leqse (Gornath)",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "vojjor_samui",
                "Vojjor Samui (Kasath)",
                GeographicFeatureType.RUINED_CITY
        );

        add(
                "yalli_qamayi",
                "Yalli Qamayi (Sathar)",
                GeographicFeatureType.RUINED_CITY
        );

        /*
         * ------------------------------------------------------------
         * OVERLAPS / EXCLUSIONS
         * ------------------------------------------------------------
         *
         * Already registered elsewhere:
         * - Tarth
         * - Skagos
         * - Westerosi land visible at the far left
         * - labels south of Ny Sar / Ar Noy belonging to the
         *   previous South-West Essos crop
         *
         * Deferred:
         * - The Dothraki Sea
         *
         * It is only partially visible here and will be registered
         * from a later regional crop.
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