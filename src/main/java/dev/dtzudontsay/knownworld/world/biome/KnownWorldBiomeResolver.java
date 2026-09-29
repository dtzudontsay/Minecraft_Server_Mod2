package dev.dtzudontsay.knownworld.world.biome;

import dev.dtzudontsay.knownworld.world.data.GeographicDataManager;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.terrain.TerrainSample;


public final class KnownWorldBiomeResolver {

    private static final double BIOME_VARIATION_SCALE =
            12_000.0;


    private final KnownWorldBiomeRasterData biomeData;


    public KnownWorldBiomeResolver() {

        biomeData =
                KnownWorldBiomeRasterData.getInstance();
    }


    public String resolveBiomeId(
            int minecraftX,
            int minecraftZ
    ) {

        WorldCoordinate coordinate =
                WorldProjection.fromMinecraft(
                        minecraftX + 0.5,
                        minecraftZ + 0.5
                );


        KnownWorldBiomeRasterData.Sample raster =
                biomeData.sample(
                        coordinate
                );


        /*
         * Ocean remains separate from the land climate system.
         *
         * We can later add canonical ocean-temperature regions.
         */
        if (
                !raster.land()
        ) {

            return "minecraft:ocean";
        }


        /*
         * Exact painted biome overrides ALWAYS win.
         *
         * This is intentional.
         *
         * If the user paints a biome over a mountain, the mountain
         * remains a mountain geometrically but uses this biome.
         */
        if (
                raster.overrideBiomeId()
                        != null
        ) {

            return raster.overrideBiomeId();
        }


        TerrainSample terrain =
                GeographicDataManager
                        .getInstance()
                        .sample(
                                coordinate
                        );


        double elevation =
                terrain.elevationMetres();


        return resolveAutomaticBiome(
                raster.climateCode(),
                elevation,
                coordinate
        );
    }


    private String resolveAutomaticBiome(
            int climateCode,
            double elevation,
            WorldCoordinate coordinate
    ) {

        /*
         * ========================================================
         * HIGH MOUNTAINS
         * ========================================================
         */

        if (
                elevation >= 1_350.0
        ) {

            if (
                    climateCode <= 3
            ) {

                return "minecraft:frozen_peaks";
            }


            if (
                    climateCode <= 5
            ) {

                return "minecraft:jagged_peaks";
            }


            return "minecraft:stony_peaks";
        }


        if (
                elevation >= 1_000.0
        ) {

            if (
                    climateCode <= 4
            ) {

                return "minecraft:snowy_slopes";
            }


            if (
                    climateCode == 5
            ) {

                return choose(
                        coordinate,
                        "minecraft:meadow",
                        "minecraft:jagged_peaks"
                );
            }


            return "minecraft:stony_peaks";
        }


        if (
                elevation >= 700.0
        ) {

            return switch (
                    climateCode
                    ) {

                case 1, 2 ->
                        "minecraft:snowy_slopes";

                case 3 ->
                        "minecraft:grove";

                case 4, 5 ->
                        "minecraft:meadow";

                case 6, 7, 8, 9, 10, 11, 12 ->
                        "minecraft:stony_peaks";

                case 13 ->
                        "minecraft:meadow";

                default ->
                        "minecraft:meadow";
            };
        }


        /*
         * ========================================================
         * LOWLAND CLIMATE
         * ========================================================
         */

        return switch (
                climateCode
                ) {

            /*
             * POLAR
             */
            case 1 ->
                    choose(
                            coordinate,
                            "minecraft:snowy_plains",
                            "minecraft:ice_spikes",
                            "minecraft:snowy_plains"
                    );


            /*
             * TUNDRA
             */
            case 2 ->
                    choose(
                            coordinate,
                            "minecraft:snowy_plains",
                            "minecraft:snowy_plains",
                            "minecraft:snowy_taiga"
                    );


            /*
             * BOREAL
             */
            case 3 ->
                    choose(
                            coordinate,
                            "minecraft:taiga",
                            "minecraft:old_growth_pine_taiga",
                            "minecraft:old_growth_spruce_taiga"
                    );


            /*
             * COOL TEMPERATE
             */
            case 4 ->
                    choose(
                            coordinate,
                            "minecraft:forest",
                            "minecraft:birch_forest",
                            "minecraft:taiga",
                            "minecraft:plains"
                    );


            /*
             * TEMPERATE
             */
            case 5 ->
                    choose(
                            coordinate,
                            "minecraft:plains",
                            "minecraft:forest",
                            "minecraft:birch_forest",
                            "minecraft:plains"
                    );


            /*
             * WARM TEMPERATE
             */
            case 6 ->
                    choose(
                            coordinate,
                            "minecraft:plains",
                            "minecraft:forest",
                            "minecraft:savanna"
                    );


            /*
             * MEDITERRANEAN
             */
            case 7 ->
                    choose(
                            coordinate,
                            "minecraft:plains",
                            "minecraft:savanna",
                            "minecraft:savanna_plateau"
                    );


            /*
             * STEPPE
             */
            case 8 ->
                    choose(
                            coordinate,
                            "minecraft:plains",
                            "minecraft:plains",
                            "minecraft:savanna"
                    );


            /*
             * SEMI-ARID
             */
            case 9 ->
                    choose(
                            coordinate,
                            "minecraft:savanna",
                            "minecraft:savanna_plateau",
                            "minecraft:windswept_savanna"
                    );


            /*
             * DESERT
             */
            case 10 ->
                    choose(
                            coordinate,
                            "minecraft:desert",
                            "minecraft:desert",
                            "minecraft:badlands"
                    );


            /*
             * SUBTROPICAL
             */
            case 11 ->
                    choose(
                            coordinate,
                            "minecraft:sparse_jungle",
                            "minecraft:savanna",
                            "minecraft:jungle"
                    );


            /*
             * TROPICAL
             */
            case 12 ->
                    choose(
                            coordinate,
                            "minecraft:jungle",
                            "minecraft:sparse_jungle",
                            "minecraft:bamboo_jungle"
                    );


            /*
             * WETLAND
             */
            case 13 ->
                    choose(
                            coordinate,
                            "minecraft:swamp",
                            "minecraft:swamp",
                            "minecraft:mangrove_swamp"
                    );


            /*
             * Climate code 0 should only occur if an authored land
             * pixel was accidentally missed.
             *
             * Keep it playable rather than crashing.
             */
            default ->
                    "minecraft:plains";
        };
    }


    private static String choose(
            WorldCoordinate coordinate,
            String... choices
    ) {

        if (
                choices.length == 0
        ) {

            return "minecraft:plains";
        }


        long cellX =
                (long) Math.floor(
                        coordinate.eastMetres()
                                / BIOME_VARIATION_SCALE
                );


        long cellZ =
                (long) Math.floor(
                        coordinate.northMetres()
                                / BIOME_VARIATION_SCALE
                );


        long hash =
                0x9E3779B97F4A7C15L;


        hash ^=
                cellX
                        * 0xBF58476D1CE4E5B9L;


        hash ^=
                cellZ
                        * 0x94D049BB133111EBL;


        hash ^=
                hash >>> 30;


        hash *=
                0xBF58476D1CE4E5B9L;


        hash ^=
                hash >>> 27;


        hash *=
                0x94D049BB133111EBL;


        hash ^=
                hash >>> 31;


        int index =
                Math.floorMod(
                        hash,
                        choices.length
                );


        return choices[
                index
                ];
    }
}