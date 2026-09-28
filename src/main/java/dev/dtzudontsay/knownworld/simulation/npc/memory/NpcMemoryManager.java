package dev.dtzudontsay.knownworld.simulation.npc.memory;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canonical storage for persistent NPC memories.
 */
public final class NpcMemoryManager {

    private final NpcRegistry registry;

    private final Map<NpcMemoryId, NpcMemory> memories =
            new LinkedHashMap<>();

    private final Map<NpcId, List<NpcMemoryId>> memoriesByOwner =
            new LinkedHashMap<>();

    private long nextId =
            1L;

    public NpcMemoryManager(
            NpcRegistry registry
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized NpcMemory remember(
            NpcId owner,
            NpcMemoryType type,
            String summary,
            double importance,
            NpcId relatedNpc,
            String factKey,
            long createdTick
    ) {
        validateNpc(
                owner
        );

        if (relatedNpc != null) {
            validateNpc(
                    relatedNpc
            );
        }

        NpcMemoryId id =
                allocateId();

        NpcMemory memory =
                new NpcMemory(
                        id,
                        owner,
                        type,
                        summary,
                        importance,
                        relatedNpc,
                        factKey,
                        createdTick
                );

        put(
                memory
        );

        return memory;
    }

    public synchronized void registerLoaded(
            NpcMemory memory
    ) {
        Objects.requireNonNull(
                memory,
                "memory"
        );

        validateNpc(
                memory.owner()
        );

        if (memory.relatedNpc() != null) {
            validateNpc(
                    memory.relatedNpc()
            );
        }

        if (memories.containsKey(
                memory.id()
        )) {
            throw new IllegalStateException(
                    "Duplicate memory ID: "
                            + memory.id()
            );
        }

        put(
                memory
        );

        if (memory.id().value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {
            nextId =
                    Math.max(
                            nextId,
                            memory.id()
                                    .value()
                                    + 1
                    );
        }
    }

    public synchronized List<NpcMemory> memoriesOf(
            NpcId owner
    ) {
        validateNpc(
                owner
        );

        List<NpcMemoryId> ids =
                memoriesByOwner.get(
                        owner
                );

        if (ids == null
                || ids.isEmpty()) {

            return List.of();
        }

        List<NpcMemory> result =
                new ArrayList<>(
                        ids.size()
                );

        for (NpcMemoryId id : ids) {

            NpcMemory memory =
                    memories.get(
                            id
                    );

            if (memory != null) {
                result.add(
                        memory
                );
            }
        }

        return List.copyOf(
                result
        );
    }

    public synchronized List<NpcMemory> importantMemoriesOf(
            NpcId owner,
            double minimumImportance
    ) {
        if (!Double.isFinite(
                minimumImportance
        )
                || minimumImportance < 0.0
                || minimumImportance > 1.0) {

            throw new IllegalArgumentException(
                    "minimumImportance must be between 0.0 and 1.0"
            );
        }

        return memoriesOf(
                owner
        )
                .stream()
                .filter(
                        memory ->
                                memory.importance()
                                        >= minimumImportance
                )
                .toList();
    }

    public synchronized Collection<NpcMemory> all() {
        return List.copyOf(
                memories.values()
        );
    }

    public synchronized int size() {
        return memories.size();
    }

    private void put(
            NpcMemory memory
    ) {
        memories.put(
                memory.id(),
                memory
        );

        memoriesByOwner
                .computeIfAbsent(
                        memory.owner(),
                        ignored ->
                                new ArrayList<>()
                )
                .add(
                        memory.id()
                );
    }

    private NpcMemoryId allocateId() {

        if (nextId <= 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Memory ID space exhausted"
            );
        }

        NpcMemoryId id =
                new NpcMemoryId(
                        nextId
                );

        nextId++;

        return id;
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