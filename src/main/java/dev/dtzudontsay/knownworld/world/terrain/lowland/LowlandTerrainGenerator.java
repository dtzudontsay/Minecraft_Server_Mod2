package dev.dtzudontsay.knownworld.world.terrain.lowland;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;


public final class LowlandTerrainGenerator {

    /*
     * ============================================================
     * PURPOSE
     * ============================================================
     *
     * This generator creates ONLY ordinary lowland terrain.
     *
     * It is not a replacement for the canonical mountain system.
     *
     * It creates:
     *
     *     - rolling plains
     *     - shallow broad valleys
     *     - low hills
     *     - low ridges
     *     - local terrain irregularity
     *
     * It must never create mountain-scale relief.
     */


    /*
     * ============================================================
     * COAST PROTECTION
     * ============================================================
     *
     * Terrain variation fades close to the canonical shoreline.
     *
     * This helps preserve the coastline geometry we already
     * calibrated and authored.
     */

    private static final double COAST_FADE_START_METRES =
            2_000.0;

    private static final double COAST_FADE_END_METRES =
            18_000.0;


    /*
     * ============================================================
     * MOUNTAIN PROTECTION
     * ============================================================
     *
     * Once the authored relief layer starts becoming significant,
     * this lowland terrain fades away.
     *
     * This ensures our existing mountain generator remains
     * authoritative.
     */

    private static final double MOUNTAIN_FADE_START =
            0.10;

    private static final double MOUNTAIN_FADE_END =
            0.52;


    /*
     * ============================================================
     * DOMAIN WARP
     * ============================================================
     */

    private static final double WARP_SCALE =
            14_000.0;

    private static final double WARP_AMPLITUDE =
            2_100.0;


    /*
     * ============================================================
     * LOWLAND TERRAIN SCALES
     * ============================================================
     */

    private static final double MACRO_SCALE =
            22_000.0;

    private static final double BROAD_SCALE =
            9_000.0;

    private static final double ROLLING_SCALE =
            3_200.0;

    private static final double LOCAL_SCALE =
            950.0;

    private static final double DETAIL_SCALE =
            310.0;


    /*
     * ============================================================
     * LOWLAND HEIGHT CONTRIBUTIONS
     * ============================================================
     *
     * These are deliberately small compared with real mountains.
     *
     * Even when several components align, this system should produce
     * hills and rolling countryside rather than a replacement
     * mountain range.
     */

    private static final double MACRO_AMPLITUDE =
            10.0;

    private static final double BROAD_AMPLITUDE =
            14.0;

    private static final double ROLLING_AMPLITUDE =
            9.0;

    private static final double LOCAL_AMPLITUDE =
            4.5;

    private static final double DETAIL_AMPLITUDE =
            2.0;


    /*
     * ============================================================
     * LOW RIDGES
     * ============================================================
     *
     * These provide occasional terrain structure.
     *
     * Maximum contribution is deliberately limited.
     */

    private static final double LOW_RIDGE_SCALE =
            4_800.0;

    private static final double LOW_RIDGE_MAX_AMPLITUDE =
            16.0;

    private static final double RIDGE_SELECTOR_SCALE =
            15_000.0;


    /*
     * ============================================================
     * NATURAL FLATNESS VARIATION
     * ============================================================
     *
     * Some large regions become calmer plains while others become
     * more rolling.
     *
     * This is continuous and does not follow political borders.
     */

    private static final double FLATNESS_SCALE =
            28_000.0;


    /*
     * ============================================================
     * FIXED SEEDS
     * ============================================================
     *
     * Fixed seeds are intentional.
     *
     * The same canonical coordinate must always produce the same
     * terrain so that later hydrology can analyze exactly the
     * landscape Minecraft will generate.
     */

    private static final long WARP_X_SEED =
            0x291D4E5A7B13C6F1L;

    private static final long WARP_Z_SEED =
            0x76C3B5A9142E8D0FL;

    private static final long MACRO_SEED =
            0xA14F92D36BC80571L;

    private static final long BROAD_SEED =
            0x75DA83C12E496BF0L;

    private static final long ROLLING_SEED =
            0xC21D73A549B80E6FL;

    private static final long LOCAL_SEED =
            0x34AB8E91D5C276F0L;

    private static final long DETAIL_SEED =
            0x918F26C4DA750BE3L;

    private static final long RIDGE_SEED =
            0xB79D35A84E12C60FL;

    private static final long RIDGE_SELECTOR_SEED =
            0x54C9E2B71A806D3FL;

    private static final long FLATNESS_SEED =
            0xE1379B42C80D65AFL;


