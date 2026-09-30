package dev.dtzudontsay.knownworld.simulation.npc.religion;

import dev.dtzudontsay.knownworld.world.reference.ReferenceProvenance;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ReligiousOrderDefinition(
        String id,
        String displayName,
        String religionId,
        ReligiousOrderCategory category,
        boolean clergy,
        boolean militant,
        boolean exclusiveMembership,
        double minimumDevotion,
        double minimumDoctrinalKnowledge,
        Set<String> rituals,
        Set<String> taboos,
        Set<String> roles,
        ReferenceProvenance provenance,
        String sourceNote
) {

    public ReligiousOrderDefinition {

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

        religionId =
                requireId(
                        religionId,
                        "religionId"
                );

        category =
                Objects.requireNonNull(
                        category,
                        "category"
                );

        if (!Double.isFinite(
                minimumDevotion
        )
                || minimumDevotion < 0.0
                || minimumDevotion > 1.0) {

            throw new IllegalArgumentException(
                    "minimumDevotion must be between 0 and 1"
            );
        }

        if (!Double.isFinite(
                minimumDoctrinalKnowledge
        )
                || minimumDoctrinalKnowledge < 0.0
                || minimumDoctrinalKnowledge > 1.0) {

            throw new IllegalArgumentException(
                    "minimumDoctrinalKnowledge must be between 0 and 1"
            );
        }

        rituals =
                rituals == null
                        ? Set.of()
                        : Set.copyOf(
                        rituals
                );

        taboos =
                taboos == null
                        ? Set.of()
                        : Set.copyOf(
                        taboos
                );

        roles =
                roles == null
                        ? Set.of()
                        : Set.copyOf(
                        roles
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

    private static String requireId(
            String value,
            String name
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    name + " cannot be blank"
            );
        }

        return value.trim()
                .toLowerCase();
    }

    private static String requireText(
            String value,
            String name
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    name + " cannot be blank"
            );
        }

        return value.trim();
    }
}