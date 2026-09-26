package dev.dtzudontsay.knownworld.world.terrain;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;

@FunctionalInterface
public interface TerrainProvider {
    TerrainSample sample(WorldCoordinate coordinate);
}