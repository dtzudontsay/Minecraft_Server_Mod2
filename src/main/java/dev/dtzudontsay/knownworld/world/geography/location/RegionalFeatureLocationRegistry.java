package dev.dtzudontsay.knownworld.world.geography.location;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;
import dev.dtzudontsay.knownworld.world.geography.zones.MapZoneId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class RegionalFeatureLocationRegistry {

    private static final Map<String, RegionalFeatureLocation> LOCATIONS =
            new LinkedHashMap<>();

    static {
        register(
                new RegionalFeatureLocation(
                        "castle_black",
                        MapZoneId.NW,
                        new MapCoordinate(
                                1439.46,
                                729.00
                        ),
                        "Measured from marked high-resolution North Westeros regional map."
                )
        );

        register(
                new RegionalFeatureLocation(
                        "winterfell",
                        MapZoneId.NW,
                        new MapCoordinate(
                                1230.12,
                                1198.65
                        ),
                        "Measured from marked high-resolution North Westeros regional map."
                )
        );
    }

    private RegionalFeatureLocationRegistry() {
    }

    private static void register(
            RegionalFeatureLocation location
    ) {
        if (LOCATIONS.containsKey(location.featureId())) {
            throw new IllegalStateException(
                    "Duplicate regional feature location: "
                            + location.featureId()
            );
        }

        LOCATIONS.put(
                location.featureId(),
                location
        );
    }

    public static Optional<RegionalFeatureLocation> get(
            String featureId
    ) {
        return Optional.ofNullable(
                LOCATIONS.get(featureId)
        );
    }

    public static int locationCount() {
        return LOCATIONS.size();
    }

    public static Map<String, RegionalFeatureLocation> getAll() {
        return Map.copyOf(
                LOCATIONS
        );
    }
}