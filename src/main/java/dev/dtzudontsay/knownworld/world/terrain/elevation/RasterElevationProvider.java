package dev.dtzudontsay.knownworld.world.terrain.elevation;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.calibration.MasterMapCalibration;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoData;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public final class RasterElevationProvider implements ElevationProvider {

    private static final String ELEVATION_RESOURCE =
            "/assets/knownworld/geodata/elevation.png";

    /*
     * elevation.png encoding:
     *
     * 16-bit grayscale PNG
     *
     * value 0     = reserved / no-data
     * value 1     = -64 metres
     * value 65535 = +2048 metres
     *
     * We deliberately reserve zero so that a future partially
     * populated elevation raster can distinguish "unknown" from
     * a genuine low elevation.
     *
     * These values are storage limits, not statements about the
     * real Known World.
     */
    private static final double MIN_STORED_ELEVATION_METRES =
            -64.0;

    private static final double MAX_STORED_ELEVATION_METRES =
            2048.0;

    private static final int NO_DATA_VALUE =
            0;

    private final KnownWorldGeoData geographyData;

    private final boolean available;

    private final int width;

    private final int height;

    private final int[] elevationValues;

    public RasterElevationProvider() {

        geographyData =
                KnownWorldGeoData.getInstance();

        BufferedImage image =
                loadElevationImage();

        if (
                image == null
        ) {

            available =
                    false;

            width =
                    0;

            height =
                    0;

            elevationValues =
                    new int[0];

            KnownWorld.LOGGER.info(
                    "No canonical elevation raster found yet. "
                            + "Terrain will continue using the temporary coast-distance elevation model."
            );

            return;
        }

        if (
                image.getWidth()
                        != geographyData.width()
                        ||
                        image.getHeight()
                                != geographyData.height()
        ) {
            throw new IllegalStateException(
                    "Known World elevation raster dimensions do not match geography raster. "
                            + "Expected "
                            + geographyData.width()
                            + "x"
                            + geographyData.height()
                            + " but got "
                            + image.getWidth()
                            + "x"
                            + image.getHeight()
            );
        }

        width =
                image.getWidth();

        height =
                image.getHeight();

        elevationValues =
                new int[
                        width
                                * height
                        ];

        for (
                int y = 0;
                y < height;
                y++
        ) {

            for (
                    int x = 0;
                    x < width;
                    x++
            ) {

                int value =
                        image
                                .getRaster()
                                .getSample(
                                        x,
                                        y,
                                        0
                                );

                elevationValues[
                        y * width + x
                        ] =
                        value;
            }
        }

        available =
                true;

        KnownWorld.LOGGER.info(
                "Loaded Known World elevation raster: {}x{} px",
                width,
                height
        );
    }

    @Override
    public ElevationSample sample(
            WorldCoordinate coordinate
    ) {

        if (
                !available
        ) {
            return noData();
        }

        KnownWorldGeoSample geography =
                KnownWorldGeoSampler.sampleWorld(
                        coordinate
                );

        if (
                !geography.insideKnownWorldMap()
        ) {
            return noData();
        }

        double rasterX =
                geography.rasterPixelX();

        double rasterY =
                geography.rasterPixelY();

        /*
         * Bilinear sampling prevents the elevation raster from
         * producing visible ~1.36 km stair-step cells.
         */
        int x0 =
                clamp(
                        (int) Math.floor(
                                rasterX
                        ),
                        0,
                        width - 1
                );

        int y0 =
                clamp(
                        (int) Math.floor(
                                rasterY
                        ),
                        0,
                        height - 1
                );

        int x1 =
                Math.min(
                        x0 + 1,
                        width - 1
                );

        int y1 =
                Math.min(
                        y0 + 1,
                        height - 1
                );

        double tx =
                clamp01(
                        rasterX - x0
                );

        double ty =
                clamp01(
                        rasterY - y0
                );

        int raw00 =
                valueAt(
                        x0,
                        y0
                );

        int raw10 =
                valueAt(
                        x1,
                        y0
                );

        int raw01 =
                valueAt(
                        x0,
                        y1
                );

        int raw11 =
                valueAt(
                        x1,
                        y1
                );

        /*
         * If any corner is no-data, do not fabricate the missing
         * elevation through interpolation.
         */
        if (
                raw00 == NO_DATA_VALUE
                        ||
                        raw10 == NO_DATA_VALUE
                        ||
                        raw01 == NO_DATA_VALUE
                        ||
                        raw11 == NO_DATA_VALUE
        ) {
            return noData();
        }

        double elevation00 =
                decodeElevation(
                        raw00
                );

        double elevation10 =
                decodeElevation(
                        raw10
                );

        double elevation01 =
                decodeElevation(
                        raw01
                );

        double elevation11 =
                decodeElevation(
                        raw11
                );

        double north =
                lerp(
                        elevation00,
                        elevation10,
                        tx
                );

        double south =
                lerp(
                        elevation01,
                        elevation11,
                        tx
                );

        double elevation =
                lerp(
                        north,
                        south,
                        ty
                );

        return new ElevationSample(
                elevation,
                "CANONICAL_ELEVATION_RASTER"
        );
    }

    @Override
    public boolean hasCanonicalElevationData() {
        return available;
    }

    private BufferedImage loadElevationImage() {

        try (
                InputStream stream =
                        RasterElevationProvider.class
                                .getResourceAsStream(
                                        ELEVATION_RESOURCE
                                )
        ) {

            if (
                    stream == null
            ) {
                return null;
            }

            BufferedImage image =
                    ImageIO.read(
                            stream
                    );

            if (
                    image == null
            ) {
                throw new IllegalStateException(
                        "Unable to decode Known World elevation raster: "
                                + ELEVATION_RESOURCE
                );
            }

            return image;

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to load Known World elevation raster: "
                            + ELEVATION_RESOURCE,
                    exception
            );
        }
    }

    private int valueAt(
            int x,
            int y
    ) {

        return elevationValues[
                y * width + x
                ];
    }

    private static double decodeElevation(
            int raw
    ) {

        double factor =
                (
                        raw - 1.0
                )
                        / 65534.0;

        return lerp(
                MIN_STORED_ELEVATION_METRES,
                MAX_STORED_ELEVATION_METRES,
                factor
        );
    }

    private static ElevationSample noData() {

        return new ElevationSample(
                Double.NaN,
                "NO_CANONICAL_ELEVATION_DATA"
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