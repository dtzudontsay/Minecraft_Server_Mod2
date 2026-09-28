package dev.dtzudontsay.knownworld.simulation.npc.decision;

import dev.dtzudontsay.knownworld.simulation.SimulationLevel;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalManager;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalType;
import dev.dtzudontsay.knownworld.simulation.npc.need.NpcNeedManager;
import dev.dtzudontsay.knownworld.simulation.npc.need.NpcNeeds;

import java.util.Objects;

/**
 * Converts NPC internal state into intentions/goals.
 *
 * This is intentionally deterministic.
 *
 * LLM reasoning may later propose higher-level intentions, but ordinary
 * needs should never require an LLM call.
 */
public final class NpcDecisionService {

    private static final double REST_THRESHOLD =
            0.75;

    private final NpcRegistry registry;

    private final NpcNeedManager needs;

    private final NpcGoalManager goals;

    public NpcDecisionService(
            NpcRegistry registry,
            NpcNeedManager needs,
            NpcGoalManager goals
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.needs =
                Objects.requireNonNull(
                        needs,
                        "needs"
                );

        this.goals =
                Objects.requireNonNull(
                        goals,
                        "goals"
                );
    }

    public void update(
            long tick
    ) {
        for (
                NpcState npc :
                registry.all()
        ) {
            if (!npc.isAlive()) {
                continue;
            }

            if (npc.simulationLevel()
                    == SimulationLevel.DORMANT) {

                continue;
            }

            NpcNeeds state =
                    needs.getOrCreate(
                            npc.id()
                    );

            considerRest(
                    npc,
                    state,
                    tick
            );
        }
    }

    private void considerRest(
            NpcState npc,
            NpcNeeds needsState,
            long tick
    ) {
        if (needsState.fatigue()
                < REST_THRESHOLD) {

            return;
        }

        if (
                goals.hasOpenGoalOfType(
                        npc.id(),
                        NpcGoalType.REST
                )
        ) {
            return;
        }

        /*
         * Priority rises with fatigue.
         *
         * At threshold:
         * 0.75
         *
         * At exhaustion:
         * 1.00
         */
        double priority =
                needsState.fatigue();

        goals.create(
                npc.id(),
                NpcGoalType.REST,
                "Rest and recover from fatigue",
                priority,
                tick,
                null,
                null,
                null
        );
    }
}