    public double sampleLandOffset(
            WorldCoordinate coordinate,
            double coastDistanceMetres,
            double reliefIntensity
    ) {

        double x =
                coordinate.eastMetres();

        double z =
                coordinate.northMetres();


        /*
         * --------------------------------------------------------
         * COAST WEIGHT
         * --------------------------------------------------------
         */

        double inlandDistance =
                Math.max(
                        0.0,
                        coastDistanceMetres
                );


        double coastWeight =
                smoothstep(
                        COAST_FADE_START_METRES,
                        COAST_FADE_END_METRES,
                        inlandDistance
                );


        /*
         * --------------------------------------------------------
         * MOUNTAIN WEIGHT
         * --------------------------------------------------------
         *
         * relief = 0
         *     full lowland terrain
         *
         * strong authored mountain relief
         *     lowland terrain approaches zero
         */

        double mountainWeight =
                1.0
                        -
                        smoothstep(
                                MOUNTAIN_FADE_START,
                                MOUNTAIN_FADE_END,
                                reliefIntensity
                        );


        double finalWeight =
                coastWeight
                        * mountainWeight;


        if (
                finalWeight <= 0.0001
        ) {

            return 0.0;
        }


        /*
         * --------------------------------------------------------
         * DOMAIN WARP
         * --------------------------------------------------------
         */

        double warpX =
                signedFractalNoise(
                        x,
                        z,
                        WARP_SCALE,
                        WARP_X_SEED,
                        3
                )
                        * WARP_AMPLITUDE;


        double warpZ =
                signedFractalNoise(
                        x,
                        z,
                        WARP_SCALE,
                        WARP_Z_SEED,
                        3
                )
                        * WARP_AMPLITUDE;


        double warpedX =
                x
                        + warpX;

        double warpedZ =
                z
                        + warpZ;


        /*
         * --------------------------------------------------------
         * BROAD NATURAL FLATNESS
         * --------------------------------------------------------
         */

        double flatnessNoise =
                fractalNoise01(
                        warpedX,
                        warpedZ,
                        FLATNESS_SCALE,
                        FLATNESS_SEED,
                        3
                );


        double terrainStrength =
                lerp(
                        0.42,
                        1.0,
                        smoothstep(
                                0.20,
                                0.82,
                                flatnessNoise
                        )
                );


        /*
         * --------------------------------------------------------
         * LARGE TERRAIN FORMS
         * --------------------------------------------------------
         */

        double macro =
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        MACRO_SCALE,
                        MACRO_SEED,
                        4
                )
                        * MACRO_AMPLITUDE;


