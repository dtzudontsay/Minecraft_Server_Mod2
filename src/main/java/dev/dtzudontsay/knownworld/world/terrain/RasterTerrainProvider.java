package dev.dtzudontsay.knownworld.world.terrain;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import dev.dtzudontsay.knownworld.world.terrain.elevation.ElevationProvider;
import dev.dtzudontsay.knownworld.world.terrain.elevation.ElevationSample;
import dev.dtzudontsay.knownworld.world.terrain.elevation.RasterElevationProvider;
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

    private final ElevationProvider elevationProvider;

    private final ReliefIntensityProvider reliefProvider;

    private final MountainTerrainGenerator mountainGenerator;

    public RasterTerrainProvider() {

        elevationProvider =
                new RasterElevationProvider();

        reliefProvider =
                new ReliefIntensityProvider();

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

        if (
                geography.land()
        ) {

            double baseElevation;

            String source;

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

            double reliefIntensity =
                    reliefProvider.sample(
                            coordinate
                    );

            double proceduralOffset =
                    mountainGenerator.sampleLandOffset(
                            coordinate,
                            reliefIntensity
                    );

            double elevation =
                    Math.max(
                            SEA_LEVEL_METRES + 1.0,
                            baseElevation
                                    + proceduralOffset
                    );

            return new TerrainSample(
                    elevation,
                    "LAND",
                    source
                            + "+BLOCK_SCALE_TERRAIN"
            );
        }

        /*
         * Ocean does not receive procedural mountain generation.
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
                + (
                end - start
        )
                * factor;
    }
}