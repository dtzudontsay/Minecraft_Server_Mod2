package dev.dtzudontsay.knownworld.world.terrain;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;
import dev.dtzudontsay.knownworld.world.terrain.elevation.ElevationProvider;
import dev.dtzudontsay.knownworld.world.terrain.elevation.ElevationSample;
import dev.dtzudontsay.knownworld.world.terrain.elevation.RasterElevationProvider;

public final class RasterTerrainProvider implements TerrainProvider {

    public static final double SEA_LEVEL_METRES =
            63.0;

    /*
     * TEMPORARY fallback values.
     *
     * These remain active only wherever we do not yet possess
     * trustworthy canonical elevation data.
     */
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

    public RasterTerrainProvider() {

        elevationProvider =
                new RasterElevationProvider();
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
                !Double.isNaN(
                        canonicalElevation.elevationMetres()
                )
        ) {

            double elevation =
                    canonicalElevation.elevationMetres();

            /*
             * The land/water mask remains authoritative.
             *
             * A future elevation source must never accidentally
             * turn canonical land into ocean or canonical ocean
             * into dry land simply because its vertical source
             * disagrees slightly at a coastline.
             */
            if (
                    geography.land()
            ) {

                elevation =
                        Math.max(
                                elevation,
                                SEA_LEVEL_METRES + 1.0
                        );

                return new TerrainSample(
                        elevation,
                        "LAND",
                        canonicalElevation.source()
                );
            }

            elevation =
                    Math.min(
                            elevation,
                            SEA_LEVEL_METRES - 1.0
                    );

            return new TerrainSample(
                    elevation,
                    "OCEAN",
                    canonicalElevation.source()
            );
        }

        /*
         * No canonical elevation exists here yet.
         * Preserve today's proven behaviour rather than inventing
         * topography.
         */
        if (
                geography.land()
        ) {
            return sampleFallbackLand(
                    geography
            );
        }

        return sampleFallbackOcean(
                geography
        );
    }

    public boolean hasCanonicalElevationData() {

        return elevationProvider
                .hasCanonicalElevationData();
    }

    private TerrainSample sampleFallbackLand(
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

        double elevation =
                lerp(
                        FALLBACK_COASTAL_LAND_HEIGHT,
                        FALLBACK_MAX_LAND_HEIGHT,
                        factor
                );

        return new TerrainSample(
                elevation,
                "LAND",
                "CANONICAL_RASTER_FALLBACK"
        );
    }

    private TerrainSample sampleFallbackOcean(
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

        double elevation =
                lerp(
                        FALLBACK_SHALLOW_OCEAN_FLOOR,
                        FALLBACK_DEEP_OCEAN_FLOOR,
                        factor
                );

        return new TerrainSample(
                elevation,
                "OCEAN",
                "CANONICAL_RASTER_FALLBACK"
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