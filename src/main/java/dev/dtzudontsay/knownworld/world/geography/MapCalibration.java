package dev.dtzudontsay.knownworld.world.geography;

public record MapCalibration(
        MapCoordinate sourceOrigin,
        WorldCoordinate worldOrigin,
        double metresPerPixel,
        double rotationRadians
) {
    public MapCalibration {
        if (metresPerPixel <= 0.0) {
            throw new IllegalArgumentException("metresPerPixel must be > 0");
        }
    }

    public WorldCoordinate toWorld(MapCoordinate mapCoordinate) {
        double pixelEast =
                mapCoordinate.pixelX() - sourceOrigin.pixelX();

        double pixelNorth =
                -(mapCoordinate.pixelY() - sourceOrigin.pixelY());

        double east =
                pixelEast * metresPerPixel;

        double north =
                pixelNorth * metresPerPixel;

        double cos = Math.cos(rotationRadians);
        double sin = Math.sin(rotationRadians);

        double rotatedEast =
                east * cos - north * sin;

        double rotatedNorth =
                east * sin + north * cos;

        return new WorldCoordinate(
                worldOrigin.eastMetres() + rotatedEast,
                worldOrigin.northMetres() + rotatedNorth
        );
    }
}