package dev.dtzudontsay.knownworld.world.geography.location;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.geography.zones.MapZoneDefinition;
import dev.dtzudontsay.knownworld.world.geography.zones.MapZoneId;
import dev.dtzudontsay.knownworld.world.geography.zones.MapZoneRegistry;

public record RegionalFeatureLocation(
        String featureId,
        MapZoneId zoneId,
        MapCoordinate regionalMapCoordinate,
        String sourceNote
) {

    public FeatureLocation resolve() {
        MapZoneDefinition zone =
                MapZoneRegistry
                        .get(zoneId)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Map zone is not calibrated: "
                                                + zoneId
                                )
                        );

        MapCoordinate masterCoordinate =
                zone.toMaster(
                        regionalMapCoordinate
                );

        return new FeatureLocation(
                featureId,
                masterCoordinate,
                sourceNote
        );
    }
}