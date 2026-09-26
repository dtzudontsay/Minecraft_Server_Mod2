package dev.dtzudontsay.knownworld.world.geography.zones;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;

public record MapZoneDefinition(
        MapZoneId id,
        String sourceName,
        int regionalWidthPixels,
        int regionalHeightPixels,
        RegionalMapTransform transform
) {

    public MapZoneDefinition {
        if (regionalWidthPixels <= 0) {
            throw new IllegalArgumentException(
                    "regionalWidthPixels must be > 0"
            );
        }

        if (regionalHeightPixels <= 0) {
            throw new IllegalArgumentException(
                    "regionalHeightPixels must be > 0"
            );
        }
    }

    public boolean containsRegionalPixel(
            MapCoordinate coordinate
    ) {
        return coordinate.pixelX() >= 0.0
                && coordinate.pixelX() <= regionalWidthPixels
                && coordinate.pixelY() >= 0.0
                && coordinate.pixelY() <= regionalHeightPixels;
    }

    public MapCoordinate toMaster(
            MapCoordinate regionalCoordinate
    ) {
        if (!containsRegionalPixel(regionalCoordinate)) {
            throw new IllegalArgumentException(
                    "Regional coordinate lies outside zone image: "
                            + regionalCoordinate
            );
        }

        return transform.toMaster(
                regionalCoordinate
        );
    }
}