package dev.dtzudontsay.knownworld.simulation.npc;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.personality.NpcPersonality;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class NpcRegistry {

    private final Map<NpcId, NpcState> npcs =
            new LinkedHashMap<>();

    private final NpcSpatialIndex spatialIndex =
            new NpcSpatialIndex();

    private long nextId =
            1L;

    public synchronized NpcState create(
            String givenName,
            String familyName,
            NpcSex sex,
            int birthYear,
            SimulationPosition position
    ) {
        return create(
                givenName,
                familyName,
                sex,
                birthYear,
                position,
                NpcPersonality.NEUTRAL
        );
    }

    public synchronized NpcState create(
            String givenName,
            String familyName,
            NpcSex sex,
            int birthYear,
            SimulationPosition position,
            NpcPersonality personality
    ) {
        Objects.requireNonNull(
                givenName,
                "givenName"
        );

        Objects.requireNonNull(
                familyName,
                "familyName"
        );

        Objects.requireNonNull(
                sex,
                "sex"
        );

        Objects.requireNonNull(
                position,
                "position"
        );

        Objects.requireNonNull(
                personality,
                "personality"
        );

        NpcId id =
                allocateId();

        NpcIdentity identity =
                new NpcIdentity(
                        id,
                        givenName,
                        familyName,
                        sex,
                        birthYear
                );

        NpcState state =
                new NpcState(
                        identity,
                        position,
                        personality,
                        dev.dtzudontsay.knownworld.simulation.SimulationLevel.DORMANT,
                        NpcLifeState.ALIVE
                );

        npcs.put(
                id,
                state
        );

        spatialIndex.add(
                id,
                position
        );

        return state;
    }

    public synchronized void registerLoaded(
            NpcState npc
    ) {
        Objects.requireNonNull(
                npc,
                "npc"
        );

        if (npcs.containsKey(
                npc.id()
        )) {
            throw new IllegalStateException(
                    "Duplicate NPC ID: "
                            + npc.id()
            );
        }

        npcs.put(
                npc.id(),
                npc
        );

        spatialIndex.add(
                npc.id(),
                npc.position()
        );

        if (npc.id().value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {
            nextId =
                    Math.max(
                            nextId,
                            npc.id().value()
                                    + 1
                    );
        }
    }

    public synchronized Optional<NpcState> find(
            NpcId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        return Optional.ofNullable(
                npcs.get(id)
        );
    }

    public synchronized boolean contains(
            NpcId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        return npcs.containsKey(
                id
        );
    }

    public synchronized void move(
            NpcId id,
            SimulationPosition destination
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        Objects.requireNonNull(
                destination,
                "destination"
        );

        NpcState npc =
                npcs.get(id);

        if (npc == null) {
            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + id
            );
        }

        SimulationPosition previous =
                npc.position();

        spatialIndex.move(
                id,
                previous,
                destination
        );

        npc.setPosition(
                destination
        );
    }

    public synchronized int size() {
        return npcs.size();
    }

    public synchronized long aliveCount() {
        return npcs.values()
                .stream()
                .filter(
                        NpcState::isAlive
                )
                .count();
    }

    public synchronized Collection<NpcState> all() {
        return Collections.unmodifiableList(
                new ArrayList<>(
                        npcs.values()
                )
        );
    }

    public synchronized List<NpcState> findWithinHorizontalRadius(
            SimulationPosition center,
            double radius
    ) {
        Objects.requireNonNull(
                center,
                "center"
        );

        if (!Double.isFinite(radius)
                || radius < 0.0) {

            throw new IllegalArgumentException(
                    "radius must be finite and >= 0"
            );
        }

        double radiusSquared =
                radius * radius;

        List<NpcState> result =
                new ArrayList<>();

        for (
                NpcId id :
                spatialIndex.queryCandidates(
                        center,
                        radius
                )
        ) {
            NpcState npc =
                    npcs.get(id);

            if (npc == null
                    || !npc.isAlive()) {
                continue;
            }

            if (
                    npc.position()
                            .horizontalDistanceSquared(
                                    center
                            )
                            <= radiusSquared
            ) {
                result.add(
                        npc
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    private NpcId allocateId() {
        if (nextId <= 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "NPC ID space exhausted"
            );
        }

        NpcId id =
                new NpcId(
                        nextId
                );

        nextId++;

        return id;
    }
}