package dev.dtzudontsay.knownworld.simulation.npc.relationship;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class NpcRelationshipManager {

    private final NpcRegistry registry;

    private final Map<RelationshipKey, NpcRelationship> relationships =
            new LinkedHashMap<>();

    public NpcRelationshipManager(
            NpcRegistry registry
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized NpcRelationship getOrCreate(
            NpcId subject,
            NpcId target
    ) {
        validateNpc(
                subject
        );

        validateNpc(
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
                        new NpcRelationship(
                                subject,
                                target
                        )
        );
    }

    public synchronized Optional<NpcRelationship> find(
            NpcId subject,
            NpcId target
    ) {
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
            NpcRelationship relationship
    ) {
        Objects.requireNonNull(
                relationship,
                "relationship"
        );

        validateNpc(
                relationship.subject()
        );

        validateNpc(
                relationship.target()
        );

        relationships.put(
                new RelationshipKey(
                        relationship.subject(),
                        relationship.target()
                ),
                relationship
        );
    }

    public synchronized List<NpcRelationship> relationshipsFrom(
            NpcId subject
    ) {
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

    public synchronized Collection<NpcRelationship> all() {
        return List.copyOf(
                relationships.values()
        );
    }

    public synchronized int size() {
        return relationships.size();
    }

    private void validateNpc(
            NpcId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        if (!registry.contains(id)) {
            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + id
            );
        }
    }

    private record RelationshipKey(
            NpcId subject,
            NpcId target
    ) {
    }
}