package dev.dtzudontsay.knownworld.world.terrain.mountain;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;

public final class MountainTerrainGenerator {

    /*
     * ============================================================
     * PURPOSE
     * ============================================================
     *
     * The authored relief raster says WHERE mountain terrain belongs
     * and roughly how strongly mountainous the location should be.
     *
     * It does NOT define final elevation.
     *
     * Final mountain geometry is generated here directly from
     * canonical world coordinates at Minecraft-block scale.
     *
     * No circles.
     * No elliptical domes.
     * No one-peak-per-cell system.
     *
     * Instead:
     *
     *   relief envelope
     *       +
     *   domain warping
     *       +
     *   broad mountain structure
     *       +
     *   major ridges
     *       +
     *   secondary ridges
     *       +
     *   broken slope detail
     *
     * This is intended to create large Minecraft-style mountain
     * chains with repeated peaks, valleys, saddles and irregular
     * faces.
     */


    /*
     * ============================================================
     * RANGE ENVELOPE
     * ============================================================
     */

    private static final double RELIEF_ENVELOPE_POWER =
            0.82;

    /*
     * There is deliberately almost no generic uplift simply because
     * a point lies inside a mountain region.
     *
     * Otherwise the complete authored region turns into one elevated
     * tableland again.
     */
    private static final double RANGE_BASE_UPLIFT =
            14.0;


    /*
     * ============================================================
     * DOMAIN WARPING
     * ============================================================
     *
     * Straight noise tends to produce obviously procedural,
     * repetitive ridges.
     *
     * These warp fields bend, fork and distort the mountains before
     * the actual mountain noise is evaluated.
     */

    private static final double LARGE_WARP_SCALE =
            11_000.0;

    private static final double LARGE_WARP_AMPLITUDE =
            2_600.0;

    private static final double MEDIUM_WARP_SCALE =
            3_800.0;

    private static final double MEDIUM_WARP_AMPLITUDE =
            850.0;


    /*
     * ============================================================
     * MOUNTAIN STRUCTURE
     * ============================================================
     *
     * These are actual geographic wavelengths in metres / blocks.
     *
     * A strong mountain chain contains structure on all of these
     * scales simultaneously.
     */

    private static final double BROAD_STRUCTURE_SCALE =
            8_500.0;

    private static final double MAJOR_RIDGE_SCALE =
            3_400.0;

    private static final double SECONDARY_RIDGE_SCALE =
            1_350.0;

    private static final double LOCAL_RIDGE_SCALE =
            520.0;

    private static final double FACE_DETAIL_SCALE =
            190.0;


    /*
     * Maximum vertical contribution.
     *
     * With the current custom dimension ceiling near Y=2031 and the
     * ~66-105 m macro land base, this still leaves useful headroom.
     */
    private static final double MAX_MOUNTAIN_UPLIFT =
            1_650.0;


    /*
     * ============================================================
     * ORDINARY NON-MOUNTAIN TERRAIN
     * ============================================================
     */

    private static final double PLAINS_BROAD_SCALE =
            1_300.0;

    private static final double PLAINS_BROAD_AMPLITUDE =
            8.0;

    private static final double PLAINS_FINE_SCALE =
            420.0;

    private static final double PLAINS_FINE_AMPLITUDE =
            3.0;


    /*
     * ============================================================
     * MOUNTAIN FACE BREAKUP
     * ============================================================
     *
     * These values add smaller irregularities AFTER the large
     * mountain silhouette exists.
     *
     * They are deliberately much smaller than the mountain height.
     */
    private static final double FACE_DETAIL_AMPLITUDE =
            46.0;

    private static final double LOCAL_RIDGE_DETAIL_AMPLITUDE =
            82.0;


    /*
     * ============================================================
     * FIXED CANONICAL SEEDS
     * ============================================================
     */

    private static final long WARP_X_SEED =
            0x32A4D71BL;

    private static final long WARP_Z_SEED =
            0x7F4A7C159E3779B9L;

    private static final long STRUCTURE_SEED =
            0x632BE59BD9B4E019L;

    private static final long RIDGE_SEED =
            0xC6BC279692B5CC83L;

    private static final long DETAIL_SEED =
            0x94D049BB133111EBL;

    private static final long PLAINS_SEED =
            0xD1B54A32D192ED03L;


