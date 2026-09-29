package dev.dtzudontsay.knownworld.world.reference.spatial;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LandmassZoneDefinition {

    private final String locationId;

    private final int priority;

    private final List<List<MapCoordinate>> polygons;

    private final ReferenceProvenance provenance;

    private final String sourceNote;

    public LandmassZoneDefinition(
            String locationId,
            int priority,
            List<List<MapCoordinate>> polygons,
            ReferenceProvenance provenance,
            String sourceNote
    ) {
        if (locationId == null
                || locationId.isBlank()) {

            throw new IllegalArgumentException(
                    "locationId cannot be empty"
            );
        }

        this.locationId =
                locationId.trim()
                        .toLowerCase();

        this.priority =
                priority;

        Objects.requireNonNull(
                polygons,
                "polygons"
        );

        if (polygons.isEmpty()) {

            throw new IllegalArgumentException(
                    "Landmass zone requires at least one polygon"
            );
        }

        List<List<MapCoordinate>> copied =
                new ArrayList<>();

        for (
                List<MapCoordinate> polygon :
                polygons
        ) {

            if (polygon == null
                    || polygon.size() < 3) {

                throw new IllegalArgumentException(
                        "Every landmass polygon requires at least 3 points"
                );
            }

            copied.add(
                    List.copyOf(
                            polygon
                    )
            );
        }

        this.polygons =
                List.copyOf(
                        copied
                );

        this.provenance =
                Objects.requireNonNull(
                        provenance,
                        "provenance"
                );

        this.sourceNote =
                sourceNote == null
                        ? ""
                        : sourceNote.trim();
    }

    public String locationId() {
        return locationId;
    }

    public int priority() {
        return priority;
    }

    public List<List<MapCoordinate>> polygons() {
        return polygons;
    }

    public ReferenceProvenance provenance() {
        return provenance;
    }

    public String sourceNote() {
        return sourceNote;
    }

    public boolean contains(
            MapCoordinate point
    ) {
        for (
                List<MapCoordinate> polygon :
                polygons
        ) {

            if (containsPolygon(
                    polygon,
                    point
            )) {

                return true;
            }
        }

        return false;
    }

    public double totalAreaPixels() {

        double area =
                0.0;

        for (
                List<MapCoordinate> polygon :
                polygons
        ) {

            area +=
                    polygonArea(
                            polygon
                    );
        }

        return area;
    }

    private static boolean containsPolygon(
            List<MapCoordinate> polygon,
            MapCoordinate point
    ) {
        boolean inside =
                false;

        int size =
                polygon.size();

        for (
                int current = 0,
                previous = size - 1;
                current < size;
                previous = current++
        ) {

            MapCoordinate a =
                    polygon.get(
                            current
                    );

            MapCoordinate b =
                    polygon.get(
                            previous
                    );

            boolean crosses =
                    (
                            a.pixelY()
                                    > point.pixelY()
                    )
                            !=
                            (
                                    b.pixelY()
                                            > point.pixelY()
                            );

            if (!crosses) {
                continue;
            }

            double intersectionX =
                    (
                            b.pixelX()
                                    - a.pixelX()
                    )
                            * (
                            point.pixelY()
                                    - a.pixelY()
                    )
                            / (
                            b.pixelY()
                                    - a.pixelY()
                    )
                            + a.pixelX();

            if (point.pixelX()
                    < intersectionX) {

                inside =
                        !inside;
            }
        }

        return inside;
    }

    private static double polygonArea(
            List<MapCoordinate> polygon
    ) {
        double area =
                0.0;

        for (
                int index = 0;
                index < polygon.size();
                index++
        ) {

            MapCoordinate current =
                    polygon.get(
                            index
                    );

            MapCoordinate next =
                    polygon.get(
                            (
                                    index + 1
                            )
                                    % polygon.size()
                    );

            area +=
                    current.pixelX()
                            * next.pixelY()
                            - next.pixelX()
                            * current.pixelY();
        }

        return Math.abs(
                area
        )
                / 2.0;
    }
}