package dev.dtzudontsay.knownworld.world.geography.calibration;

import dev.dtzudontsay.knownworld.world.geography.GeographicBounds;
import dev.dtzudontsay.knownworld.world.geography.MapCalibration;
import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.geography.MasterMapDefinition;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;

public final class MasterMapCalibration {

    private static final DistanceCalibrationAnchor PRIMARY_ANCHOR =
            CalibrationRegistry.primaryAnchor();

    public static final double METRES_PER_PIXEL =
            PRIMARY_ANCHOR.metresPerPixel();

    /*
     * The centre of the full master raster becomes world coordinate 0,0.
     */
    public static final MapCoordinate MAP_ORIGIN =
            new MapCoordinate(
                    MasterMapDefinition.IMAGE_WIDTH_PIXELS / 2.0,
                    MasterMapDefinition.IMAGE_HEIGHT_PIXELS / 2.0
            );

    public static final WorldCoordinate WORLD_ORIGIN =
            new WorldCoordinate(
                    0.0,
                    0.0
            );

    public static final MapCalibration CALIBRATION =
            new MapCalibration(
                    MAP_ORIGIN,
                    WORLD_ORIGIN,
                    METRES_PER_PIXEL,
                    0.0
            );

    private MasterMapCalibration() {
    }

    public static CalibrationStatus status() {
        return CalibrationStatus.PROVISIONAL;
    }

    public static double worldWidthMetres() {
        return MasterMapDefinition.IMAGE_WIDTH_PIXELS
                * METRES_PER_PIXEL;
    }

    public static double worldHeightMetres() {
        return MasterMapDefinition.IMAGE_HEIGHT_PIXELS
                * METRES_PER_PIXEL;
    }

    public static WorldCoordinate toWorld(
            MapCoordinate mapCoordinate
    ) {
        return CALIBRATION.toWorld(
                mapCoordinate
        );
    }

    public static GeographicBounds worldBounds() {
        double halfWidth =
                worldWidthMetres() / 2.0;

        double halfHeight =
                worldHeightMetres() / 2.0;

        return new GeographicBounds(
                -halfWidth,
                halfWidth,
                -halfHeight,
                halfHeight
        );
    }

    public static CalibrationResult result() {
        return new CalibrationResult(
                status(),
                PRIMARY_ANCHOR.id(),
                METRES_PER_PIXEL,
                worldWidthMetres(),
                worldHeightMetres()
        );
    }
}