    public double sampleLandOffset(
            WorldCoordinate coordinate,
            double reliefIntensity
    ) {

        double x =
                coordinate.eastMetres();

        double z =
                coordinate.northMetres();

        double relief =
                clamp01(
                        reliefIntensity
                );

        /*
         * Ordinary terrain continues everywhere, including underneath
         * mountains. This avoids perfectly flat valleys.
         */
        double ordinaryTerrain =
                sampleOrdinaryTerrain(
                        x,
                        z
                );

        if (
                relief <= 0.001
        ) {
            return ordinaryTerrain;
        }

        double envelope =
                Math.pow(
                        relief,
                        RELIEF_ENVELOPE_POWER
                );

        /*
         * ------------------------------------------------------------
         * DOMAIN WARP
         * ------------------------------------------------------------
         */

        double largeWarpX =
                signedFractalNoise(
                        x,
                        z,
                        LARGE_WARP_SCALE,
                        WARP_X_SEED,
                        3
                )
                        * LARGE_WARP_AMPLITUDE;

        double largeWarpZ =
                signedFractalNoise(
                        x,
                        z,
                        LARGE_WARP_SCALE,
                        WARP_Z_SEED,
                        3
                )
                        * LARGE_WARP_AMPLITUDE;

        double warpedX =
                x + largeWarpX;

        double warpedZ =
                z + largeWarpZ;

        double mediumWarpX =
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        MEDIUM_WARP_SCALE,
                        WARP_X_SEED
                                ^ 0x6A09E667L,
                        2
                )
                        * MEDIUM_WARP_AMPLITUDE;

