package dev.dtzudontsay.knownworld.world.geography.raster;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.geography.MasterMapDefinition;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.geography.calibration.MasterMapCalibration;

public final class KnownWorldGeoSampler {

    private static final KnownWorldGeoData DATA =
            KnownWorldGeoData.getInstance();

    private KnownWorldGeoSampler() {
    }

    /*
     * --------------------------------------------------------
     * Public Minecraft-coordinate API
     * --------------------------------------------------------
     */

    public static KnownWorldGeoSample sampleMinecraft(
            double minecraftX,
            double minecraftZ
    ) {
        WorldCoordinate worldCoordinate =
                WorldProjection.fromMinecraft(
                        minecraftX,
                        minecraftZ
                );

        return sampleWorld(
                worldCoordinate
        );
    }

    public static boolean isLand(
            double minecraftX,
            double minecraftZ
    ) {
        KnownWorldGeoSample sample =
                sampleMinecraft(
                        minecraftX,
                        minecraftZ
                );

        return sample.insideKnownWorldMap()
                && sample.land();
    }

    public static boolean isWater(
            double minecraftX,
            double minecraftZ
    ) {
        KnownWorldGeoSample sample =
                sampleMinecraft(
                        minecraftX,
                        minecraftZ
                );

        return sample.water();
    }

    public static boolean isInsideKnownWorld(
            double minecraftX,
            double minecraftZ
    ) {
        return sampleMinecraft(
                minecraftX,
                minecraftZ
        ).insideKnownWorldMap();
    }

    public static double distanceToCoastMetres(
            double minecraftX,
            double minecraftZ
    ) {
        return sampleMinecraft(
                minecraftX,
                minecraftZ
        ).coastDistanceMetres();
    }

    public static boolean isNearCoast(
            double minecraftX,
            double minecraftZ,
            double distanceMetres
    ) {
        return sampleMinecraft(
                minecraftX,
                minecraftZ
        ).nearCoast(
                distanceMetres
        );
    }

    /*
     * --------------------------------------------------------
     * Canonical-world API
     * --------------------------------------------------------
     */

    public static KnownWorldGeoSample sampleWorld(
            WorldCoordinate worldCoordinate
    ) {
        MapCoordinate logicalCoordinate =
                worldToLogicalMap(
                        worldCoordinate
                );

        double logicalX =
                logicalCoordinate.pixelX();

        double logicalY =
                logicalCoordinate.pixelY();

        boolean inside =
                logicalX >= 0.0
                        && logicalY >= 0.0
                        && logicalX
                        < MasterMapDefinition.IMAGE_WIDTH_PIXELS
                        && logicalY
                        < MasterMapDefinition.IMAGE_HEIGHT_PIXELS;

        double rasterX =
                logicalX
                        / MasterMapDefinition.IMAGE_WIDTH_PIXELS
                        * DATA.width();

        double rasterY =
                logicalY
                        / MasterMapDefinition.IMAGE_HEIGHT_PIXELS
                        * DATA.height();

        if (
                !inside
        ) {
            return new KnownWorldGeoSample(
                    false,
                    false,
                    Double.NaN,
                    logicalCoordinate,
                    rasterX,
                    rasterY,
                    worldCoordinate
            );
        }

        int pixelX =
                clamp(
                        (int) Math.floor(
                                rasterX
                        ),
                        0,
                        DATA.width() - 1
                );

        int pixelY =
                clamp(
                        (int) Math.floor(
                                rasterY
                        ),
                        0,
                        DATA.height() - 1
                );

        boolean land =
                DATA.isLandPixel(
                        pixelX,
                        pixelY
                );

        int signedDistancePixels =
                DATA.signedCoastDistancePixels(
                        pixelX,
                        pixelY
                );

        double metresPerRasterPixelX =
                MasterMapCalibration.METRES_PER_PIXEL
                        * MasterMapDefinition.IMAGE_WIDTH_PIXELS
                        / DATA.width();

        double metresPerRasterPixelY =
                MasterMapCalibration.METRES_PER_PIXEL
                        * MasterMapDefinition.IMAGE_HEIGHT_PIXELS
                        / DATA.height();

        /*
         * X/Y raster scale differs very slightly because the
         * full-resolution source is 9050x6000 while the logical
         * calibration raster is 2048x1357.
         *
         * Use the mean scale for scalar coast distance.
         */
        double metresPerRasterPixel =
                (
                        metresPerRasterPixelX
                                + metresPerRasterPixelY
                ) / 2.0;

        /*
         * scipy.ndimage.distance_transform_edt gives the first
         * land/water pixel adjacent to a boundary a distance of
         * approximately 1 pixel.
         *
         * The actual coastline lies between the two raster
         * cells, so subtract half a pixel from the magnitude.
         */
        double correctedPixelDistance;

        if (
                signedDistancePixels > 0
        ) {
            correctedPixelDistance =
                    Math.max(
                            0.0,
                            signedDistancePixels
                                    - 0.5
                    );

        } else if (
                signedDistancePixels < 0
        ) {
            correctedPixelDistance =
                    Math.min(
                            0.0,
                            signedDistancePixels
                                    + 0.5
                    );

        } else {
            correctedPixelDistance =
                    0.0;
        }

        double coastDistanceMetres =
                correctedPixelDistance
                        * metresPerRasterPixel;

        return new KnownWorldGeoSample(
                true,
                land,
                coastDistanceMetres,
                logicalCoordinate,
                rasterX,
                rasterY,
                worldCoordinate
        );
    }

