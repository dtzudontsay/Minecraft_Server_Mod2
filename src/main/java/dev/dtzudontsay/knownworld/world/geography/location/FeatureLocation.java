package dev.dtzudontsay.knownworld.world.geography.location;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldCoordinate;
import dev.dtzudontsay.knownworld.world.geography.WorldProjection;
import dev.dtzudontsay.knownworld.world.geography.calibration.MasterMapCalibration;

public record FeatureLocation(
        String featureId,
        MapCoordinate masterMapCoordinate,
        String sourceNote
) {

    public WorldCoordinate worldCoordinate() {
        return MasterMapCalibration.toWorld(
                masterMapCoordinate
        );
    }

    public double minecraftX() {
        return WorldProjection.minecraftX(
                worldCoordinate()
        );
    }

    public double minecraftZ() {
        return WorldProjection.minecraftZ(
                worldCoordinate()
        );
    }
}