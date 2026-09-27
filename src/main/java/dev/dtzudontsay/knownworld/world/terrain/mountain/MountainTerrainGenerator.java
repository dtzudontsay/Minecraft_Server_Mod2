package dev.dtzudontsay.knownworld.world.terrain.mountain;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;

public final class MountainTerrainGenerator {

    /*
     * ========================================================
     * DESIGN
     * ========================================================
     *
     * The authored relief raster answers only:
     *
     *     "How mountainous is this geographic location?"
     *
     * It does NOT define the actual mountain surface.
     *
     * Actual mountains are generated here at Minecraft-scale
     * coordinates.
     *
     * Each primary grid cell creates a MASSIF:
     *
     *     main summit
     *        +
     *     two connected subsidiary summits
     *
     * Nearby massifs overlap slightly, producing mountain chains,
     * saddles and passes without creating one continuous plateau.
     */


    /*
     * ========================================================
     * PRIMARY MASSIFS
     * ========================================================
     *
     * Much tighter than the previous generator.
     *
     * This is intentional.
     *
     * A mountain that rises 1000 blocks over a horizontal radius
     * of only a few thousand blocks actually reads as a mountain
     * in Minecraft.
     */

    private static final double PRIMARY_SPACING =
            3_200.0;

    private static final double PRIMARY_MIN_RADIUS =
            2_400.0;

    private static final double PRIMARY_MAX_RADIUS =
            4_100.0;

    private static final double PRIMARY_MIN_HEIGHT =
            520.0;

    private static final double PRIMARY_MAX_HEIGHT =
            1_420.0;


    /*
     * ========================================================
     * SUBSIDIARY SUMMITS
     * ========================================================
     *
     * Every major massif gets two extra summits.
     *
     * These prevent the mountain from looking like a single
     * smooth cone or dome.
     */

    private static final double SUBPEAK_MIN_DISTANCE =
            650.0;

    private static final double SUBPEAK_MAX_DISTANCE =
            1_450.0;

    private static final double SUBPEAK_MIN_HEIGHT_FACTOR =
            0.48;

    private static final double SUBPEAK_MAX_HEIGHT_FACTOR =
            0.78;

    private static final double SUBPEAK_MIN_RADIUS_FACTOR =
            0.48;

    private static final double SUBPEAK_MAX_RADIUS_FACTOR =
            0.72;


    /*
     * ========================================================
     * SECONDARY MOUNTAINS
     * ========================================================
     *
     * These fill the gaps between the major massifs.
     */

    private static final double SECONDARY_SPACING =
            1_250.0;

    private static final double SECONDARY_MIN_RADIUS =
            700.0;

    private static final double SECONDARY_MAX_RADIUS =
            1_500.0;

    private static final double SECONDARY_MIN_HEIGHT =
            100.0;

    private static final double SECONDARY_MAX_HEIGHT =
            360.0;


    /*
     * ========================================================
     * RANGE CONNECTION
     * ========================================================
     *
     * Only a small fraction of the second-strongest mountain is
     * retained.
     *
     * This creates saddles between nearby mountains without
     * filling the entire region into one raised sheet.
     */

    private static final double SECOND_PRIMARY_FACTOR =
            0.10;


    /*
     * There is deliberately almost no generic "mountain floor".
     *
     * Previously this helped create the giant raised staircase.
     */

    private static final double RANGE_FLOOR_MAX =
            18.0;


    /*
     * The authored mask still progressively reduces mountains
     * toward the outer edge of the range.
     */

    private static final double RELIEF_POWER =
            0.90;


    /*
     * ========================================================
     * ORDINARY TERRAIN
     * ========================================================
     */

    private static final double BROAD_ROLLING_SCALE =
            1_100.0;

    private static final double BROAD_ROLLING_AMPLITUDE =
            9.0;

    private static final double SMALL_ROLLING_SCALE =
            340.0;

    private static final double SMALL_ROLLING_AMPLITUDE =
            3.5;


