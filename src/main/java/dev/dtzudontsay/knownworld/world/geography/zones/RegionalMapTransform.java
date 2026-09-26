package dev.dtzudontsay.knownworld.world.geography.zones;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;

public record RegionalMapTransform(
        double scaleXX,
        double scaleXY,
        double translateX,
        double scaleYX,
        double scaleYY,
        double translateY
) {

    public MapCoordinate toMaster(
            MapCoordinate regionalCoordinate
    ) {
        double x =
                regionalCoordinate.pixelX();

        double y =
                regionalCoordinate.pixelY();

        return new MapCoordinate(
                scaleXX * x
                        + scaleXY * y
                        + translateX,

                scaleYX * x
                        + scaleYY * y
                        + translateY
        );
    }

    public MapCoordinate toRegional(
            MapCoordinate masterCoordinate
    ) {
        double determinant =
                scaleXX * scaleYY
                        - scaleXY * scaleYX;

        if (Math.abs(determinant) < 1.0e-12) {
            throw new IllegalStateException(
                    "Regional map transform is not invertible."
            );
        }

        double translatedX =
                masterCoordinate.pixelX()
                        - translateX;

        double translatedY =
                masterCoordinate.pixelY()
                        - translateY;

        double regionalX =
                (
                        scaleYY * translatedX
                                - scaleXY * translatedY
                ) / determinant;

        double regionalY =
                (
                        -scaleYX * translatedX
                                + scaleXX * translatedY
                ) / determinant;

        return new MapCoordinate(
                regionalX,
                regionalY
        );
    }
}