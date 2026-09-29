package dev.dtzudontsay.knownworld.world.reference;

import java.util.Objects;

public record WorldLocationDefinition(
        String id,
        String displayName,
        WorldLocationKind kind,
        String parentId,
        String geographicFeatureId,
        String boundaryMaskId,
        ReferenceProvenance provenance,
        String sourceNote,
        boolean importedFromGeography
) {

    public WorldLocationDefinition {

        id =
                requireId(
                        id,
                        "id"
                );

        if (displayName == null
                || displayName.isBlank()) {

            throw new IllegalArgumentException(
                    "displayName cannot be empty"
            );
        }

        displayName =
                displayName.trim();

        kind =
                Objects.requireNonNull(
                        kind,
                        "kind"
                );

        parentId =
                optionalId(
                        parentId
                );

        geographicFeatureId =
                optionalId(
                        geographicFeatureId
                );

        boundaryMaskId =
                optionalId(
                        boundaryMaskId
                );

        provenance =
                Objects.requireNonNull(
                        provenance,
                        "provenance"
                );

        sourceNote =
                sourceNote == null
                        ? ""
                        : sourceNote.trim();
    }

    public boolean hasParent() {
        return parentId != null;
    }

    public boolean hasGeographicFeature() {
        return geographicFeatureId != null;
    }

    public boolean hasBoundaryMask() {
        return boundaryMaskId != null;
    }

    private static String requireId(
            String value,
            String description
    ) {
        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    description
                            + " cannot be empty"
            );
        }

        String normalized =
                value.trim()
                        .toLowerCase();

        if (!normalized.matches(
                "[a-z0-9_.\\-]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid "
                            + description
                            + ": "
                            + value
            );
        }

        return normalized;
    }

    private static String optionalId(
            String value
    ) {
        if (value == null
                || value.isBlank()) {

            return null;
        }

        return requireId(
                value,
                "reference ID"
        );
    }
}