package dev.dtzudontsay.knownworld.world.geography;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GeographicFeatureRegistry {

    private static final Map<String, GeographicFeature> FEATURES =
            new LinkedHashMap<>();

    private GeographicFeatureRegistry() {
    }

    public static void register(GeographicFeature feature) {
        if (FEATURES.containsKey(feature.id())) {
            throw new IllegalStateException(
                    "Duplicate geographic feature id: " + feature.id()
            );
        }

        FEATURES.put(
                feature.id(),
                feature
        );
    }

    public static Optional<GeographicFeature> get(String id) {
        return Optional.ofNullable(
                FEATURES.get(id)
        );
    }

    public static List<GeographicFeature> getAll() {
        return Collections.unmodifiableList(
                new ArrayList<>(FEATURES.values())
        );
    }

    public static int getFeatureCount() {
        return FEATURES.size();
    }

    public static long countByGeometryType(
            FeatureGeometryType geometryType
    ) {
        return FEATURES.values()
                .stream()
                .filter(
                        feature ->
                                feature.geometryType() == geometryType
                )
                .count();
    }

    public static long countByFeatureType(
            GeographicFeatureType featureType
    ) {
        return FEATURES.values()
                .stream()
                .filter(
                        feature ->
                                feature.type() == featureType
                )
                .count();
    }

    public static boolean contains(String id) {
        return FEATURES.containsKey(id);
    }
}