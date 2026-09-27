package dev.dtzudontsay.knownworld.world.terrain.mountain;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.terrain.mountain.MountainRegionProfileProvider.MountainProfileSample;
import dev.dtzudontsay.knownworld.world.terrain.mountain.MountainRegionProfileProvider.MountainStyle;

public final class MountainTerrainGenerator {

    private static final double RELIEF_ENVELOPE_POWER =
            0.82;

    private static final double RANGE_BASE_UPLIFT =
            14.0;


    /*
     * ============================================================
     * DOMAIN WARPING
     * ============================================================
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
     * NORMAL MOUNTAIN STRUCTURE
     * ============================================================
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
     * ============================================================
     * ORDINARY TERRAIN
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
     * MOTHER OF MOUNTAINS
     * ============================================================
     *
     * This is intentionally different from every ordinary range.
     *
     * The complete configured Mother region suppresses the normal
     * procedural mountain-chain field.
     *
     * Only one mountain is generated around the configured centre.
     */

    private static final double SINGULAR_WARP_SCALE =
            3_200.0;

    private static final double SINGULAR_WARP_AMPLITUDE =
            650.0;

    private static final double SINGULAR_FACE_SCALE =
            650.0;

    private static final double SINGULAR_FINE_SCALE =
            210.0;


    /*
     * ============================================================
     * FIXED SEEDS
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

    private static final long SINGULAR_SEED =
            0xA24BAED4963EE407L;


    public double sampleLandOffset(
            WorldCoordinate coordinate,
            double reliefIntensity,
            double baseElevation,
            MountainProfileSample profile
    ) {

        double ordinaryTerrain =
                sampleOrdinaryTerrain(
                        coordinate.eastMetres(),
                        coordinate.northMetres()
                );


        if (
                profile.style()
                        == MountainStyle.SINGULAR
        ) {

            return sampleSingularMountain(
                    coordinate,
                    baseElevation,
                    ordinaryTerrain,
                    profile
            );
        }


        return sampleNormalMountainTerrain(
                coordinate,
                reliefIntensity,
                baseElevation,
                ordinaryTerrain,
                profile
        );
    }


    private double sampleNormalMountainTerrain(
            WorldCoordinate coordinate,
            double reliefIntensity,
            double baseElevation,
            double ordinaryTerrain,
            MountainProfileSample profile
    ) {

        double x =
                coordinate.eastMetres();

        double z =
                coordinate.northMetres();

        double relief =
                clamp01(
                        reliefIntensity
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


        warpedX +=
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        MEDIUM_WARP_SCALE,
                        WARP_X_SEED
                                ^ 0x6A09E667L,
                        2
                )
                        * MEDIUM_WARP_AMPLITUDE;

        warpedZ +=
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        MEDIUM_WARP_SCALE,
                        WARP_Z_SEED
                                ^ 0xBB67AE85L,
                        2
                )
                        * MEDIUM_WARP_AMPLITUDE;


        double broad =
                fractalNoise01(
                        warpedX,
                        warpedZ,
                        BROAD_STRUCTURE_SCALE,
                        STRUCTURE_SEED,
                        4
                );

        double broadMass =
                smoothstep(
                        0.28,
                        0.78,
                        broad
                );


        double majorRidges =
                ridgedFractalNoise(
                        warpedX,
                        warpedZ,
                        MAJOR_RIDGE_SCALE,
                        RIDGE_SEED,
                        4
                );

        majorRidges =
                smoothstep(
                        0.30,
                        0.87,
                        majorRidges
                );


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


        mountainShape =
                clamp01(
                        mountainShape
                );


        mountainShape =
                Math.pow(
                        mountainShape,
                        1.42
                );


        double mountainPresence =
                smoothstep(
                        0.06,
                        0.50,
                        mountainShape
                );


        /*
         * Detail is now expressed as a FRACTION of the mountain
         * budget instead of raw blocks.
         *
         * This allows a 200-block hill region and a 1500-block
         * mountain region to use the same natural geometry without
         * detail accidentally breaking their ceiling.
         */

        double detailFraction =
                signedFractalNoise(
                        warpedX,
                        warpedZ,
                        LOCAL_RIDGE_SCALE,
                        DETAIL_SEED
                                ^ 0x1F83D9ABL,
                        3
                )
                        * 0.055
                        +
                        signedFractalNoise(
                                warpedX,
                                warpedZ,
                                FACE_DETAIL_SCALE,
                                DETAIL_SEED
                                        ^ 0x5BE0CD19L,
                                2
                        )
                                * 0.028;


