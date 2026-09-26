package dev.dtzudontsay.knownworld.world.geography;

public record GeographicBounds(
        double minEastMetres,
        double maxEastMetres,
        double minNorthMetres,
        double maxNorthMetres
) {
    public GeographicBounds {
        if (maxEastMetres <= minEastMetres) {
            throw new IllegalArgumentException(
                    "maxEastMetres must be greater than minEastMetres"
            );
        }

        if (maxNorthMetres <= minNorthMetres) {
            throw new IllegalArgumentException(
                    "maxNorthMetres must be greater than minNorthMetres"
            );
        }
    }

    public double widthMetres() {
        return maxEastMetres - minEastMetres;
    }

    public double heightMetres() {
        return maxNorthMetres - minNorthMetres;
    }

    public boolean contains(WorldCoordinate coordinate) {
        return coordinate.eastMetres() >= minEastMetres
                && coordinate.eastMetres() <= maxEastMetres
                && coordinate.northMetres() >= minNorthMetres
                && coordinate.northMetres() <= maxNorthMetres;
    }
}