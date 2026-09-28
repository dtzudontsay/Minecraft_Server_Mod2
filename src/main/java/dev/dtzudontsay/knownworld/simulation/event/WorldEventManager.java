package dev.dtzudontsay.knownworld.simulation.event;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Permanent journal of objective world events.
 */
public final class WorldEventManager {

    private final NpcRegistry npcRegistry;

    private final Map<WorldEventId, WorldEvent> events =
            new LinkedHashMap<>();

    private long nextId =
            1L;

    public WorldEventManager(
            NpcRegistry npcRegistry
    ) {
        this.npcRegistry =
                Objects.requireNonNull(
                        npcRegistry,
                        "npcRegistry"
                );
    }

    public synchronized WorldEvent create(
            WorldEventType type,
            String summary,
            SimulationPosition position,
            long occurredTick,
            double importance,
            NpcId actorNpc,
            NpcId subjectNpc,
            String factKey,
            String factValue
    ) {
        validateNpcIfPresent(
                actorNpc
        );

        validateNpcIfPresent(
                subjectNpc
        );

        WorldEvent event =
                new WorldEvent(
                        allocateId(),
                        type,
                        summary,
                        position,
                        occurredTick,
                        importance,
                        actorNpc,
                        subjectNpc,
                        factKey,
                        factValue
                );

        events.put(
                event.id(),
                event
        );

        return event;
    }

    public synchronized void registerLoaded(
            WorldEvent event
    ) {
        Objects.requireNonNull(
                event,
                "event"
        );

        validateNpcIfPresent(
                event.actorNpc()
        );

        validateNpcIfPresent(
                event.subjectNpc()
        );

        if (events.containsKey(
                event.id()
        )) {
            throw new IllegalStateException(
                    "Duplicate world event ID: "
                            + event.id()
            );
        }

        events.put(
                event.id(),
                event
        );

        if (event.id().value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {

            nextId =
                    Math.max(
                            nextId,
                            event.id().value()
                                    + 1
                    );
        }
    }

    public synchronized Optional<WorldEvent> find(
            WorldEventId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        return Optional.ofNullable(
                events.get(
                        id
                )
        );
    }

    public synchronized Collection<WorldEvent> all() {
        return List.copyOf(
                events.values()
        );
    }

    public synchronized int size() {
        return events.size();
    }

    private WorldEventId allocateId() {

        if (nextId <= 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "World event ID space exhausted"
            );
        }

        WorldEventId id =
                new WorldEventId(
                        nextId
                );

        nextId++;

        return id;
    }

    private void validateNpcIfPresent(
            NpcId id
    ) {
        if (id != null
                && !npcRegistry.contains(
                id
        )) {

            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + id
            );
        }
    }
}