        double detailedShape =
                mountainShape
                        +
                        detailFraction
                                * mountainPresence
                                * (
                                1.0
                                        - mountainShape
                                        * 0.70
                        );


        detailedShape =
                clamp01(
                        detailedShape
                );


        double rangeBase =
                RANGE_BASE_UPLIFT
                        * envelope;


        double nonMountainOffset =
                ordinaryTerrain
                        + rangeBase;


        /*
         * maxSummitY is ABSOLUTE Minecraft Y.
         *
         * We derive the amount of vertical space available above the
         * local base rather than generating huge terrain and clipping
         * it afterward.
         *
         * This is what prevents flat "cut-off" summits.
         */

        double mountainBudget =
                Math.max(
                        0.0,
                        profile.maxSummitY()
                                - baseElevation
                                - nonMountainOffset
                );


        double mountainUplift =
                detailedShape
                        * mountainBudget
                        * envelope;


        return nonMountainOffset
                + mountainUplift;
    }


    private double sampleSingularMountain(
            WorldCoordinate coordinate,
            double baseElevation,
            double ordinaryTerrain,
            MountainProfileSample profile
    ) {

        double radius =
                profile.singularMountainRadiusMetres();


        if (
                radius <= 0.0
        ) {

            return ordinaryTerrain;
        }


        double relativeX =
                coordinate.eastMetres()
                        - profile.singularCenterEastMetres();

        double relativeZ =
                coordinate.northMetres()
                        - profile.singularCenterNorthMetres();


        /*
         * Small coordinate distortion gives the mountain an
         * irregular footprint rather than a perfect mathematical
         * cone.
         */

        double warpedX =
                relativeX
                        +
                        signedFractalNoise(
                                coordinate.eastMetres(),
                                coordinate.northMetres(),
                                SINGULAR_WARP_SCALE,
                                SINGULAR_SEED,
                                3
                        )
                                * SINGULAR_WARP_AMPLITUDE;

        double warpedZ =
                relativeZ
                        +
                        signedFractalNoise(
                                coordinate.eastMetres(),
                                coordinate.northMetres(),
                                SINGULAR_WARP_SCALE,
                                SINGULAR_SEED
                                        ^ 0xB7E15162L,
                                3
                        )
                                * SINGULAR_WARP_AMPLITUDE;


        /*
         * Slightly elliptical but still recognisably one mountain.
         */

        double normalizedDistance =
                Math.sqrt(
                        square(
                                warpedX
                                        / radius
                        )
                                +
                                square(
                                        warpedZ
                                                / (
                                                radius
                                                        * 0.88
                                        )
                                )
                );


        if (
                normalizedDistance >= 1.0
        ) {

            return ordinaryTerrain;
        }


        double core =
                1.0
                        - normalizedDistance;


        /*
         * Close to linear gives the mountain a strong visible rise
         * from the surrounding terrain.
         */

        double baseShape =
                Math.pow(
                        core,
                        0.94
                );


        /*
         * These details alter the flanks and ridges but are tapered
         * away at both the summit and the outer edge, preserving one
         * dominant summit.
         */

        double ridgeDetail =
                (
                        ridgedFractalNoise(
                                coordinate.eastMetres(),
                                coordinate.northMetres(),
                                SINGULAR_FACE_SCALE,
                                SINGULAR_SEED
                                        ^ 0x243F6A88L,
                                4
                        )
                                - 0.5
                )
                        * 0.18;


        double fineDetail =
                signedFractalNoise(
                        coordinate.eastMetres(),
                        coordinate.northMetres(),
                        SINGULAR_FINE_SCALE,
                        SINGULAR_SEED
                                ^ 0x85A308D3L,
                        2
                )
                        * 0.055;


        double taper =
                baseShape
                        * (
                        1.0
                                - baseShape
                );


        double finalShape =
                baseShape
                        +
                        (
                                ridgeDetail
                                        + fineDetail
                        )
                                * taper;


        finalShape =
                clamp01(
                        finalShape
                );


        double mountainBudget =
                Math.max(
                        0.0,
                        profile.maxSummitY()
                                - baseElevation
                                - ordinaryTerrain
                );


        return ordinaryTerrain
                +
                finalShape
                        * mountainBudget;
    }


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


        return broad
                + fine;
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
                            - Math.abs(
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

        return (
                int
                ) Math.floor(
                value
        );
    }


    private static double square(
            double value
    ) {

        return value
                * value;
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