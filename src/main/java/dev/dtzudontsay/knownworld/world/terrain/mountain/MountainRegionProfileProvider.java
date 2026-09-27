package dev.dtzudontsay.knownworld.world.terrain.mountain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dtzudontsay.knownworld.KnownWorld;
import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.calibration.MasterMapCalibration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MountainRegionProfileProvider {

    private static final String RESOURCE =
            "/assets/knownworld/geodata/mountain_regions.json";

    private final double defaultMaxSummitY;

    private final List<Region> regions;


    public MountainRegionProfileProvider() {

        JsonObject root =
                loadJson();

        defaultMaxSummitY =
                root.get(
                        "defaultMaxSummitY"
                ).getAsDouble();

        regions =
                new ArrayList<>();

        JsonArray regionArray =
                root.getAsJsonArray(
                        "regions"
                );

        for (
                JsonElement element
                :
                regionArray
        ) {

            JsonObject object =
                    element.getAsJsonObject();

            String id =
                    object.get(
                            "id"
                    ).getAsString();

            double centerX =
                    object.get(
                            "centerX"
                    ).getAsDouble();

            double centerY =
                    object.get(
                            "centerY"
                    ).getAsDouble();

            double radiusX =
                    object.get(
                            "radiusX"
                    ).getAsDouble();

            double radiusY =
                    object.get(
                            "radiusY"
                    ).getAsDouble();

            double maxSummitY =
                    object.get(
                            "maxSummitY"
                    ).getAsDouble();

            MountainStyle style =
                    MountainStyle.NORMAL;

            if (
                    object.has(
                            "style"
                    )
            ) {

                style =
                        MountainStyle.valueOf(
                                object.get(
                                                "style"
                                        )
                                        .getAsString()
                                        .toUpperCase(
                                                Locale.ROOT
                                        )
                        );
            }

            double singularMountainRadiusMetres =
                    object.has(
                            "singularMountainRadiusMetres"
                    )
                            ?
                            object.get(
                                            "singularMountainRadiusMetres"
                                    )
                                    .getAsDouble()
                            :
                            0.0;

            WorldCoordinate centreWorld =
                    MasterMapCalibration.toWorld(
                            new MapCoordinate(
                                    centerX,
                                    centerY
                            )
                    );

            regions.add(
                    new Region(
                            id,
                            centerX,
                            centerY,
                            radiusX,
                            radiusY,
                            maxSummitY,
                            style,
                            centreWorld.eastMetres(),
                            centreWorld.northMetres(),
                            singularMountainRadiusMetres
                    )
            );
        }

        KnownWorld.LOGGER.info(
                "Loaded {} Known World mountain-height regions",
                regions.size()
        );
    }


    public MountainProfileSample sample(
            MapCoordinate coordinate
    ) {

        /*
         * Special regions always win.
         *
         * This matters for the Mother of Mountains, which overlaps
         * geographically with the wider eastern mountain systems.
         */
        for (
                Region region
                :
                regions
        ) {

            if (
                    region.style()
                            == MountainStyle.SINGULAR
                            &&
                            region.contains(
                                    coordinate
                            )
            ) {

                return region.toSample(
                        true
                );
            }
        }


        double weightedCeiling =
                0.0;

        double totalWeight =
                0.0;

        Region strongestRegion =
                null;

        double strongestWeight =
                -1.0;


        for (
                Region region
                :
                regions
        ) {

            if (
                    region.style()
                            != MountainStyle.NORMAL
            ) {
                continue;
            }

            double normalizedDistance =
                    region.normalizedDistance(
                            coordinate
                    );

            if (
                    normalizedDistance > 1.0
            ) {
                continue;
            }

            /*
             * The weight is only used where two profile ellipses
             * overlap.
             *
             * A lone region still receives its exact configured
             * ceiling everywhere inside it.
             */
            double weight =
                    0.20
                            +
                            (
                                    1.0
                                            - normalizedDistance
                            )
                                    * 0.80;

            weightedCeiling +=
                    region.maxSummitY()
                            * weight;

            totalWeight +=
                    weight;

            if (
                    weight
                            > strongestWeight
            ) {

                strongestWeight =
                        weight;

                strongestRegion =
                        region;
            }
        }


        if (
                totalWeight <= 0.0
        ) {

            return new MountainProfileSample(
                    "default",
                    defaultMaxSummitY,
                    MountainStyle.NORMAL,
                    false,
                    0.0,
                    0.0,
                    0.0
            );
        }


        return new MountainProfileSample(
                strongestRegion != null
                        ?
                        strongestRegion.id()
                        :
                        "blend",
                weightedCeiling
                        / totalWeight,
                MountainStyle.NORMAL,
                true,
                0.0,
                0.0,
                0.0
        );
    }


    private JsonObject loadJson() {

        try (
                InputStream stream =
                        MountainRegionProfileProvider.class
                                .getResourceAsStream(
                                        RESOURCE
                                )
        ) {

            if (
                    stream == null
            ) {

                throw new IllegalStateException(
                        "Missing mountain-region profile resource: "
                                + RESOURCE
                );
            }

            try (
                    InputStreamReader reader =
                            new InputStreamReader(
                                    stream,
                                    StandardCharsets.UTF_8
                            )
            ) {

                return JsonParser
                        .parseReader(
                                reader
                        )
                        .getAsJsonObject();
            }

        } catch (
                Exception exception
        ) {

            throw new IllegalStateException(
                    "Failed to load mountain-region profiles: "
                            + RESOURCE,
                    exception
            );
        }
    }


    public enum MountainStyle {

        NORMAL,

        SINGULAR
    }


    public record MountainProfileSample(
            String regionId,
            double maxSummitY,
            MountainStyle style,
            boolean explicitRegion,
            double singularCenterEastMetres,
            double singularCenterNorthMetres,
            double singularMountainRadiusMetres
    ) {
    }


    private record Region(
            String id,
            double centerX,
            double centerY,
            double radiusX,
            double radiusY,
            double maxSummitY,
            MountainStyle style,
            double centreEastMetres,
            double centreNorthMetres,
            double singularMountainRadiusMetres
    ) {

        double normalizedDistance(
                MapCoordinate coordinate
        ) {

            double dx =
                    (
                            coordinate.pixelX()
                                    - centerX
                    )
                            / radiusX;

            double dy =
                    (
                            coordinate.pixelY()
                                    - centerY
                    )
                            / radiusY;

            return Math.sqrt(
                    dx * dx
                            +
                            dy * dy
            );
        }


        boolean contains(
                MapCoordinate coordinate
        ) {

            return normalizedDistance(
                    coordinate
            ) <= 1.0;
        }


        MountainProfileSample toSample(
                boolean explicitRegion
        ) {

            return new MountainProfileSample(
                    id,
                    maxSummitY,
                    style,
                    explicitRegion,
                    centreEastMetres,
                    centreNorthMetres,
                    singularMountainRadiusMetres
            );
        }
    }
}