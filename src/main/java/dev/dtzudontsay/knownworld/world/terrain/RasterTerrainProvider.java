package dev.dtzudontsay.knownworld.world.terrain;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldDefinition;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import dev.dtzudontsay.knownworld.world.terrain.elevation.ElevationProvider;
import dev.dtzudontsay.knownworld.world.terrain.elevation.ElevationSample;
import dev.dtzudontsay.knownworld.world.terrain.elevation.RasterElevationProvider;
import dev.dtzudontsay.knownworld.world.terrain.lowland.LowlandTerrainGenerator;
import dev.dtzudontsay.knownworld.world.terrain.mountain.MountainRegionProfileProvider;
import dev.dtzudontsay.knownworld.world.terrain.mountain.MountainTerrainGenerator;
import dev.dtzudontsay.knownworld.world.terrain.relief.ReliefIntensityProvider;


public final class RasterTerrainProvider implements TerrainProvider {

    public static final double SEA_LEVEL_METRES =
            63.0;


    public static final double FALLBACK_COASTAL_LAND_HEIGHT =
            67.0;


    public static final double FALLBACK_MAX_LAND_HEIGHT =
            92.0;


    public static final double FALLBACK_SHALLOW_OCEAN_FLOOR =
            52.0;


    public static final double FALLBACK_DEEP_OCEAN_FLOOR =
            34.0;


    private static final double FALLBACK_LAND_TRANSITION_DISTANCE =
            120_000.0;


    private static final double FALLBACK_OCEAN_TRANSITION_DISTANCE =
            150_000.0;


    /*
     * Low configured hill regions still need some room above the
     * macro elevation base.
     */

    private static final double MINIMUM_PROFILE_RELIEF_ROOM =
            14.0;


    private final ElevationProvider elevationProvider;

    private final ReliefIntensityProvider reliefProvider;

    private final LowlandTerrainGenerator lowlandGenerator;

    private final MountainRegionProfileProvider mountainProfileProvider;

    private final MountainTerrainGenerator mountainGenerator;


    public RasterTerrainProvider() {

        elevationProvider =
                new RasterElevationProvider();


        reliefProvider =
                new ReliefIntensityProvider();


        lowlandGenerator =
                new LowlandTerrainGenerator();


        mountainProfileProvider =
                new MountainRegionProfileProvider();


        mountainGenerator =
                new MountainTerrainGenerator();
    }


