package dev.dtzudontsay.knownworld.world.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoData;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSample;
import dev.dtzudontsay.knownworld.world.geography.raster.KnownWorldGeoSampler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public final class KnownWorldBiomeRasterData {

    private static final String CLIMATE_RESOURCE =
            "/assets/knownworld/geodata/climate.png";

    private static final String BIOME_OVERRIDE_RESOURCE =
            "/assets/knownworld/geodata/biome_override.png";

    private static final String BIOME_LEGEND_RESOURCE =
            "/assets/knownworld/geodata/biome_override_legend.json";


    private static final KnownWorldBiomeRasterData INSTANCE =
            load();


    private final int width;

    private final int height;

    private final byte[] climateCodes;

    private final byte[] biomeOverrideCodes;

    private final Map<Integer, String> biomeByCode;

    private final Set<String> overrideBiomeIds;


    private KnownWorldBiomeRasterData(
            int width,
            int height,
            byte[] climateCodes,
            byte[] biomeOverrideCodes,
            Map<Integer, String> biomeByCode
    ) {

        this.width =
                width;

        this.height =
                height;

        this.climateCodes =
                climateCodes;

        this.biomeOverrideCodes =
                biomeOverrideCodes;

        this.biomeByCode =
                Collections.unmodifiableMap(
                        new HashMap<>(
                                biomeByCode
                        )
                );

        this.overrideBiomeIds =
                Collections.unmodifiableSet(
                        new HashSet<>(
                                biomeByCode.values()
                        )
                );
    }


    public static KnownWorldBiomeRasterData getInstance() {

        return INSTANCE;
    }


    public Sample sample(
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

            return new Sample(
                    false,
                    0,
                    null
            );
        }


        int x =
                clamp(
                        (int) Math.round(
                                geography.rasterPixelX()
                        ),
                        0,
                        width - 1
                );

        int y =
                clamp(
                        (int) Math.round(
                                geography.rasterPixelY()
                        ),
                        0,
                        height - 1
                );


        int index =
                y
                        * width
                        + x;


        int climateCode =
                climateCodes[
                        index
                        ] & 0xFF;


        int overrideCode =
                biomeOverrideCodes[
                        index
                        ] & 0xFF;


        String overrideBiomeId =
                overrideCode == 0
                        ?
                        null
                        :
                        biomeByCode.get(
                                overrideCode
                        );


        return new Sample(
                true,
                climateCode,
                overrideBiomeId
        );
    }


    public Set<String> overrideBiomeIds() {

        return overrideBiomeIds;
    }


    private static KnownWorldBiomeRasterData load() {

        KnownWorld.LOGGER.info(
                "Loading Known World climate and biome override rasters..."
        );


        BufferedImage climate =
                readImage(
                        CLIMATE_RESOURCE
                );

        BufferedImage override =
                readImage(
                        BIOME_OVERRIDE_RESOURCE
                );


        KnownWorldGeoData geography =
                KnownWorldGeoData.getInstance();


        requireDimensions(
                "climate",
                climate,
                geography.width(),
                geography.height()
        );

        requireDimensions(
                "biome override",
                override,
                geography.width(),
                geography.height()
        );


        int width =
                geography.width();

        int height =
                geography.height();


        int pixelCount =
                Math.multiplyExact(
                        width,
                        height
                );


        byte[] climateCodes =
                new byte[
                        pixelCount
                        ];

        byte[] biomeOverrideCodes =
                new byte[
                        pixelCount
                        ];


        var climateRaster =
                climate.getRaster();

        var overrideRaster =
                override.getRaster();


        for (
                int y = 0;
                y < height;
                y++
        ) {

            int rowOffset =
                    y
                            * width;


            for (
                    int x = 0;
                    x < width;
                    x++
            ) {

                int index =
                        rowOffset
                                + x;


                climateCodes[
                        index
                        ] =
                        (byte) climateRaster.getSample(
                                x,
                                y,
                                0
                        );


                biomeOverrideCodes[
                        index
                        ] =
                        (byte) overrideRaster.getSample(
                                x,
                                y,
                                0
                        );
            }
        }


        Map<Integer, String> biomeByCode =
                loadBiomeLegend();


        KnownWorld.LOGGER.info(
                "Known World climate/biome data loaded: {}x{} px | {} biome override definitions",
                width,
                height,
                biomeByCode.size()
        );


        return new KnownWorldBiomeRasterData(
                width,
                height,
                climateCodes,
                biomeOverrideCodes,
                biomeByCode
        );
    }


    private static Map<Integer, String> loadBiomeLegend() {

        try (
                InputStream stream =
                        KnownWorldBiomeRasterData.class
                                .getResourceAsStream(
                                        BIOME_LEGEND_RESOURCE
                                )
        ) {

            if (
                    stream == null
            ) {

                throw new IllegalStateException(
                        "Missing Known World biome legend: "
                                + BIOME_LEGEND_RESOURCE
                );
            }


            try (
                    InputStreamReader reader =
                            new InputStreamReader(
                                    stream,
                                    StandardCharsets.UTF_8
                            )
            ) {

                JsonObject root =
                        JsonParser
                                .parseReader(
                                        reader
                                )
                                .getAsJsonObject();


                JsonArray biomes =
                        root.getAsJsonArray(
                                "biomes"
                        );


                Map<Integer, String> result =
                        new HashMap<>();


                for (
                        JsonElement element
                        :
                        biomes
                ) {

                    JsonObject entry =
                            element.getAsJsonObject();


                    int code =
                            entry.get(
                                    "code"
                            ).getAsInt();


                    String biome =
                            entry.get(
                                    "biome"
                            ).getAsString();


                    result.put(
                            code,
                            biome
                    );
                }


                return result;
            }

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to read Known World biome legend.",
                    exception
            );
        }
    }


    private static BufferedImage readImage(
            String resource
    ) {

        try (
                InputStream stream =
                        KnownWorldBiomeRasterData.class
                                .getResourceAsStream(
                                        resource
                                )
        ) {

            if (
                    stream == null
            ) {

                throw new IllegalStateException(
                        "Missing Known World runtime resource: "
                                + resource
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
                        "Unable to decode Known World runtime resource: "
                                + resource
                );
            }


            return image;

        } catch (
                IOException exception
        ) {

            throw new IllegalStateException(
                    "Failed to read Known World runtime resource: "
                            + resource,
                    exception
            );
        }
    }


    private static void requireDimensions(
            String label,
            BufferedImage image,
            int expectedWidth,
            int expectedHeight
    ) {

        if (
                image.getWidth()
                        != expectedWidth
                        ||
                        image.getHeight()
                                != expectedHeight
        ) {

            throw new IllegalStateException(
                    "Known World "
                            + label
                            + " raster has wrong dimensions. Expected "
                            + expectedWidth
                            + "x"
                            + expectedHeight
                            + " but got "
                            + image.getWidth()
                            + "x"
                            + image.getHeight()
            );
        }
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


    public record Sample(
            boolean land,
            int climateCode,
            String overrideBiomeId
    ) {
    }
}