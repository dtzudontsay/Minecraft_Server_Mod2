package dev.dtzudontsay.knownworld.simulation.npc.action;

import dev.dtzudontsay.knownworld.simulation.SimulationLevel;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventManager;
import dev.dtzudontsay.knownworld.simulation.event.WorldEventType;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoal;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryManager;
import dev.dtzudontsay.knownworld.simulation.npc.memory.NpcMemoryType;
import dev.dtzudontsay.knownworld.simulation.npc.need.NpcNeedManager;
import dev.dtzudontsay.knownworld.simulation.npc.need.NpcNeeds;

import java.util.Objects;

public final class NpcActionProcessor {

    private static final double ABSTRACT_TRAVEL_SPEED_PER_SECOND =
            4.0;

    private static final double ARRIVAL_DISTANCE =
            2.0;

    private static final double REST_RECOVERY_PER_SECOND =
            0.08;

    private static final double REST_COMPLETE_AT =
            0.20;

    private final NpcRegistry registry;

    private final NpcGoalManager goals;

    private final NpcNeedManager needs;

    private final NpcMemoryManager memories;

    private final WorldEventManager events;

    public NpcActionProcessor(
            NpcRegistry registry,
            NpcGoalManager goals,
            NpcNeedManager needs,
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

        this.needs =
                Objects.requireNonNull(
                        needs,
                        "needs"
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

    public void update(
            long tick
    ) {
        for (NpcState npc : registry.all()) {

            if (!npc.isAlive()) {
                continue;
            }

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

        switch (goal.type()) {

            case TRAVEL ->
                    processTravel(
                            npc,
                            goal,
                            tick
                    );

            case REST ->
                    processRest(
                            npc,
                            goal,
                            tick
                    );

            case WORK ->
                    processWork(
                            npc,
                            goal
                    );

            default -> {
            }
        }
    }

    /**
     * Generic work currently has no world-space side effect.
     *
     * The goal remains ACTIVE until the routine service cancels it
     * when the work period ends.
     *
     * Later this dispatches to role-specific systems.
     */
    private void processWork(
            NpcState npc,
            NpcGoal goal
    ) {
        /*
         * Intentionally empty for now.
         */
    }

    private void processRest(
            NpcState npc,
            NpcGoal goal,
            long tick
    ) {
        NpcNeeds state =
                needs.getOrCreate(
                        npc.id()
                );

        state.reduceFatigue(
                REST_RECOVERY_PER_SECOND
        );

        if (state.fatigue()
                > REST_COMPLETE_AT) {

            return;
        }

        goal.complete();

        memories.remember(
                npc.id(),
                NpcMemoryType.PERSONAL_EXPERIENCE,
                "Rested and recovered from fatigue.",
                0.15,
                null,
                null,
                tick
        );
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

        if (distance
                <= ARRIVAL_DISTANCE) {

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

        SimulationPosition next =
                new SimulationPosition(
                        current.dimension(),
                        current.x()
                                + dx * scale,
                        current.y(),
                        current.z()
                                + dz * scale
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