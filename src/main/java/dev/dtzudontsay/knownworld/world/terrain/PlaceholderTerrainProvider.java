package dev.dtzudontsay.knownworld.world.terrain;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;

public final class PlaceholderTerrainProvider implements TerrainProvider {

    @Override
    public TerrainSample sample(WorldCoordinate coordinate) {
        return new TerrainSample(
                0.0,
                "UNASSIGNED",
                "PLACEHOLDER"
        );
    }
}