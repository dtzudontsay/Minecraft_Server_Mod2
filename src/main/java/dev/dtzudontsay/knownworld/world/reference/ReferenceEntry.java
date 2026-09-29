package dev.dtzudontsay.knownworld.world.reference;

import java.util.Objects;

public record ReferenceEntry(
        String id,
        String displayName,
        String parentId,
        String category,
        ReferenceProvenance provenance,
        String sourceNote
) {

    public ReferenceEntry {

        id =
                requireId(
                        id,
                        "id"
                );

        displayName =
                requireText(
                        displayName,
                        "displayName"
                );

        parentId =
                normalizeOptionalId(
                        parentId
                );

        category =
                requireId(
                        category,
                        "category"
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

    private static String normalizeOptionalId(
            String value
    ) {
        if (value == null
                || value.isBlank()) {

            return null;
        }

        return requireId(
                value,
                "parentId"
        );
    }

    private static String requireText(
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

        return value.trim();
    }
}