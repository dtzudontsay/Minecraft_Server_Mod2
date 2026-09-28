package dev.dtzudontsay.knownworld.simulation.npc.routine;

import dev.dtzudontsay.knownworld.simulation.SimulationLevel;
import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoal;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalManager;
import dev.dtzudontsay.knownworld.simulation.npc.goal.NpcGoalType;

import java.util.Objects;

/**
 * Converts daily routine information into ordinary NPC goals.
 *
 * Routine goals are deliberately low priority so urgent needs,
 * danger, military orders or other important goals can override them.
 */
public final class NpcRoutineService {

    private static final double ROUTINE_DISTANCE =
            2.0;

    private static final double WORK_TRAVEL_PRIORITY =
            0.35;

    private static final double WORK_PRIORITY =
            0.25;

    private static final double HOME_TRAVEL_PRIORITY =
            0.30;

    private final NpcRegistry registry;

    private final NpcRoutineManager routines;

    private final NpcGoalManager goals;

    public NpcRoutineService(
            NpcRegistry registry,
            NpcRoutineManager routines,
            NpcGoalManager goals
    ) {
        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.routines =
                Objects.requireNonNull(
                        routines,
                        "routines"
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
        for (NpcState npc : registry.all()) {

            if (!npc.isAlive()) {
                continue;
            }

            if (npc.simulationLevel()
                    == SimulationLevel.DORMANT) {

                continue;
            }

            NpcRoutine routine =
                    routines.getOrCreate(
                            npc.id()
                    );

            if (routine.isWorkTime(
                    tick
            )) {

                handleWorkTime(
                        npc,
                        routine,
                        tick
                );

            } else {

                handleOffTime(
                        npc,
                        routine,
                        tick
                );
            }
        }
    }

    private void handleWorkTime(
            NpcState npc,
            NpcRoutine routine,
            long tick
    ) {
        if (!routine.hasWorkplace()
                || routine.role()
                == NpcRoleType.UNASSIGNED) {

            return;
        }

        /*
         * If some more important goal already exists, routine duties
         * wait.
         */
        NpcGoal current =
                goals.currentGoal(
                                npc.id()
                        )
                        .orElse(
                                null
                        );

        if (current != null
                && current.type()
                != NpcGoalType.WORK
                && current.priority()
                > WORK_TRAVEL_PRIORITY) {

            return;
        }

        SimulationPosition workplace =
                routine.workPosition();

        double distance =
                npc.position()
                        .horizontalDistance(
                                workplace
                        );

        if (distance
                > ROUTINE_DISTANCE) {

            if (!goals.hasOpenGoalOfType(
                    npc.id(),
                    NpcGoalType.TRAVEL
            )) {

                goals.create(
                        npc.id(),
                        NpcGoalType.TRAVEL,
                        "Travel to workplace as "
                                + routine.role(),
                        WORK_TRAVEL_PRIORITY,
                        tick,
                        null,
                        workplace,
                        null
                );
            }

            return;
        }

        if (!goals.hasOpenGoalOfType(
                npc.id(),
                NpcGoalType.WORK
        )) {

            goals.create(
                    npc.id(),
                    NpcGoalType.WORK,
                    "Perform duties as "
                            + routine.role(),
                    WORK_PRIORITY,
                    tick,
                    null,
                    workplace,
                    null
            );
        }
    }

    private void handleOffTime(
            NpcState npc,
            NpcRoutine routine,
            long tick
    ) {
        /*
         * Work is no longer relevant once duty hours end.
         */
        goals.cancelOpenGoalsOfType(
                npc.id(),
                NpcGoalType.WORK
        );

        if (!routine.hasHome()) {
            return;
        }

        NpcGoal current =
                goals.currentGoal(
                                npc.id()
                        )
                        .orElse(
                                null
                        );

        /*
         * Do not interrupt important autonomous behavior simply
         * because the routine says "go home".
         */
        if (current != null
                && current.priority()
                > HOME_TRAVEL_PRIORITY) {

            return;
        }

        double distance =
                npc.position()
                        .horizontalDistance(
                                routine.homePosition()
                        );

        if (distance
                <= ROUTINE_DISTANCE) {

            return;
        }

        if (!goals.hasOpenGoalOfType(
                npc.id(),
                NpcGoalType.TRAVEL
        )) {

            goals.create(
                    npc.id(),
                    NpcGoalType.TRAVEL,
                    "Return home",
                    HOME_TRAVEL_PRIORITY,
                    tick,
                    null,
                    routine.homePosition(),
                    null
            );
        }
    }
}