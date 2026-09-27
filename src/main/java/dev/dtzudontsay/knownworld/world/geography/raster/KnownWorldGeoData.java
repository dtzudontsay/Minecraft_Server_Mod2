package dev.dtzudontsay.knownworld.world.geography.raster;

import dev.dtzudontsay.knownworld.KnownWorld;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

public final class KnownWorldGeoData {

    private static final String LAND_MASK_RESOURCE =
            "/assets/knownworld/geodata/land_mask.png";

    private static final String COAST_DISTANCE_RESOURCE =
            "/assets/knownworld/geodata/coast_distance.png";

    private static final KnownWorldGeoData INSTANCE =
            load();

    private final int width;
    private final int height;

    /*
     * Land is stored as one bit per raster pixel.
     *
     * At 9050 x 6000 this is only about 6.5 MB,
     * instead of keeping another 54 MB byte array.
     */
    private final BitSet landBits;

    /*
     * Raw encoded coast-distance values.
     *
     * Java byte is signed, so always decode with:
     *
     *     value & 0xFF
     */
    private final byte[] coastDistance;

    private KnownWorldGeoData(
            int width,
            int height,
            BitSet landBits,
            byte[] coastDistance
    ) {
        this.width = width;
        this.height = height;
        this.landBits = landBits;
        this.coastDistance = coastDistance;
    }

    public static KnownWorldGeoData getInstance() {
        return INSTANCE;
    }

    private static KnownWorldGeoData load() {
        KnownWorld.LOGGER.info(
                "Loading Known World runtime geography..."
        );

        BufferedImage landImage =
                readImage(
                        LAND_MASK_RESOURCE
                );

        BufferedImage coastImage =
                readImage(
                        COAST_DISTANCE_RESOURCE
                );

        if (
                landImage.getWidth()
                        != coastImage.getWidth()
                        ||
                        landImage.getHeight()
                                != coastImage.getHeight()
        ) {
            throw new IllegalStateException(
                    "Known World geodata dimensions do not match. "
                            + "Land mask is "
                            + landImage.getWidth()
                            + "x"
                            + landImage.getHeight()
                            + ", coast distance is "
                            + coastImage.getWidth()
                            + "x"
                            + coastImage.getHeight()
            );
        }

        int width =
                landImage.getWidth();

        int height =
                landImage.getHeight();

        long pixelCountLong =
                (long) width
                        * height;

        if (
                pixelCountLong
                        > Integer.MAX_VALUE
        ) {
            throw new IllegalStateException(
                    "Known World geodata raster is too large "
                            + "for the current runtime representation: "
                            + width
                            + "x"
                            + height
            );
        }

        int pixelCount =
                (int) pixelCountLong;

        BitSet landBits =
                new BitSet(
                        pixelCount
                );

        byte[] coastDistance =
                new byte[
                        pixelCount
                        ];

        var landRaster =
                landImage.getRaster();

        var coastRaster =
                coastImage.getRaster();

        int landCount = 0;

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
                int index =
                        rowOffset + x;

                int landValue =
                        landRaster.getSample(
                                x,
                                y,
                                0
                        );

                if (
                        landValue
                                >= 128
                ) {
                    landBits.set(
                            index
                    );

                    landCount++;
                }

                int coastValue =
                        coastRaster.getSample(
                                x,
                                y,
                                0
                        );

                coastDistance[
                        index
                        ] = (byte) coastValue;
            }
        }

        KnownWorld.LOGGER.info(
                "Known World runtime geography loaded: {}x{} px | {} land pixels | {} water pixels",
                width,
                height,
                landCount,
                pixelCount - landCount
        );

        return new KnownWorldGeoData(
                width,
                height,
                landBits,
                coastDistance
        );
    }

    private static BufferedImage readImage(
            String resourcePath
    ) {
        try (
                InputStream stream =
                        KnownWorldGeoData.class
                                .getResourceAsStream(
                                        resourcePath
                                )
        ) {
            if (
                    stream == null
            ) {
                throw new IllegalStateException(
                        "Missing Known World runtime resource: "
                                + resourcePath
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
                        "Could not decode Known World runtime resource: "
                                + resourcePath
                );
            }

            return image;

        } catch (
                IOException exception
        ) {
            throw new IllegalStateException(
                    "Failed to read Known World runtime resource: "
                            + resourcePath,
                    exception
            );
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public long pixelCount() {
        return (long) width
                * height;
    }

    public boolean containsPixel(
            int x,
            int y
    ) {
        return x >= 0
                && y >= 0
                && x < width
                && y < height;
    }

    public boolean isLandPixel(
            int x,
            int y
    ) {
        requirePixel(
                x,
                y
        );

        return landBits.get(
                index(
                        x,
                        y
                )
        );
    }

    public int encodedCoastDistance(
            int x,
            int y
    ) {
        requirePixel(
                x,
                y
        );

        return coastDistance[
                index(
                        x,
                        y
                )
                ] & 0xFF;
    }

    /*
     * Decode the Python representation:
     *
     *     encoded = signedPixelDistance + 128
     *
     * Returns approximately:
     *
     *     negative = offshore
     *     positive = inland
     */
    public int signedCoastDistancePixels(
            int x,
            int y
    ) {
        return encodedCoastDistance(
                x,
                y
        ) - 128;
    }

    private int index(
            int x,
            int y
    ) {
        return y
                * width
                + x;
    }

    private void requirePixel(
            int x,
            int y
    ) {
        if (
                !containsPixel(
                        x,
                        y
                )
        ) {
            throw new IndexOutOfBoundsException(
                    "Geodata pixel outside raster: "
                            + x
                            + ","
                            + y
                            + " for "
                            + width
                            + "x"
                            + height
            );
        }
    }
}