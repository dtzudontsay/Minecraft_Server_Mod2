package dev.dtzudontsay.knownworld.world.geography;

public record CanonicalAnchor(
        String id,
        String displayName,
        MapCoordinate sourceMapCoordinate,
        WorldCoordinate worldCoordinate,
        SourceConfidence confidence,
        String sourceNote
) {
}