package dev.dtzudontsay.knownworld.simulation.npc.routine;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.LinkedHashMap;

/**
 * Canonical storage of NPC routine information.
 */
public final class NpcRoutineManager {

    private final NpcRegistry registry;

    private final Map<NpcId, NpcRoutine> routines =
            new LinkedHashMap<>();

    public NpcRoutineManager(
            NpcRegistry registry
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized NpcRoutine getOrCreate(
            NpcId npc
    ) {
        validateNpc(
                npc
        );

        return routines.computeIfAbsent(
                npc,
                NpcRoutine::new
        );
    }

    public synchronized void registerLoaded(
            NpcRoutine routine
    ) {
        Objects.requireNonNull(
                routine,
                "routine"
        );

        validateNpc(
                routine.owner()
        );

        routines.put(
                routine.owner(),
                routine
        );
    }

    public synchronized List<NpcRoutine> all() {
        return List.copyOf(
                routines.values()
        );
    }

    public synchronized int size() {
        return routines.size();
    }

    private void validateNpc(
            NpcId id
    ) {
        Objects.requireNonNull(
                id,
                "id"
        );

        if (!registry.contains(
                id
        )) {

            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + id
            );
        }
    }
}