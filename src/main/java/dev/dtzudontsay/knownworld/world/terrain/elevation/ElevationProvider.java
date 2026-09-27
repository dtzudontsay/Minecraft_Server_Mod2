package dev.dtzudontsay.knownworld.world.terrain.elevation;

import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;

public interface ElevationProvider {

    ElevationSample sample(
            WorldCoordinate coordinate
    );

    boolean hasCanonicalElevationData();
}