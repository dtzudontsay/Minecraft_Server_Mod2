package dev.dtzudontsay.knownworld.simulation.npc;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Canonical runtime registry of all persistent NPCs.
 *
 * This registry owns NPC identity allocation.
 */
public final class NpcRegistry {

    private final Map<NpcId, NpcState> npcs =
            new LinkedHashMap<>();

    private long nextId = 1L;

    public synchronized NpcState create(
            String givenName,
            String familyName,
            NpcSex sex,
            int birthYear,
            SimulationPosition position
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
                        position
                );

        npcs.put(
                id,
                state
        );

        return state;
    }

    /**
     * Adds a previously persisted NPC.
     *
     * This will matter when save loading is added in the next slice.
     */
    public synchronized void registerLoaded(
            NpcState npc
    ) {
        Objects.requireNonNull(
                npc,
                "npc"
        );

        if (npcs.containsKey(npc.id())) {
            throw new IllegalStateException(
                    "Duplicate NPC ID: "
                            + npc.id()
            );
        }

        npcs.put(
                npc.id(),
                npc
        );

        nextId =
                Math.max(
                        nextId,
                        npc.id().value() + 1
                );
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
        return npcs.containsKey(id);
    }

    public synchronized int size() {
        return npcs.size();
    }

    public synchronized long aliveCount() {
        return npcs.values()
                .stream()
                .filter(NpcState::isAlive)
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

        for (NpcState npc : npcs.values()) {

            if (!npc.isAlive()) {
                continue;
            }

            if (npc.position()
                    .horizontalDistanceSquared(center)
                    <= radiusSquared) {

                result.add(npc);
            }
        }

        return List.copyOf(result);
    }

    private NpcId allocateId() {
        if (nextId == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "NPC ID space exhausted"
            );
        }

        return new NpcId(
                nextId++
        );
    }
}