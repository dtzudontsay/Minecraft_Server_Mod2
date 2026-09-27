package dev.dtzudontsay.knownworld.world.terrain;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;

public final class RasterTerrainProvider implements TerrainProvider {

    /*
     * Initial terrain prototype values.
     *
     * These are deliberately simple.
     *
     * Later these will be replaced / augmented by:
     * - proper elevation data
     * - mountain ranges
     * - rivers
     * - climate
     * - regional terrain rules
     */
    public static final double SEA_LEVEL_METRES =
            63.0;

    public static final double COASTAL_LAND_HEIGHT =
            67.0;

    public static final double MAX_BASIC_LAND_HEIGHT =
            92.0;

    public static final double SHALLOW_OCEAN_FLOOR =
            52.0;

    public static final double DEEP_OCEAN_FLOOR =
            34.0;

    /*
     * Distance over which simple prototype terrain transitions
     * from coastal values to inland / deep-ocean values.
     */
    private static final double LAND_TRANSITION_DISTANCE =
            120_000.0;

    private static final double OCEAN_TRANSITION_DISTANCE =
            150_000.0;

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

        if (
                geography.land()
        ) {
            return sampleLand(
                    geography
            );
        }

        return sampleOcean(
                geography
        );
    }

    private TerrainSample sampleLand(
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
                                / LAND_TRANSITION_DISTANCE
                );

        /*
         * Smoothstep avoids an abrupt slope change.
         */
        factor =
                smoothstep(
                        factor
                );

        double elevation =
                lerp(
                        COASTAL_LAND_HEIGHT,
                        MAX_BASIC_LAND_HEIGHT,
                        factor
                );

        return new TerrainSample(
                elevation,
                "LAND",
                "CANONICAL_RASTER"
        );
    }

    private TerrainSample sampleOcean(
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
                                / OCEAN_TRANSITION_DISTANCE
                );

        factor =
                smoothstep(
                        factor
                );

        double elevation =
                lerp(
                        SHALLOW_OCEAN_FLOOR,
                        DEEP_OCEAN_FLOOR,
                        factor
                );

        return new TerrainSample(
                elevation,
                "OCEAN",
                "CANONICAL_RASTER"
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
                end
                        - start
        )
                * factor;
    }
}