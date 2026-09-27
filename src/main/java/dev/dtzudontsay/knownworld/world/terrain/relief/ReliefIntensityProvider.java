package dev.dtzudontsay.knownworld.world.terrain.relief;

import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoData;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public final class ReliefIntensityProvider {

    private static final String RESOURCE =
            "/assets/knownworld/geodata/relief_intensity.png";

    private final int width;
    private final int height;

    private final byte[] values;

    public ReliefIntensityProvider() {

        BufferedImage image =
                loadImage();

        KnownWorldGeoData geography =
                KnownWorldGeoData.getInstance();

        if (
                image.getWidth()
                        != geography.width()
                        ||
                        image.getHeight()
                                != geography.height()
        ) {
            throw new IllegalStateException(
                    "Known World relief intensity raster "
                            + "does not match geography raster. "
                            + "Expected "
                            + geography.width()
                            + "x"
                            + geography.height()
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

        values =
                new byte[
                        width * height
                        ];

        var raster =
                image.getRaster();

        for (
                int y = 0;
                y < height;
                y++
        ) {

            int rowOffset =
                    y * width;

            for (
                    int x = 0;
                    x < width;
                    x++
            ) {

                values[
                        rowOffset + x
                        ] =
                        (byte) raster.getSample(
                                x,
                                y,
                                0
                        );
            }
        }

        KnownWorld.LOGGER.info(
                "Loaded Known World runtime relief intensity: {}x{} px",
                width,
                height
        );
    }

    public double sample(
            WorldCoordinate coordinate
    ) {

        KnownWorldGeoSample geography =
                KnownWorldGeoSampler.sampleWorld(
                        coordinate
                );

        if (
                !geography.insideKnownWorldMap()
                        ||
                        !geography.land()
        ) {
            return 0.0;
        }

        double rasterX =
                geography.rasterPixelX();

        double rasterY =
                geography.rasterPixelY();

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

        double north =
                lerp(
                        valueAt(
                                x0,
                                y0
                        ),
                        valueAt(
                                x1,
                                y0
                        ),
                        tx
                );

        double south =
                lerp(
                        valueAt(
                                x0,
                                y1
                        ),
                        valueAt(
                                x1,
                                y1
                        ),
                        tx
                );

        return lerp(
                north,
                south,
                ty
        );
    }

    private double valueAt(
            int x,
            int y
    ) {

        return (
                values[
                        y * width + x
                        ] & 0xFF
        ) / 255.0;
    }

    private BufferedImage loadImage() {

        try (
                InputStream stream =
                        ReliefIntensityProvider.class
                                .getResourceAsStream(
                                        RESOURCE
                                )
        ) {

            if (
                    stream == null
            ) {
                throw new IllegalStateException(
                        "Missing Known World runtime relief raster: "
                                + RESOURCE
                );
            }

            BufferedImage image =
                    ImageIO.read(
                            stream
                    );

            if (
                    image == null
            ) {
                throw new IllegalStateException(
                        "Unable to decode Known World relief raster: "
                                + RESOURCE
                );
            }

            return image;

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to load Known World relief raster: "
                            + RESOURCE,
                    exception
            );
        }
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
}