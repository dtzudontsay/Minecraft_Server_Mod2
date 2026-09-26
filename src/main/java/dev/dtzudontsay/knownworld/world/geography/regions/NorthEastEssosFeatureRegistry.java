package dev.dtzudontsay.knownworld.world.geography.regions;

import dev.dtzudontsay.knownworld.world.geography.GeographicFeature;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureRegistry;
import dev.dtzudontsay.knownworld.world.geography.GeographicFeatureType;
import dev.dtzudontsay.knownworld.world.geography.SourceConfidence;

public final class NorthEastEssosFeatureRegistry {

    private static final String SOURCE_NOTE =
            "Official The Known World regional map: North-East Essos";

    private static boolean bootstrapped = false;

    private NorthEastEssosFeatureRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        // ------------------------------------------------------------
        // SHIVERING SEA / THOUSAND ISLANDS
        // ------------------------------------------------------------

        add(
                "leviathan_sound",
                "Leviathan Sound",
                GeographicFeatureType.SOUND
        );

        add(
                "the_thousand_islands",
                "The Thousand Islands",
                GeographicFeatureType.ISLAND_GROUP
        );

        // ------------------------------------------------------------
        // JOGOS NHAI / N'GHAI / MOSSOVY
        // ------------------------------------------------------------

        add(
                "plains_of_the_jogos_nhai",
                "Plains of the Jogos Nhai",
                GeographicFeatureType.REGION
        );

        add(
                "nefer",
                "Nefer",
                GeographicFeatureType.CITY
        );

        add(
                "nghai",
                "N'Ghai",
                GeographicFeatureType.REGION
        );

        add(
                "mossovy",
                "Mossovy",
                GeographicFeatureType.REGION
        );

        // ------------------------------------------------------------
        // FURTHER EAST
        // ------------------------------------------------------------

        add(
                "cannibal_sands",
                "Cannibal Sands",
                GeographicFeatureType.DESERT
        );

        add(
                "the_grey_waste",
                "The Grey Waste",
                GeographicFeatureType.DESERT
        );

        add(
                "bleeding_sea",
                "Bleeding Sea",
                GeographicFeatureType.SEA
        );

        add(
                "kdath",
                "K'Dath",
                GeographicFeatureType.CITY
        );

        add(
                "land_of_the_shrykes",
                "Land of the Shrykes",
                GeographicFeatureType.REGION
        );

        add(
                "bonetown",
                "Bonetown",
                GeographicFeatureType.TOWN
        );

        add(
                "the_dry_deep",
                "The Dry Deep",
                GeographicFeatureType.CANYON
        );

        add(
                "the_five_forts",
                "The Five Forts",
                GeographicFeatureType.FORTRESS_GROUP
        );

        add(
                "cities_of_the_bloodless_men",
                "Cities of the Bloodless Men",
                GeographicFeatureType.CITY_GROUP
        );

        // ------------------------------------------------------------
        // EASTERN YI TI APPROACH
        // ------------------------------------------------------------

        add(
                "trader_town",
                "Trader Town",
                GeographicFeatureType.CITY
        );

        add(
                "the_shrinking_sea",
                "The Shrinking Sea",
                GeographicFeatureType.SEA
        );

        add(
                "tiqui",
                "Tiqui",
                GeographicFeatureType.CITY
        );

        /*
         * ------------------------------------------------------------
         * OVERLAPS — DO NOT REGISTER AGAIN
         * ------------------------------------------------------------
         *
         * NorthWestEssosFeatureRegistry:
         * - The Shivering Sea
         *
         * NorthEssosFeatureRegistry:
         * - Steel Road
         *
         * No other visible labels on this crop are currently
         * duplicated in the global registry.
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