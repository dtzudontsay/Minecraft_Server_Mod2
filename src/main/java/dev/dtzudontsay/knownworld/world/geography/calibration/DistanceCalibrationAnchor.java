package dev.dtzudontsay.knownworld.world.geography.calibration;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;

public record DistanceCalibrationAnchor(
        String id,
        String displayName,
        MapCoordinate start,
        MapCoordinate end,
        double canonicalDistanceMetres,
        String sourceNote
) {

    public DistanceCalibrationAnchor {
        if (canonicalDistanceMetres <= 0.0) {
            throw new IllegalArgumentException(
                    "canonicalDistanceMetres must be > 0"
            );
        }
    }

    public double pixelDistance() {
        double deltaX =
                end.pixelX() - start.pixelX();

        double deltaY =
                end.pixelY() - start.pixelY();

        return Math.hypot(
                deltaX,
                deltaY
        );
    }

    public double metresPerPixel() {
        return canonicalDistanceMetres / pixelDistance();
    }
}