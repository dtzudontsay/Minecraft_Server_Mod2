package dev.dtzudontsay.knownworld.world.geography.raster;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;

public record KnownWorldGeoSample(
        boolean insideKnownWorldMap,
        boolean land,
        double coastDistanceMetres,
        MapCoordinate logicalMapCoordinate,
        double rasterPixelX,
        double rasterPixelY,
        WorldCoordinate worldCoordinate
) {

    public boolean water() {
        return insideKnownWorldMap
                && !land;
    }

    public boolean nearCoast(
            double distanceMetres
    ) {
        return insideKnownWorldMap
                && Math.abs(
                coastDistanceMetres
        ) <= distanceMetres;
    }

    public boolean inlandByAtLeast(
            double distanceMetres
    ) {
        return insideKnownWorldMap
                && land
                && coastDistanceMetres
                >= distanceMetres;
    }

    public boolean offshoreByAtLeast(
            double distanceMetres
    ) {
        return insideKnownWorldMap
                && !land
                && coastDistanceMetres
                <= -distanceMetres;
    }
}