    /*
     * ========================================================
     * MOUNTAIN SURFACE DETAIL
     * ========================================================
     *
     * These do not create the mountains.
     *
     * They break mathematically perfect slopes and introduce
     * shoulders / unevenness to mountain faces.
     */

    private static final double LARGE_DETAIL_SCALE =
            620.0;

    private static final double LARGE_DETAIL_AMPLITUDE =
            24.0;

    private static final double SMALL_DETAIL_SCALE =
            210.0;

    private static final double SMALL_DETAIL_AMPLITUDE =
            8.0;


    /*
     * Very broad modulation changes the character of different
     * stretches of a mountain range.
     *
     * It modifies summit height, not the underlying land height.
     */

    private static final double RANGE_MODULATION_SCALE =
            24_000.0;


    /*
     * ========================================================
     * CANONICAL SEEDS
     * ========================================================
     *
     * Fixed seeds are intentional.
     *
     * The geography should not change just because the player
     * enters another Minecraft world seed.
     */

    private static final long PRIMARY_SEED =
            0x41C64E6DL;

    private static final long SECONDARY_SEED =
            0x9E3779B97F4A7C15L;

    private static final long ROLLING_SEED =
            0x632BE59BD9B4E019L;

    private static final long DETAIL_SEED =
            0xC6BC279692B5CC83L;

    private static final long MODULATION_SEED =
            0x94D049BB133111EBL;


    public double sampleLandOffset(
            WorldCoordinate coordinate,
            double reliefIntensity
    ) {

        double east =
                coordinate.eastMetres();

        double north =
                coordinate.northMetres();

        double rolling =
                signedNoise(
                        east,
                        north,
                        BROAD_ROLLING_SCALE,
                        ROLLING_SEED
                )
                        * BROAD_ROLLING_AMPLITUDE
                        +
                        signedNoise(
                                east,
                                north,
                                SMALL_ROLLING_SCALE,
                                ROLLING_SEED
                                        ^ 0x1234ABCDL
                        )
                                * SMALL_ROLLING_AMPLITUDE;

        double relief =
                clamp01(
                        reliefIntensity
                );

        if (
                relief <= 0.001
        ) {
            return rolling;
        }

        /*
         * Edge taper.
         *
         * Strong authored relief:
         *     full-size mountains.
         *
         * Weak relief near range boundary:
         *     smaller mountains / foothills.
         */

        double envelope =
                Math.pow(
                        relief,
                        RELIEF_POWER
                );

        /*
         * Some stretches of a canonical range naturally become
         * more dramatic than others.
         */

        double modulation =
                0.78
                        +
                        valueNoise(
                                east,
                                north,
                                RANGE_MODULATION_SCALE,
                                MODULATION_SEED
                        )
                                * 0.40;

        double primary =
                sampleMassifField(
                        east,
                        north,
                        envelope
                );

        primary *=
                modulation;

        double secondary =
                samplePeakField(
                        east,
                        north,
                        SECONDARY_SPACING,
                        SECONDARY_MIN_RADIUS,
                        SECONDARY_MAX_RADIUS,
                        SECONDARY_MIN_HEIGHT,
                        SECONDARY_MAX_HEIGHT,
                        SECONDARY_SEED,
                        1.42
                );

        /*
         * Detail affects mountains but does not raise the whole
         * mountain region.
         */

        double mountainPresence =
                clamp01(
                        (
                                primary
                                        + secondary
                        )
                                / 500.0
                );

        double detail =
                (
                        signedNoise(
                                east,
                                north,
                                LARGE_DETAIL_SCALE,
                                DETAIL_SEED
                        )
                                * LARGE_DETAIL_AMPLITUDE
                                +
                                signedNoise(
                                        east,
                                        north,
                                        SMALL_DETAIL_SCALE,
                                        DETAIL_SEED
                                                ^ 0x761E37ABL
                                )
                                        * SMALL_DETAIL_AMPLITUDE
                )
                        * mountainPresence
                        * envelope;

        double rangeFloor =
                envelope
                        * RANGE_FLOOR_MAX;

        double mountainElevation =
                envelope
                        * (
                        primary
                                + secondary
                )
                        +
                        rangeFloor
                        +
                        detail;

        return rolling
                + Math.max(
                0.0,
                mountainElevation
        );
    }