    /*
     * Inverse of the current MasterMapCalibration mapping.
     *
     * Master map convention:
     *
     *     +X image = east
     *     +Y image = south
     *
     * Canonical world:
     *
     *     +east  = east
     *     +north = north
     */
    public static MapCoordinate worldToLogicalMap(
            WorldCoordinate coordinate
    ) {
        double eastRelative =
                coordinate.eastMetres()
                        - MasterMapCalibration
                        .WORLD_ORIGIN
                        .eastMetres();

        double northRelative =
                coordinate.northMetres()
                        - MasterMapCalibration
                        .WORLD_ORIGIN
                        .northMetres();

        double rotation =
                MasterMapCalibration
                        .CALIBRATION
                        .rotationRadians();

        double cos =
                Math.cos(
                        rotation
                );

        double sin =
                Math.sin(
                        rotation
                );

        /*
         * Undo the calibration rotation.
         */
        double unrotatedEast =
                eastRelative * cos
                        + northRelative * sin;

        double unrotatedNorth =
                -eastRelative * sin
                        + northRelative * cos;

        double logicalX =
                MasterMapCalibration
                        .MAP_ORIGIN
                        .pixelX()
                        + unrotatedEast
                        / MasterMapCalibration.METRES_PER_PIXEL;

        double logicalY =
                MasterMapCalibration
                        .MAP_ORIGIN
                        .pixelY()
                        - unrotatedNorth
                        / MasterMapCalibration.METRES_PER_PIXEL;

        return new MapCoordinate(
                logicalX,
                logicalY
        );
    }

    public static double metresPerRasterPixelX() {
        return MasterMapCalibration.METRES_PER_PIXEL
                * MasterMapDefinition.IMAGE_WIDTH_PIXELS
                / DATA.width();
    }

    public static double metresPerRasterPixelY() {
        return MasterMapCalibration.METRES_PER_PIXEL
                * MasterMapDefinition.IMAGE_HEIGHT_PIXELS
                / DATA.height();
    }

    public static int rasterWidth() {
        return DATA.width();
    }

    public static int rasterHeight() {
        return DATA.height();
    }

    private static int clamp(
            int value,
            int minimum,
            int maximum
    ) {
        return Math.max(
                minimum,
                Math.min(
                        maximum,
                        value
                )
        );
    }
}