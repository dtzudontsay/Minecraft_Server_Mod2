package dev.dtzudontsay.knownworld.world.geography;

public record GeographicFeature(
        String id,
        String displayName,
        GeographicFeatureType type,
        boolean showEntryTitle,
        SourceConfidence confidence,
        String sourceNote
) {

    public FeatureGeometryType geometryType() {
        return type.geometryType();
    }
}