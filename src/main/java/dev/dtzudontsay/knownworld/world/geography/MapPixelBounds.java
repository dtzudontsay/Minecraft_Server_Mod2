package dev.dtzudontsay.knownworld.world.geography;

public record MapPixelBounds(
        int minX,
        int minY,
        int maxX,
        int maxY
) {
    public MapPixelBounds {
        if (maxX <= minX) {
            throw new IllegalArgumentException(
                    "maxX must be greater than minX"
            );
        }

        if (maxY <= minY) {
            throw new IllegalArgumentException(
                    "maxY must be greater than minY"
            );
        }
    }

    public int widthPixels() {
        return maxX - minX;
    }

    public int heightPixels() {
        return maxY - minY;
    }

    public boolean contains(MapCoordinate coordinate) {
        return coordinate.pixelX() >= minX
                && coordinate.pixelX() <= maxX
                && coordinate.pixelY() >= minY
                && coordinate.pixelY() <= maxY;
    }
}