        double mediumWarpZ =
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        MEDIUM_WARP_SCALE,
                        WARP_Z_SEED
                                ^ 0xBB67AE85L,
                        2
                )
                        * MEDIUM_WARP_AMPLITUDE;

        warpedX +=
                mediumWarpX;

        warpedZ +=
                mediumWarpZ;


        /*
         * ------------------------------------------------------------
         * BROAD MOUNTAIN MASSES
         * ------------------------------------------------------------
         *
         * This says where the large mountain masses are stronger and
         * weaker.
         *
         * Importantly, it is NOT added directly as elevation.
         */

        double broad =
                fractalNoise01(
                        warpedX,
                        warpedZ,
                        BROAD_STRUCTURE_SCALE,
                        STRUCTURE_SEED,
                        4
                );

        /*
         * Broader low areas become valleys between major systems.
         */
        double broadMass =
                smoothstep(
                        0.28,
                        0.78,
                        broad
                );


        /*
         * ------------------------------------------------------------
         * MAJOR RIDGES
         * ------------------------------------------------------------
         */

        double majorRidges =
                ridgedFractalNoise(
                        warpedX,
                        warpedZ,
                        MAJOR_RIDGE_SCALE,
                        RIDGE_SEED,
                        4
                );

        /*
         * Thresholding is intentional.
         *
         * A raw noise field tends to raise everything somewhat.
         *
         * Thresholding creates actual low terrain between mountain
         * ridges.
         */
        majorRidges =
                smoothstep(
                        0.30,
                        0.87,
                        majorRidges
                );


        /*
         * ------------------------------------------------------------
         * SECONDARY RIDGES
         * ------------------------------------------------------------
         */

        double secondaryRidges =
                ridgedFractalNoise(
                        warpedX,
                        warpedZ,
                        SECONDARY_RIDGE_SCALE,
                        RIDGE_SEED
                                ^ 0x510E527FL,
                        4
                );

        secondaryRidges =
                smoothstep(
                        0.34,
                        0.90,
                        secondaryRidges
                );


        /*
         * ------------------------------------------------------------
         * LOCAL RIDGES
         * ------------------------------------------------------------
         *
         * This scale is small enough that a player can actually see
         * terrain shape changing while looking at one mountainside.
         */

        double localRidges =
                ridgedFractalNoise(
                        warpedX,
                        warpedZ,
                        LOCAL_RIDGE_SCALE,
                        DETAIL_SEED,
                        3
                );

        localRidges =
                smoothstep(
                        0.40,
                        0.92,
                        localRidges
                );


        /*
         * ------------------------------------------------------------
         * BUILD THE MOUNTAIN SILHOUETTE
         * ------------------------------------------------------------
         *
         * Major ridges provide the main summits.
         *
         * Secondary ridges split those large forms into multiple
         * peaks and shoulders.
         *
         * Broad structure changes how dramatic each section of the
         * mountain range becomes.
         */

        double majorShape =
                majorRidges
                        * (
                        0.52
                                + broadMass
                                * 0.48
                );

        double secondaryShape =
                secondaryRidges
                        * 0.32
                        * (
                        0.40
                                + broadMass
                                * 0.60
                );

        /*
         * Multiplying some local ridge structure into the main shape
         * creates broken summit lines rather than simply stacking
         * another smooth layer vertically.
         */
        double summitBreakup =
                0.72
                        + localRidges
                        * 0.28;

        double mountainShape =
                (
                        majorShape
                                + secondaryShape
                )
                        * summitBreakup;

        /*
         * Normalize useful range.
         */
        mountainShape =
                clamp01(
                        mountainShape
                );

        /*
         * Sharpen mountains without converting them into vertical
         * spikes.
         *
         * Values near zero stay low.
         * Strong ridge values rise rapidly.
         */
        mountainShape =
                Math.pow(
                        mountainShape,
                        1.42
                );


        /*
         * ------------------------------------------------------------
         * RANGE-EDGE BEHAVIOUR
         * ------------------------------------------------------------
         *
         * Near the authored range edge:
         *
         * - elevation decreases
         * - ruggedness decreases
         *
         * But the local mountain shapes remain mountains rather than
         * giant landforms squashed vertically.
         */

        double mountainUplift =
                mountainShape
                        * MAX_MOUNTAIN_UPLIFT
                        * envelope;


        /*
         * ------------------------------------------------------------
         * LOCAL MOUNTAINSIDE DETAIL
         * ------------------------------------------------------------
         */

        double localDetail =
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        LOCAL_RIDGE_SCALE,
                        DETAIL_SEED
                                ^ 0x1F83D9ABL,
                        3
                )
                        * LOCAL_RIDGE_DETAIL_AMPLITUDE;

        double faceDetail =
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        FACE_DETAIL_SCALE,
                        DETAIL_SEED
                                ^ 0x5BE0CD19L,
                        2
                )
                        * FACE_DETAIL_AMPLITUDE;

        /*
         * Detail should be strongest on actual mountain terrain and
         * weak inside valleys.
         */
        double mountainPresence =
                smoothstep(
                        0.06,
                        0.50,
                        mountainShape
                );

        double detail =
                (
                        localDetail
                                + faceDetail
                )
                        * mountainPresence
                        * envelope;


        /*
         * Tiny broad uplift only.
         *
         * We no longer elevate the complete relief region by hundreds
         * of blocks.
         */
        double rangeBase =
                RANGE_BASE_UPLIFT
                        * envelope;


        return ordinaryTerrain
                + rangeBase
                + Math.max(
                0.0,
                mountainUplift
                        + detail
        );
    }


    /*
     * ============================================================
     * ORDINARY TERRAIN
     * ============================================================
     */

    private static double sampleOrdinaryTerrain(
            double x,
            double z
    ) {

        double broad =
                signedFractalNoise(
                        x,
                        z,
                        PLAINS_BROAD_SCALE,
                        PLAINS_SEED,
                        3
                )
                        * PLAINS_BROAD_AMPLITUDE;

        double fine =
                signedFractalNoise(
                        x,
                        z,
                        PLAINS_FINE_SCALE,
                        PLAINS_SEED
                                ^ 0x243F6A88L,
                        2
                )
                        * PLAINS_FINE_AMPLITUDE;

        return broad + fine;
    }


    /*
     * ============================================================
     * RIDGED FRACTAL NOISE
     * ============================================================
     */

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
                                    + octave
                                    * 0x9E3779B97F4A7C15L
                    );

            /*
             * gradientNoise = approximately -1..1
             *
             * Convert into ridge:
             *
             *   0 -> ridge crest
             *  ±1 -> valley
             */
            double ridge =
                    1.0
                            - Math.abs(
                            noise
                    );

            ridge =
                    clamp01(
                            ridge
                    );

            /*
             * Sharpen ridge crests.
             */
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


    /*
     * ============================================================
     * NORMAL FRACTAL NOISE
     * ============================================================
     */

    private static double fractalNoise01(
            double x,
            double z,
            double baseScale,
            long seed,
            int octaves
    ) {

        double signed =
                signedFractalNoise(
                        x,
                        z,
                        baseScale,
                        seed,
                        octaves
                );

        return clamp01(
                signed
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
                                    + octave
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


    /*
     * ============================================================
     * 2D GRADIENT NOISE
     * ============================================================
     *
     * Unlike interpolated random heights, gradient noise naturally
     * creates directional slopes and is much better suited to terrain
     * surfaces.
     */

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
                scaledX - x0;

        double localZ =
                scaledZ - z0;

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

        /*
         * Approximate normalization for this 2D gradient set.
         */
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


    /*
     * ============================================================
     * HASH
     * ============================================================
     */

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


    /*
     * ============================================================
     * HELPERS
     * ============================================================
     */

    private static double quinticFade(
            double value
    ) {

        return value
                * value
                * value
                * (
                value
                        * (
                        value * 6.0
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
                    ? 1.0
                    : 0.0;
        }

        double t =
                (
                        value - edge0
                )
                        / (
                        edge1 - edge0
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

        return (
                int
                ) Math.floor(
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
                + (
                end - start
        )
                * factor;
    }
}