        double broad =
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        BROAD_SCALE,
                        BROAD_SEED,
                        4
                )
                        * BROAD_AMPLITUDE;


        /*
         * --------------------------------------------------------
         * ROLLING TERRAIN
         * --------------------------------------------------------
         */

        double rolling =
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        ROLLING_SCALE,
                        ROLLING_SEED,
                        3
                )
                        * ROLLING_AMPLITUDE;


        /*
         * --------------------------------------------------------
         * SELECTIVE LOW RIDGES
         * --------------------------------------------------------
         */

        double ridgeSelector =
                fractalNoise01(
                        warpedX,
                        warpedZ,
                        RIDGE_SELECTOR_SCALE,
                        RIDGE_SELECTOR_SEED,
                        3
                );


        ridgeSelector =
                smoothstep(
                        0.55,
                        0.80,
                        ridgeSelector
                );


        double lowRidge =
                ridgedFractalNoise(
                        warpedX,
                        warpedZ,
                        LOW_RIDGE_SCALE,
                        RIDGE_SEED,
                        3
                );


        lowRidge =
                smoothstep(
                        0.40,
                        0.90,
                        lowRidge
                );


        double ridgeContribution =
                lowRidge
                        * ridgeSelector
                        * LOW_RIDGE_MAX_AMPLITUDE;


        /*
         * --------------------------------------------------------
         * LOCAL DETAIL
         * --------------------------------------------------------
         */

        double local =
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        LOCAL_SCALE,
                        LOCAL_SEED,
                        3
                )
                        * LOCAL_AMPLITUDE;


        double detail =
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        DETAIL_SCALE,
                        DETAIL_SEED,
                        2
                )
                        * DETAIL_AMPLITUDE;


        double localStrength =
                0.55
                        +
                        terrainStrength
                                * 0.45;


        /*
         * --------------------------------------------------------
         * FINAL LOWLAND OFFSET
         * --------------------------------------------------------
         */

        double offset =
                (
                        macro
                                + broad
                                + rolling
                                + ridgeContribution
                                + local
                                * localStrength
                                + detail
                                * localStrength
                )
                        * terrainStrength;


        return offset
                * finalWeight;
    }


    private static double ridgedFractalNoise(
            double x,
            double z,
            double baseScale,
            long seed,
            int octaves
    ) {

        double sum =
                0.0;

        double weight =
                1.0;

        double totalWeight =
                0.0;

        double scale =
                baseScale;


        for (
                int octave = 0;
                octave < octaves;
                octave++
        ) {

            double noise =
                    gradientNoise(
                            x,
                            z,
                            scale,
                            seed
                                    +
                                    octave
                                            * 0x9E3779B97F4A7C15L
                    );


            double ridge =
                    1.0
                            -
                            Math.abs(
                                    noise
                            );


            ridge =
                    clamp01(
                            ridge
                    );


            ridge *=
                    ridge;


            sum +=
                    ridge
                            * weight;


            totalWeight +=
                    weight;


            scale *=
                    0.5;


            weight *=
                    0.54;
        }


        if (
                totalWeight <= 0.0
        ) {

            return 0.0;
        }


        return clamp01(
                sum
                        / totalWeight
        );
    }


    private static double fractalNoise01(
            double x,
            double z,
            double baseScale,
            long seed,
            int octaves
    ) {

        return clamp01(
                signedFractalNoise(
                        x,
                        z,
                        baseScale,
                        seed,
                        octaves
                )
                        * 0.5
                        + 0.5
        );
    }


    private static double signedFractalNoise(
            double x,
            double z,
            double baseScale,
            long seed,
            int octaves
    ) {

        double sum =
                0.0;

        double weight =
                1.0;

        double totalWeight =
                0.0;

        double scale =
                baseScale;


        for (
                int octave = 0;
                octave < octaves;
                octave++
        ) {

            sum +=
                    gradientNoise(
                            x,
                            z,
                            scale,
                            seed
                                    +
                                    octave
                                            * 0x632BE59BD9B4E019L
                    )
                            * weight;


            totalWeight +=
                    weight;


            scale *=
                    0.5;


            weight *=
                    0.52;
        }


        if (
                totalWeight <= 0.0
        ) {

            return 0.0;
        }


        return clamp(
                sum
                        / totalWeight,
                -1.0,
                1.0
        );
    }


    private static double gradientNoise(
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
                floorToInt(
                        scaledX
                );

        int z0 =
                floorToInt(
                        scaledZ
                );


        int x1 =
                x0 + 1;

        int z1 =
                z0 + 1;


        double localX =
                scaledX
                        - x0;

        double localZ =
                scaledZ
                        - z0;


        double n00 =
                gradientDot(
                        x0,
                        z0,
                        localX,
                        localZ,
                        seed
                );


        double n10 =
                gradientDot(
                        x1,
                        z0,
                        localX - 1.0,
                        localZ,
                        seed
                );


        double n01 =
                gradientDot(
                        x0,
                        z1,
                        localX,
                        localZ - 1.0,
                        seed
                );


        double n11 =
                gradientDot(
                        x1,
                        z1,
                        localX - 1.0,
                        localZ - 1.0,
                        seed
                );


        double fadeX =
                quinticFade(
                        localX
                );

        double fadeZ =
                quinticFade(
                        localZ
                );


        double north =
                lerp(
                        n00,
                        n10,
                        fadeX
                );


        double south =
                lerp(
                        n01,
                        n11,
                        fadeX
                );


        return clamp(
                lerp(
                        north,
                        south,
                        fadeZ
                )
                        * 1.41421356237,
                -1.0,
                1.0
        );
    }


    private static double gradientDot(
            int latticeX,
            int latticeZ,
            double offsetX,
            double offsetZ,
            long seed
    ) {

        long hash =
                hash(
                        latticeX,
                        latticeZ,
                        seed
                );


        int direction =
                (int) (
                        hash & 7L
                );


        return switch (
                direction
                ) {

            case 0 ->
                    offsetX;

            case 1 ->
                    -offsetX;

            case 2 ->
                    offsetZ;

            case 3 ->
                    -offsetZ;

            case 4 ->
                    (
                            offsetX
                                    + offsetZ
                    )
                            * 0.70710678118;

            case 5 ->
                    (
                            -offsetX
                                    + offsetZ
                    )
                            * 0.70710678118;

            case 6 ->
                    (
                            offsetX
                                    - offsetZ
                    )
                            * 0.70710678118;

            default ->
                    (
                            -offsetX
                                    - offsetZ
                    )
                            * 0.70710678118;
        };
    }


    private static long hash(
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


        return value;
    }


    private static double quinticFade(
            double value
    ) {

        return value
                * value
                * value
                * (
                value
                        * (
                        value
                                * 6.0
                                - 15.0
                )
                        + 10.0
        );
    }


    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {

        if (
                edge1 <= edge0
        ) {

            return value >= edge1
                    ?
                    1.0
                    :
                    0.0;
        }


        double t =
                (
                        value
                                - edge0
                )
                        /
                        (
                                edge1
                                        - edge0
                        );


        t =
                clamp01(
                        t
                );


        return t
                * t
                * (
                3.0
                        - 2.0
                        * t
        );
    }


    private static int floorToInt(
            double value
    ) {

        return (int) Math.floor(
                value
        );
    }


    private static double clamp01(
            double value
    ) {

        return clamp(
                value,
                0.0,
                1.0
        );
    }


    private static double clamp(
            double value,
            double minimum,
            double maximum
    ) {

        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
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