    /*
     * ========================================================
     * MASSIF FIELD
     * ========================================================
     *
     * One cell does NOT equal one mountain anymore.
     *
     * One cell produces a connected three-summit massif.
     */

    private static double sampleMassifField(
            double x,
            double z,
            double envelope
    ) {

        int cellX =
                floorToInt(
                        x / PRIMARY_SPACING
                );

        int cellZ =
                floorToInt(
                        z / PRIMARY_SPACING
                );

        double highest =
                0.0;

        double secondHighest =
                0.0;

        for (
                int dz = -2;
                dz <= 2;
                dz++
        ) {

            for (
                    int dx = -2;
                    dx <= 2;
                    dx++
            ) {

                int massifCellX =
                        cellX + dx;

                int massifCellZ =
                        cellZ + dz;

                double centreX =
                        (
                                massifCellX
                                        + 0.12
                                        + random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        1
                                )
                                        * 0.76
                        )
                                * PRIMARY_SPACING;

                double centreZ =
                        (
                                massifCellZ
                                        + 0.12
                                        + random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        2
                                )
                                        * 0.76
                        )
                                * PRIMARY_SPACING;

                double radius =
                        lerp(
                                PRIMARY_MIN_RADIUS,
                                PRIMARY_MAX_RADIUS,
                                random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        3
                                )
                        );

