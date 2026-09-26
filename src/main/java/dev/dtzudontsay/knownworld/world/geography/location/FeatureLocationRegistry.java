package dev.dtzudontsay.knownworld.world.geography.location;

import dev.dtzudontsay.knownworld.world.geography.MapCoordinate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class FeatureLocationRegistry {

    private static final Map<String, FeatureLocation> LOCATIONS =
            new LinkedHashMap<>();

    static {
        /*
         * Castle Black and Winterfell are no longer stored here.
         *
         * Their more accurate positions now come from the
         * high-resolution North Westeros regional map through
         * RegionalFeatureLocationRegistry.
         */

        register(
                new FeatureLocation(
                        "kings_landing",
                        new MapCoordinate(
                                412.35,
                                694.49
                        ),
                        "Measured from marked 2048x1357 Known World master map."
                )
        );

        register(
                new FeatureLocation(
                        "oldtown",
                        new MapCoordinate(
                                229.13,
                                844.25
                        ),
                        "Measured from marked 2048x1357 Known World master map."
                )
        );

        register(
                new FeatureLocation(
                        "volantis",
                        new MapCoordinate(
                                790.18,
                                897.27
                        ),
                        "Measured from marked 2048x1357 Known World master map."
                )
        );

        register(
                new FeatureLocation(
                        "asshai",
                        new MapCoordinate(
                                1815.09,
                                1257.47
                        ),
                        "Measured from marked 2048x1357 Known World master map."
                )
        );
    }

    private FeatureLocationRegistry() {
    }

    private static void register(
            FeatureLocation location
    ) {
        if (LOCATIONS.containsKey(location.featureId())) {
            throw new IllegalStateException(
                    "Duplicate master feature location: "
                            + location.featureId()
            );
        }

        LOCATIONS.put(
                location.featureId(),
                location
        );
    }

    public static Optional<FeatureLocation> get(
            String featureId
    ) {
        return Optional.ofNullable(
                LOCATIONS.get(featureId)
        );
    }

    public static int locationCount() {
        return LOCATIONS.size();
    }

    public static Map<String, FeatureLocation> getAll() {
        return Map.copyOf(
                LOCATIONS
        );
    }
}