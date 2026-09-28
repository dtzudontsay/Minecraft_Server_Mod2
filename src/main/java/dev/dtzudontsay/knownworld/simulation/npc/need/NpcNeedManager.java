package dev.dtzudontsay.knownworld.simulation.npc.need;

import dev.dtzudontsay.knownworld.simulation.SimulationLevel;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stores and updates basic NPC needs.
 */
public final class NpcNeedManager {

    private static final double FATIGUE_PER_SECOND =
            0.003;

    private static final double HUNGER_PER_SECOND =
            0.0015;

    private static final double SOCIAL_PER_SECOND =
            0.0008;

    private final NpcRegistry registry;

    private final Map<NpcId, NpcNeeds> needs =
            new LinkedHashMap<>();

    public NpcNeedManager(
            NpcRegistry registry
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );
    }

    public synchronized NpcNeeds getOrCreate(
            NpcId npc
    ) {
        validateNpc(
                npc
        );

        return needs.computeIfAbsent(
                npc,
                ignored ->
                        new NpcNeeds()
        );
    }

    public synchronized void registerLoaded(
            NpcId npc,
            NpcNeeds loaded
    ) {
        validateNpc(
                npc
        );

        Objects.requireNonNull(
                loaded,
                "loaded"
        );

        needs.put(
                npc,
                loaded
        );
    }

    /**
     * Coarse once-per-second need update.
     */
    public synchronized void update() {

        for (
                NpcState npc :
                registry.all()
        ) {
            if (!npc.isAlive()) {
                continue;
            }

            /*
             * Completely dormant NPCs will later receive long-timescale
             * simulation instead of one-second updates.
             */
            if (npc.simulationLevel()
                    == SimulationLevel.DORMANT) {

                continue;
            }

            NpcNeeds state =
                    getOrCreate(
                            npc.id()
                    );

            state.increaseFatigue(
                    FATIGUE_PER_SECOND
            );

            state.increaseHunger(
                    HUNGER_PER_SECOND
            );

            state.increaseSocial(
                    SOCIAL_PER_SECOND
            );
        }
    }

    public synchronized Map<NpcId, NpcNeeds> all() {
        return Map.copyOf(
                needs
        );
    }

    public synchronized int size() {
        return needs.size();
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