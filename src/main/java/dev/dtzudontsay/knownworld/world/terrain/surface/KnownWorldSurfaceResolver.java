package dev.dtzudontsay.knownworld.world.terrain.surface;

import dev.dtzudontsay.knownworld.world.data.GeographicDataManager;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.terrain.TerrainSample;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class KnownWorldSurfaceResolver {

    /*
     * Sample several blocks away rather than adjacent columns.
     *
     * Adjacent-height differences are too sensitive to one-block
     * voxel changes.
     */
    private static final int SLOPE_SAMPLE_DISTANCE =
            8;


    /*
     * ------------------------------------------------------------
     * COAST
     * ------------------------------------------------------------
     */

    private static final double BEACH_MAX_COAST_DISTANCE =
            3_000.0;

    private static final int BEACH_MAX_ELEVATION =
            82;


    /*
     * ------------------------------------------------------------
     * ROCK
     * ------------------------------------------------------------
     *
     * slope = vertical rise / horizontal run
     *
     * 0.25 = roughly 1 block up per 4 horizontal blocks
     * 0.50 = roughly 1 per 2
     * 1.00 = roughly 1 per 1
     */

    private static final double ROCK_SLOPE_START =
            0.42;

    private static final double ROCK_SLOPE_FULL =
            0.88;

    /*
     * Even comparatively gentle high-altitude terrain should become
     * rocky eventually.
     */
    private static final int ROCK_ALTITUDE_START =
            620;

    private static final int ROCK_ALTITUDE_FULL =
            1_050;


    /*
     * ------------------------------------------------------------
     * SNOW
     * ------------------------------------------------------------
     */

    private static final int SNOW_ALTITUDE_START =
            900;

    private static final int SNOW_ALTITUDE_FULL =
            1_280;

    /*
     * Steep high slopes can remain exposed rock instead of becoming
     * completely white.
     */
    private static final double SNOW_STEEP_SLOPE_REDUCTION =
            0.62;


    private static final BlockState GRASS =
            Blocks.GRASS_BLOCK.defaultBlockState();

    private static final BlockState DIRT =
            Blocks.DIRT.defaultBlockState();

    private static final BlockState STONE =
            Blocks.STONE.defaultBlockState();

    private static final BlockState SNOW =
            Blocks.SNOW_BLOCK.defaultBlockState();

    private static final BlockState SAND =
            Blocks.SAND.defaultBlockState();


    public SurfaceProfile resolve(
            int worldX,
            int worldZ,
            int surfaceY,
            KnownWorldGeoSample geography
    ) {

        if (
                isBeach(
                        surfaceY,
                        geography
                )
        ) {

            return new SurfaceProfile(
                    SAND,
                    SAND,
                    SurfaceType.BEACH,
                    0.0
            );
        }


        double slope =
                sampleSlope(
                        worldX,
                        worldZ,
                        surfaceY
                );


        /*
         * --------------------------------------------------------
         * ROCK STRENGTH
         * --------------------------------------------------------
         */

        double slopeRock =
                smoothstep(
                        ROCK_SLOPE_START,
                        ROCK_SLOPE_FULL,
                        slope
                );

        double altitudeRock =
                smoothstep(
                        ROCK_ALTITUDE_START,
                        ROCK_ALTITUDE_FULL,
                        surfaceY
                );

        double rockStrength =
                Math.max(
                        slopeRock,
                        altitudeRock * 0.72
                );


        /*
         * Break up perfectly clean contour boundaries.
         */
        double materialNoise =
                surfaceNoise(
                        worldX,
                        worldZ
                );

        rockStrength +=
                materialNoise
                        * 0.16;


        /*
         * --------------------------------------------------------
         * SNOW STRENGTH
         * --------------------------------------------------------
         */

        double snowStrength =
                smoothstep(
                        SNOW_ALTITUDE_START,
                        SNOW_ALTITUDE_FULL,
                        surfaceY
                );

        /*
         * Noise moves the snowline locally up/down.
         *
         * This is intentionally deterministic.
         */
        snowStrength +=
                materialNoise
                        * 0.24;

        /*
         * Very steep faces retain more exposed stone.
         */
        double steepness =
                smoothstep(
                        0.55,
                        1.15,
                        slope
                );

        snowStrength *=
                1.0
                        - steepness
                        * SNOW_STEEP_SLOPE_REDUCTION;


        /*
         * --------------------------------------------------------
         * FINAL MATERIAL
         * --------------------------------------------------------
         */

        if (
                snowStrength >= 0.53
        ) {

            return new SurfaceProfile(
                    SNOW,
                    STONE,
                    SurfaceType.SNOW,
                    slope
            );
        }

        if (
                rockStrength >= 0.48
        ) {

            return new SurfaceProfile(
                    STONE,
                    STONE,
                    SurfaceType.ROCK,
                    slope
            );
        }

        return new SurfaceProfile(
                GRASS,
                DIRT,
                SurfaceType.GRASS,
                slope
        );
    }


    private boolean isBeach(
            int surfaceY,
            KnownWorldGeoSample geography
    ) {

        return geography.land()
                &&
                geography.coastDistanceMetres()
                        <= BEACH_MAX_COAST_DISTANCE
                &&
                surfaceY
                        <= BEACH_MAX_ELEVATION;
    }


    /*
     * ============================================================
     * SLOPE
     * ============================================================
     */

    private double sampleSlope(
            int worldX,
            int worldZ,
            int centreY
    ) {

        int distance =
                SLOPE_SAMPLE_DISTANCE;

        double east =
                sampleTerrainHeight(
                        worldX + distance,
                        worldZ,
                        centreY
                );

        double west =
                sampleTerrainHeight(
                        worldX - distance,
                        worldZ,
                        centreY
                );

        double south =
                sampleTerrainHeight(
                        worldX,
                        worldZ + distance,
                        centreY
                );

        double north =
                sampleTerrainHeight(
                        worldX,
                        worldZ - distance,
                        centreY
                );

        double xGradient =
                Math.abs(
                        east - west
                )
                        / (
                        distance * 2.0
                );

        double zGradient =
                Math.abs(
                        south - north
                )
                        / (
                        distance * 2.0
                );

        return Math.sqrt(
                xGradient * xGradient
                        +
                        zGradient * zGradient
        );
    }


    private double sampleTerrainHeight(
            int worldX,
            int worldZ,
            int fallbackHeight
    ) {

        WorldCoordinate coordinate =
                WorldProjection.fromMinecraft(
                        worldX + 0.5,
                        worldZ + 0.5
                );

        TerrainSample sample =
                GeographicDataManager
                        .getInstance()
                        .sample(
                                coordinate
                        );

        if (
                !Double.isFinite(
                        sample.elevationMetres()
                )
        ) {

            return fallbackHeight;
        }

        return sample.elevationMetres();
    }


    /*
     * ============================================================
     * MATERIAL NOISE
     * ============================================================
     *
     * Small deterministic variation prevents:
     *
     *      perfect snow rings
     *      perfect rock rings
     *
     * around mountains.
     */

    private static double surfaceNoise(
            int worldX,
            int worldZ
    ) {

        double broad =
                valueNoise(
                        worldX,
                        worldZ,
                        96.0,
                        0x62A9D9ED799705F5L
                );

        double fine =
                valueNoise(
                        worldX,
                        worldZ,
                        34.0,
                        0xCB24D0A5C88C35B3L
                );

        return (
                broad * 0.72
                        +
                        fine * 0.28
        )
                * 2.0
                - 1.0;
    }


    private static double valueNoise(
            double x,
            double z,
            double scale,
            long seed
    ) {

        double scaledX =
                x / scale;

        double scaledZ =
                z / scale;

        int x0 =
                (int) Math.floor(
                        scaledX
                );

        int z0 =
                (int) Math.floor(
                        scaledZ
                );

        int x1 =
                x0 + 1;

        int z1 =
                z0 + 1;

        double tx =
                smoothstep01(
                        scaledX - x0
                );

        double tz =
                smoothstep01(
                        scaledZ - z0
                );

        double a =
                random01(
                        x0,
                        z0,
                        seed
                );

        double b =
                random01(
                        x1,
                        z0,
                        seed
                );

        double c =
                random01(
                        x0,
                        z1,
                        seed
                );

        double d =
                random01(
                        x1,
                        z1,
                        seed
                );

        double north =
                lerp(
                        a,
                        b,
                        tx
                );

        double south =
                lerp(
                        c,
                        d,
                        tx
                );

        return lerp(
                north,
                south,
                tz
        );
    }


    private static double random01(
            int x,
            int z,
            long seed
    ) {

        long value =
                seed;

        value ^=
                (long) x
                        * 0x9E3779B97F4A7C15L;

        value ^=
                (long) z
                        * 0xC2B2AE3D27D4EB4FL;

        value ^=
                value >>> 30;

        value *=
                0xBF58476D1CE4E5B9L;

        value ^=
                value >>> 27;

        value *=
                0x94D049BB133111EBL;

        value ^=
                value >>> 31;

        return (
                value >>> 11
        )
                * 0x1.0p-53;
    }


    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {

        double t =
                (
                        value - edge0
                )
                        /
                        (
                                edge1 - edge0
                        );

        t =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                t
                        )
                );

        return t
                * t
                * (
                3.0
                        - 2.0 * t
        );
    }


    private static double smoothstep01(
            double value
    ) {

        value =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                value
                        )
                );

        return value
                * value
                * (
                3.0
                        - 2.0 * value
        );
    }


    private static double lerp(
            double a,
            double b,
            double t
    ) {

        return a
                + (
                b - a
        )
                * t;
    }


    public enum SurfaceType {

        GRASS,

        ROCK,

        SNOW,

        BEACH
    }


    public record SurfaceProfile(
            BlockState topBlock,
            BlockState subsurfaceBlock,
            SurfaceType type,
            double slope
    ) {
    }
}