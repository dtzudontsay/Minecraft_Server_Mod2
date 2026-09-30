package dev.dtzudontsay.knownworld.simulation.social.relationship;

import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;
import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyManager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DynastyRelationshipManager {

    private final DynastyManager dynasties;

    private final Map<RelationshipKey, DynastyRelationship> relationships =
            new LinkedHashMap<>();

    public DynastyRelationshipManager(
            DynastyManager dynasties
    ) {

        this.dynasties =
                Objects.requireNonNull(
                        dynasties,
                        "dynasties"
                );
    }

    public synchronized DynastyRelationship getOrCreate(
            DynastyId subject,
            DynastyId target
    ) {

        validatePair(
                subject,
                target
        );

        RelationshipKey key =
                new RelationshipKey(
                        subject,
                        target
                );

        return relationships.computeIfAbsent(
                key,
                ignored ->
                        new DynastyRelationship(
                                subject,
                                target
                        )
        );
    }

    public synchronized Optional<DynastyRelationship> find(
            DynastyId subject,
            DynastyId target
    ) {

        validatePair(
                subject,
                target
        );

        return Optional.ofNullable(
                relationships.get(
                        new RelationshipKey(
                                subject,
                                target
                        )
                )
        );
    }

    public synchronized void registerLoaded(
            DynastyRelationship relationship
    ) {

        Objects.requireNonNull(
                relationship,
                "relationship"
        );

        validatePair(
                relationship.subject(),
                relationship.target()
        );

        RelationshipKey key =
                new RelationshipKey(
                        relationship.subject(),
                        relationship.target()
                );

        if (relationships.putIfAbsent(
                key,
                relationship
        ) != null) {

            throw new IllegalStateException(
                    "Duplicate dynasty relationship "
                            + relationship.subject()
                            + " -> "
                            + relationship.target()
            );
        }
    }

    public synchronized boolean reconcileAuthored(
            DynastyId subject,
            DynastyId target,
            int authoredVersion,
            double affinity,
            double trust,
            double respect,
            double fear,
            double familiarity
    ) {

        validatePair(
                subject,
                target
        );

        RelationshipKey key =
                new RelationshipKey(
                        subject,
                        target
                );

        DynastyRelationship existing =
                relationships.get(
                        key
                );

        if (existing == null) {

            relationships.put(
                    key,
                    new DynastyRelationship(
                            subject,
                            target,
                            authoredVersion,
                            affinity,
                            trust,
                            respect,
                            fear,
                            familiarity
                    )
            );

            return true;
        }

        return existing.applyAuthoredRevision(
                authoredVersion,
                affinity,
                trust,
                respect,
                fear,
                familiarity
        );
    }

    public synchronized List<DynastyRelationship> relationshipsFrom(
            DynastyId subject
    ) {

        dynasties.require(
                subject
        );

        return relationships.values()
                .stream()
                .filter(
                        relationship ->
                                relationship.subject()
                                        .equals(
                                                subject
                                        )
                )
                .toList();
    }

    public synchronized Collection<DynastyRelationship> all() {

        return List.copyOf(
                relationships.values()
        );
    }

    public synchronized int size() {
        return relationships.size();
    }

    private void validatePair(
            DynastyId subject,
            DynastyId target
    ) {

        Objects.requireNonNull(
                subject,
                "subject"
        );

        Objects.requireNonNull(
                target,
                "target"
        );

        dynasties.require(
                subject
        );

        dynasties.require(
                target
        );

        if (subject.equals(
                target
        )) {

            throw new IllegalArgumentException(
                    "Dynasty cannot have a relationship toward itself"
            );
        }
    }

    private record RelationshipKey(
            DynastyId subject,
            DynastyId target
    ) {
    }
}