                double height =
                        lerp(
                                PRIMARY_MIN_HEIGHT,
                                PRIMARY_MAX_HEIGHT,
                                random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        4
                                )
                        );

                /*
                 * Near the outer range boundary, reduce actual
                 * mountain size as well as mountain height.
                 *
                 * This is important:
                 *
                 * foothills should be genuinely smaller mountains,
                 * not enormous mountains flattened vertically.
                 */

                double localRadiusScale =
                        0.55
                                + envelope * 0.45;

                radius *=
                        localRadiusScale;

                double orientation =
                        random01(
                                massifCellX,
                                massifCellZ,
                                PRIMARY_SEED,
                                5
                        )
                                * Math.PI
                                * 2.0;

                double aspect =
                        lerp(
                                0.58,
                                0.82,
                                random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        6
                                )
                        );

                double main =
                        peakContribution(
                                x,
                                z,
                                centreX,
                                centreZ,
                                radius,
                                radius * aspect,
                                orientation,
                                height,
                                1.34
                        );

                /*
                 * Subsidiary peak A.
                 */

                double subDistanceA =
                        lerp(
                                SUBPEAK_MIN_DISTANCE,
                                SUBPEAK_MAX_DISTANCE,
                                random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        7
                                )
                        );

                double subAngleA =
                        orientation
                                + lerp(
                                -0.55,
                                0.55,
                                random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        8
                                )
                        );

                double subCentreAX =
                        centreX
                                + Math.cos(
                                subAngleA
                        )
                                * subDistanceA;

                double subCentreAZ =
                        centreZ
                                + Math.sin(
                                subAngleA
                        )
                                * subDistanceA;

                double subHeightA =
                        height
                                * lerp(
                                SUBPEAK_MIN_HEIGHT_FACTOR,
                                SUBPEAK_MAX_HEIGHT_FACTOR,
                                random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        9
                                )
                        );

                double subRadiusA =
                        radius
                                * lerp(
                                SUBPEAK_MIN_RADIUS_FACTOR,
                                SUBPEAK_MAX_RADIUS_FACTOR,
                                random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        10
                                )
                        );

                double subA =
                        peakContribution(
                                x,
                                z,
                                subCentreAX,
                                subCentreAZ,
                                subRadiusA,
                                subRadiusA
                                        * 0.72,
                                orientation
                                        + 0.20,
                                subHeightA,
                                1.30
                        );

                /*
                 * Subsidiary peak B.
                 *
                 * Usually placed on the opposite side of the main
                 * summit so the three peaks read as a ridge.
                 */

                double subDistanceB =
                        lerp(
                                SUBPEAK_MIN_DISTANCE,
                                SUBPEAK_MAX_DISTANCE,
                                random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        11
                                )
                        );

                double subAngleB =
                        orientation
                                + Math.PI
                                + lerp(
                                -0.55,
                                0.55,
                                random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        12
                                )
                        );

                double subCentreBX =
                        centreX
                                + Math.cos(
                                subAngleB
                        )
                                * subDistanceB;

                double subCentreBZ =
                        centreZ
                                + Math.sin(
                                subAngleB
                        )
                                * subDistanceB;

                double subHeightB =
                        height
                                * lerp(
                                SUBPEAK_MIN_HEIGHT_FACTOR,
                                SUBPEAK_MAX_HEIGHT_FACTOR,
                                random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        13
                                )
                        );

                double subRadiusB =
                        radius
                                * lerp(
                                SUBPEAK_MIN_RADIUS_FACTOR,
                                SUBPEAK_MAX_RADIUS_FACTOR,
                                random01(
                                        massifCellX,
                                        massifCellZ,
                                        PRIMARY_SEED,
                                        14
                                )
                        );

                double subB =
                        peakContribution(
                                x,
                                z,
                                subCentreBX,
                                subCentreBZ,
                                subRadiusB,
                                subRadiusB
                                        * 0.72,
                                orientation
                                        - 0.20,
                                subHeightB,
                                1.30
                        );

                /*
                 * Main summit remains dominant.
                 *
                 * Subsidiary summits add shoulders and secondary
                 * peaks rather than simply stacking height.
                 */

                double massif =
                        Math.max(
                                main,
                                Math.max(
                                        subA,
                                        subB
                                )
                        );

                /*
                 * A small amount of the weaker local peaks joins
                 * the three summits into one climbable massif.
                 */

                double localSecond =
                        secondLargest(
                                main,
                                subA,
                                subB
                        );

                massif +=
                        localSecond * 0.14;

                if (
                        massif > highest
                ) {

                    secondHighest =
                            highest;

                    highest =
                            massif;

                } else if (
                        massif > secondHighest
                ) {

                    secondHighest =
                            massif;
                }
            }
        }

        return highest
                + secondHighest
                * SECOND_PRIMARY_FACTOR;
    }


    /*
     * ========================================================
     * SECONDARY PEAK FIELD
     * ========================================================
     */

    private static double samplePeakField(
            double x,
            double z,
            double spacing,
            double minimumRadius,
            double maximumRadius,
            double minimumHeight,
            double maximumHeight,
            long seed,
            double profilePower
    ) {

        int cellX =
                floorToInt(
                        x / spacing
                );

        int cellZ =
                floorToInt(
                        z / spacing
                );

        double highest =
                0.0;

        double secondHighest =
                0.0;

        for (
                int dz = -2;
                dz <= 2;
                dz++
        ) {

            for (
                    int dx = -2;
                    dx <= 2;
                    dx++
            ) {

                int peakCellX =
                        cellX + dx;

                int peakCellZ =
                        cellZ + dz;

                double centreX =
                        (
                                peakCellX
                                        + 0.15
                                        + random01(
                                        peakCellX,
                                        peakCellZ,
                                        seed,
                                        1
                                )
                                        * 0.70
                        )
                                * spacing;

                double centreZ =
                        (
                                peakCellZ
                                        + 0.15
                                        + random01(
                                        peakCellX,
                                        peakCellZ,
                                        seed,
                                        2
                                )
                                        * 0.70
                        )
                                * spacing;

                double radius =
                        lerp(
                                minimumRadius,
                                maximumRadius,
                                random01(
                                        peakCellX,
                                        peakCellZ,
                                        seed,
                                        3
                                )
                        );

                double aspect =
                        lerp(
                                0.58,
                                0.88,
                                random01(
                                        peakCellX,
                                        peakCellZ,
                                        seed,
                                        4
                                )
                        );

                double angle =
                        random01(
                                peakCellX,
                                peakCellZ,
                                seed,
                                5
                        )
                                * Math.PI
                                * 2.0;

                double height =
                        lerp(
                                minimumHeight,
                                maximumHeight,
                                random01(
                                        peakCellX,
                                        peakCellZ,
                                        seed,
                                        6
                                )
                        );

                double contribution =
                        peakContribution(
                                x,
                                z,
                                centreX,
                                centreZ,
                                radius,
                                radius * aspect,
                                angle,
                                height,
                                profilePower
                        );

                if (
                        contribution > highest
                ) {

                    secondHighest =
                            highest;

                    highest =
                            contribution;

                } else if (
                        contribution > secondHighest
                ) {

                    secondHighest =
                            contribution;
                }
            }
        }

        return highest
                + secondHighest * 0.08;
    }


    /*
     * ========================================================
     * MOUNTAIN PROFILE
     * ========================================================
     *
     * This is one of the biggest differences from the previous
     * generator.
     *
     * DO NOT smoothstep the whole mountain profile.
     *
     * smoothstep produced large gentle shoulders and broad contour
     * terraces.
     *
     * Instead:
     *
     *     profile = (1 - distance)^power
     *
     * gives:
     *
     * - a real summit
     * - strong middle slopes
     * - a smoothly fading mountain foot
     *
     * There is no flat top.
     */

    private static double peakContribution(
            double x,
            double z,
            double centreX,
            double centreZ,
            double majorRadius,
            double minorRadius,
            double angle,
            double height,
            double profilePower
    ) {

        double relativeX =
                x - centreX;

        double relativeZ =
                z - centreZ;

        double cos =
                Math.cos(
                        angle
                );

        double sin =
                Math.sin(
                        angle
                );

        double rotatedX =
                relativeX * cos
                        + relativeZ * sin;

        double rotatedZ =
                -relativeX * sin
                        + relativeZ * cos;

        double normalizedX =
                rotatedX
                        / majorRadius;

        double normalizedZ =
                rotatedZ
                        / minorRadius;

        double distance =
                Math.sqrt(
                        normalizedX
                                * normalizedX
                                +
                                normalizedZ
                                        * normalizedZ
                );

        if (
                distance >= 1.0
        ) {
            return 0.0;
        }

        double profile =
                1.0 - distance;

        profile =
                Math.pow(
                        profile,
                        profilePower
                );

        return height
                * profile;
    }


    /*
     * ========================================================
     * CONTINUOUS DETAIL NOISE
     * ========================================================
     */

    private static double signedNoise(
            double x,
            double z,
            double scale,
            long seed
    ) {

        return valueNoise(
                x,
                z,
                scale,
                seed
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

        double tx =
                smootherstep(
                        scaledX - x0
                );

        double tz =
                smootherstep(
                        scaledZ - z0
                );

        double north =
                lerp(
                        random01(
                                x0,
                                z0,
                                seed,
                                21
                        ),
                        random01(
                                x1,
                                z0,
                                seed,
                                21
                        ),
                        tx
                );

        double south =
                lerp(
                        random01(
                                x0,
                                z1,
                                seed,
                                21
                        ),
                        random01(
                                x1,
                                z1,
                                seed,
                                21
                        ),
                        tx
                );

        return lerp(
                north,
                south,
                tz
        );
    }


    /*
     * ========================================================
     * HASH
     * ========================================================
     */

    private static double random01(
            int x,
            int z,
            long seed,
            int channel
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
                (long) channel
                        * 0x165667B19E3779F9L;

        value =
                mix64(
                        value
                );

        return (
                value >>> 11
        )
                * 0x1.0p-53;
    }

    private static long mix64(
            long value
    ) {

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
     * ========================================================
     * HELPERS
     * ========================================================
     */

    private static double secondLargest(
            double a,
            double b,
            double c
    ) {

        if (
                a >= b
        ) {

            if (
                    b >= c
            ) {
                return b;
            }

            return Math.min(
                    a,
                    c
            );
        }

        if (
                a >= c
        ) {
            return a;
        }

        return Math.min(
                b,
                c
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

    private static double smootherstep(
            double value
    ) {

        value =
                clamp01(
                        value
                );

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