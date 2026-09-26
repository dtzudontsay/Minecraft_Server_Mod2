package dev.dtzudontsay.knownworld.world.geography;

public final class WorldProjection {

    private WorldProjection() {
    }

    public static WorldCoordinate fromMinecraft(
            double minecraftX,
            double minecraftZ
    ) {
        double scale =
                WorldDefinition.MINECRAFT_WORLD_SCALE;

        if (scale <= 0.0) {
            throw new IllegalStateException(
                    "MINECRAFT_WORLD_SCALE must be > 0"
            );
        }

        double canonicalMetresPerBlock =
                WorldDefinition.HORIZONTAL_METRES_PER_BLOCK
                        / scale;

        return new WorldCoordinate(
                minecraftX * canonicalMetresPerBlock,
                -minecraftZ * canonicalMetresPerBlock
        );
    }

    public static double minecraftX(
            WorldCoordinate coordinate
    ) {
        return coordinate.eastMetres()
                / WorldDefinition.HORIZONTAL_METRES_PER_BLOCK
                * WorldDefinition.MINECRAFT_WORLD_SCALE;
    }

    public static double minecraftZ(
            WorldCoordinate coordinate
    ) {
        return -coordinate.northMetres()
                / WorldDefinition.HORIZONTAL_METRES_PER_BLOCK
                * WorldDefinition.MINECRAFT_WORLD_SCALE;
    }
}