    @Override
    public TerrainSample sample(
            WorldCoordinate coordinate
    ) {

        KnownWorldGeoSample geography =
                KnownWorldGeoSampler.sampleWorld(
                        coordinate
                );


        /*
         * ========================================================
         * OUTSIDE KNOWN WORLD
         * ========================================================
         */

        if (
                !geography.insideKnownWorldMap()
        ) {

            return new TerrainSample(
                    -64.0,
                    "OUTSIDE_KNOWN_WORLD",
                    "CANONICAL_RASTER"
            );
        }


        ElevationSample canonicalElevation =
                elevationProvider.sample(
                        coordinate
                );


        /*
         * ========================================================
         * LAND
         * ========================================================
         */

        if (
                geography.land()
        ) {

            double baseElevation;

            String source;


            /*
             * ----------------------------------------------------
             * MACRO ELEVATION
             * ----------------------------------------------------
             */

            if (
                    !Double.isNaN(
                            canonicalElevation.elevationMetres()
                    )
            ) {

                baseElevation =
                        Math.max(
                                canonicalElevation.elevationMetres(),
                                SEA_LEVEL_METRES + 1.0
                        );


                source =
                        canonicalElevation.source();

            } else {

                baseElevation =
                        sampleFallbackLandBase(
                                geography
                        );


                source =
                        "CANONICAL_RASTER_FALLBACK";
            }


            /*
             * ----------------------------------------------------
             * AUTHORED MOUNTAIN PROFILE
             * ----------------------------------------------------
             */

            MountainRegionProfileProvider.MountainProfileSample profile =
                    mountainProfileProvider.sample(
                            geography.logicalMapCoordinate()
                    );


            /*
             * Very low hill profiles may have ceilings below the
             * normal inland macro-elevation maximum.
             *
             * Leave room for their authored relief.
             */

            if (
                    profile.explicitRegion()
            ) {

                double highestAllowedBase =
                        profile.maxSummitY()
                                - MINIMUM_PROFILE_RELIEF_ROOM;


                baseElevation =
                        Math.min(
                                baseElevation,
                                highestAllowedBase
                        );


                baseElevation =
                        Math.max(
                                SEA_LEVEL_METRES + 1.0,
                                baseElevation
                        );
            }


            /*
             * ----------------------------------------------------
             * AUTHORED RELIEF STRENGTH
             * ----------------------------------------------------
             */

            double reliefIntensity =
                    reliefProvider.sample(
                            coordinate
                    );


            /*
             * ----------------------------------------------------
             * OPTIONAL LOWLAND TERRAIN
             * ----------------------------------------------------
             *
             * This is our new vanilla-LIKE terrain layer.
             *
             * It does not invoke Minecraft's vanilla world generator.
             *
             * It cannot create vanilla mountain ranges.
             */

            double lowlandOffset =
                    0.0;


            if (
                    WorldDefinition.ENABLE_LOWLAND_TERRAIN
            ) {

                lowlandOffset =
                        lowlandGenerator.sampleLandOffset(
                                coordinate,
                                geography.coastDistanceMetres(),
                                reliefIntensity
                        );
            }


            /*
             * ----------------------------------------------------
             * CANONICAL MOUNTAIN TERRAIN
             * ----------------------------------------------------
             *
             * Existing mountain generation remains entirely separate
             * and authoritative.
             *
             * This is the ONLY system here allowed to generate the
             * large mountain ranges we previously authored.
             */

            double mountainOffset =
                    mountainGenerator.sampleLandOffset(
                            coordinate,
                            reliefIntensity,
                            baseElevation,
                            profile
                    );


            /*
             * ----------------------------------------------------
             * FINAL LAND ELEVATION
             * ----------------------------------------------------
             */

            double elevation =
                    baseElevation
                            + lowlandOffset
                            + mountainOffset;


            /*
             * Keep canonical land above water.
             */

            elevation =
                    Math.max(
                            SEA_LEVEL_METRES + 1.0,
                            elevation
                    );


            /*
             * Existing authored mountain ceilings remain absolute.
             *
             * This protects low hill profiles as well as large
             * mountain systems.
             */

            if (
                    profile.explicitRegion()
            ) {

                elevation =
                        Math.min(
                                elevation,
                                profile.maxSummitY()
                        );
            }


            String terrainSource =
                    source;


            if (
                    WorldDefinition.ENABLE_LOWLAND_TERRAIN
            ) {

                terrainSource +=
                        "+LOWLAND_TERRAIN";
            }


            terrainSource +=
                    "+BLOCK_SCALE_TERRAIN"
                            + "+"
                            + profile.regionId();


            return new TerrainSample(
                    elevation,
                    "LAND",
                    terrainSource
            );
        }


        /*
         * ========================================================
         * OCEAN
         * ========================================================
         *
         * The lowland generator is never applied to ocean pixels.
         *
         * Therefore it cannot:
         *
         * - create islands
         * - extend continents
         * - fill oceans
         * - move coastlines
         */

        if (
                !Double.isNaN(
                        canonicalElevation.elevationMetres()
                )
        ) {

            return new TerrainSample(
                    Math.min(
                            canonicalElevation.elevationMetres(),
                            SEA_LEVEL_METRES - 1.0
                    ),
                    "OCEAN",
                    canonicalElevation.source()
            );
        }


        return new TerrainSample(
                sampleFallbackOceanBase(
                        geography
                ),
                "OCEAN",
                "CANONICAL_RASTER_FALLBACK"
        );
    }


    public boolean hasCanonicalElevationData() {

        return elevationProvider
                .hasCanonicalElevationData();
    }


    private double sampleFallbackLandBase(
            KnownWorldGeoSample geography
    ) {

        double inlandDistance =
                Math.max(
                        0.0,
                        geography.coastDistanceMetres()
                );


        double factor =
                clamp01(
                        inlandDistance
                                / FALLBACK_LAND_TRANSITION_DISTANCE
                );


        factor =
                smoothstep(
                        factor
                );


        return lerp(
                FALLBACK_COASTAL_LAND_HEIGHT,
                FALLBACK_MAX_LAND_HEIGHT,
                factor
        );
    }


    private double sampleFallbackOceanBase(
            KnownWorldGeoSample geography
    ) {

        double offshoreDistance =
                Math.max(
                        0.0,
                        -geography.coastDistanceMetres()
                );


        double factor =
                clamp01(
                        offshoreDistance
                                / FALLBACK_OCEAN_TRANSITION_DISTANCE
                );


        factor =
                smoothstep(
                        factor
                );


        return lerp(
                FALLBACK_SHALLOW_OCEAN_FLOOR,
                FALLBACK_DEEP_OCEAN_FLOOR,
                factor
        );
    }


    private static double clamp01(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }


    private static double smoothstep(
            double value
    ) {

        return value
                * value
                * (
                3.0
                        - 2.0
                        * value
        );
    }


    private static double lerp(
            double start,
            double end,
            double factor
    ) {

        return start
                +
                (
                        end
                                - start
                )
                        * factor;
    }
}