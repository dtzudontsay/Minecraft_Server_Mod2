package dev.dtzudontsay.knownworld.simulation.npc.action;

import dev.dtzudontsay.knownworld.simulation.SimulationLevel;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoal;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalManager;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalType;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;

import java.util.Objects;

/**
 * Executes simulation actions derived from current NPC goals.
 *
 * Only abstract travel exists in this first version.
 *
 * PHYSICAL NPC movement will later be delegated to Minecraft entity
 * navigation instead.
 */
public final class NpcActionProcessor {

    private static final double ABSTRACT_TRAVEL_SPEED_PER_SECOND =
            4.0;

    private static final double ARRIVAL_DISTANCE =
            2.0;

    private final NpcRegistry registry;

    private final NpcGoalManager goals;

    private final NpcMemoryManager memories;

    private final WorldEventManager events;

    public NpcActionProcessor(
            NpcRegistry registry,
            NpcGoalManager goals,
            NpcMemoryManager memories,
            WorldEventManager events
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.goals =
                Objects.requireNonNull(
                        goals,
                        "goals"
                );

        this.memories =
                Objects.requireNonNull(
                        memories,
                        "memories"
                );

        this.events =
                Objects.requireNonNull(
                        events,
                        "events"
                );
    }

    /**
     * Runs one coarse action update.
     *
     * Expected cadence:
     * approximately once per second.
     */
    public void update(
            long tick
    ) {
        for (NpcState npc : registry.all()) {

            if (!npc.isAlive()) {
                continue;
            }

            /*
             * DORMANT NPCs receive no active goal execution.
             *
             * They will later receive very coarse long-timescale
             * simulation instead.
             */
            if (npc.simulationLevel()
                    == SimulationLevel.DORMANT) {

                continue;
            }

            goals.currentGoal(
                            npc.id()
                    )
                    .ifPresent(
                            goal ->
                                    processGoal(
                                            npc,
                                            goal,
                                            tick
                                    )
                    );
        }
    }

    private void processGoal(
            NpcState npc,
            NpcGoal goal,
            long tick
    ) {
        goal.activate();

        if (goal.type()
                == NpcGoalType.TRAVEL) {

            processTravel(
                    npc,
                    goal,
                    tick
            );
        }
    }

    private void processTravel(
            NpcState npc,
            NpcGoal goal,
            long tick
    ) {
        SimulationPosition destination =
                goal.targetPosition();

        if (destination == null) {

            goal.fail();

            return;
        }

        SimulationPosition current =
                npc.position();

        if (!current.dimension()
                .equals(
                        destination.dimension()
                )) {

            /*
             * Cross-dimension travel requires a portal / route system
             * later.
             */
            goal.fail();

            return;
        }

        double dx =
                destination.x()
                        - current.x();

        double dz =
                destination.z()
                        - current.z();

        double distance =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );

        if (distance <= ARRIVAL_DISTANCE) {

            registry.move(
                    npc.id(),
                    destination
            );

            completeTravel(
                    npc,
                    goal,
                    tick
            );

            return;
        }

        double movementDistance =
                Math.min(
                        ABSTRACT_TRAVEL_SPEED_PER_SECOND,
                        distance
                );

        double scale =
                movementDistance
                        / distance;

        double nextX =
                current.x()
                        + dx * scale;

        double nextZ =
                current.z()
                        + dz * scale;

        /*
         * Y stays unchanged for now.
         *
         * Later route/path systems will determine terrain elevation,
         * roads, bridges, ships, mountain passes, etc.
         */
        SimulationPosition next =
                new SimulationPosition(
                        current.dimension(),
                        current.x() + dx * scale,
                        current.y(),
                        current.z() + dz * scale
                );

        registry.move(
                npc.id(),
                next
        );
    }

    private void completeTravel(
            NpcState npc,
            NpcGoal goal,
            long tick
    ) {
        goal.complete();

        memories.remember(
                npc.id(),
                NpcMemoryType.PERSONAL_EXPERIENCE,
                "Reached destination: "
                        + goal.description(),
                Math.max(
                        0.20,
                        goal.priority()
                                * 0.50
                ),
                null,
                null,
                tick
        );

        events.create(
                WorldEventType.GENERAL,
                npc.identity()
                        .fullName()
                        + " arrived at a destination.",
                npc.position(),
                tick,
                Math.max(
                        0.10,
                        goal.priority()
                                * 0.25
                ),
                npc.id(),
                npc.id(),
                null,
                null
        );
    }
}