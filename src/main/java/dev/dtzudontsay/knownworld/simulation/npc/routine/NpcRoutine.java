package dev.dtzudontsay.knownworld.simulation.npc.routine;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;

/**
 * Basic daily-life configuration for one NPC.
 *
 * This is not the NPC's current action.
 *
 * It describes where they normally live, where they normally work,
 * and during which part of the simulation day they are expected to
 * perform their role.
 */
public final class NpcRoutine {

    public static final int TICKS_PER_DAY =
            24000;

    private final NpcId owner;

    private NpcRoleType role;

    private SimulationPosition homePosition;

    private SimulationPosition workPosition;

    private int workStartTick;

    private int workEndTick;

    public NpcRoutine(
            NpcId owner
    ) {
        this(
                owner,
                NpcRoleType.UNASSIGNED,
                null,
                null,
                6000,
                12000
        );
    }

    public NpcRoutine(
            NpcId owner,
            NpcRoleType role,
            SimulationPosition homePosition,
            SimulationPosition workPosition,
            int workStartTick,
            int workEndTick
    ) {
        this.owner =
                Objects.requireNonNull(
                        owner,
                        "owner"
                );

        this.role =
                Objects.requireNonNull(
                        role,
                        "role"
                );

        validateDayTick(
                workStartTick
        );

        validateDayTick(
                workEndTick
        );

        if (workStartTick
                == workEndTick) {

            throw new IllegalArgumentException(
                    "Work start and end cannot be identical"
            );
        }

        this.homePosition =
                homePosition;

        this.workPosition =
                workPosition;

        this.workStartTick =
                workStartTick;

        this.workEndTick =
                workEndTick;
    }

    public NpcId owner() {
        return owner;
    }

    public NpcRoleType role() {
        return role;
    }

    public void setRole(
            NpcRoleType role
    ) {
        this.role =
                Objects.requireNonNull(
                        role,
                        "role"
                );
    }

    public SimulationPosition homePosition() {
        return homePosition;
    }

    public void setHomePosition(
            SimulationPosition homePosition
    ) {
        this.homePosition =
                Objects.requireNonNull(
                        homePosition,
                        "homePosition"
                );
    }

    public SimulationPosition workPosition() {
        return workPosition;
    }

    public void setWorkPosition(
            SimulationPosition workPosition
    ) {
        this.workPosition =
                Objects.requireNonNull(
                        workPosition,
                        "workPosition"
                );
    }

    public int workStartTick() {
        return workStartTick;
    }

    public int workEndTick() {
        return workEndTick;
    }

    public void setWorkSchedule(
            int start,
            int end
    ) {
        validateDayTick(
                start
        );

        validateDayTick(
                end
        );

        if (start == end) {
            throw new IllegalArgumentException(
                    "Work start and end cannot be identical"
            );
        }

        this.workStartTick =
                start;

        this.workEndTick =
                end;
    }

    public boolean hasHome() {
        return homePosition != null;
    }

    public boolean hasWorkplace() {
        return workPosition != null;
    }

    /**
     * Supports both normal schedules:
     *
     * 6000 -> 12000
     *
     * and schedules crossing midnight:
     *
     * 18000 -> 2000
     */
    public boolean isWorkTime(
            long simulationTick
    ) {
        int tickOfDay =
                Math.floorMod(
                        simulationTick,
                        TICKS_PER_DAY
                );

        if (workStartTick
                < workEndTick) {

            return tickOfDay >= workStartTick
                    && tickOfDay < workEndTick;
        }

        return tickOfDay >= workStartTick
                || tickOfDay < workEndTick;
    }

    private static void validateDayTick(
            int tick
    ) {
        if (tick < 0
                || tick >= TICKS_PER_DAY) {

            throw new IllegalArgumentException(
                    "Day tick must be between 0 and 23999"
            );
        }
    }
}