package dev.dtzudontsay.knownworld.world.geography.location;

import java.util.Optional;

public final class FeatureLocationResolver {

    private FeatureLocationResolver() {
    }

    public static Optional<FeatureLocation> resolve(
            String featureId
    ) {
        Optional<RegionalFeatureLocation> regional =
                RegionalFeatureLocationRegistry.get(
                        featureId
                );

        if (regional.isPresent()) {
            return Optional.of(
                    regional.get().resolve()
            );
        }

        return FeatureLocationRegistry.get(
                featureId
        );
    }

    public static boolean usesRegionalSource(
            String featureId
    ) {
        return RegionalFeatureLocationRegistry
                .get(featureId)
                .isPresent();
    }

    public static int resolvedLocationCount() {
        return FeatureLocationRegistry.locationCount()
                + RegionalFeatureLocationRegistry.locationCount();
    }
}