package dev.dtzudontsay.knownworld.world.geography;

public final class WorldProjection {
    public static final double METRES_PER_BLOCK =
            WorldDefinition.HORIZONTAL_METRES_PER_BLOCK;

    private WorldProjection() {
    }

    public static WorldCoordinate fromMinecraft(double minecraftX, double minecraftZ) {
        return new WorldCoordinate(
                minecraftX * METRES_PER_BLOCK,
                -minecraftZ * METRES_PER_BLOCK
        );
    }

    public static double minecraftX(WorldCoordinate coordinate) {
        return coordinate.eastMetres() / METRES_PER_BLOCK;
    }

    public static double minecraftZ(WorldCoordinate coordinate) {
        return -coordinate.northMetres() / METRES_PER_BLOCK;
    }
}