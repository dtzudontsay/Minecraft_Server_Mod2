package dev.dtzudontsay.knownworld.simulation.npc.goal;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;
import java.util.Optional;

/**
 * Persistent intention belonging to one NPC.
 *
 * A goal describes what the NPC wants to accomplish.
 *
 * It is NOT the same as the action currently being performed.
 */
public final class NpcGoal {

    private final NpcGoalId id;

    private final NpcId owner;

    private final NpcGoalType type;

    private final String description;

    private final double priority;

    private final long createdTick;

    private final NpcId targetNpc;

    private final SimulationPosition targetPosition;

    private final String factKey;

    private NpcGoalStatus status;

    public NpcGoal(
            NpcGoalId id,
            NpcId owner,
            NpcGoalType type,
            String description,
            double priority,
            long createdTick,
            NpcId targetNpc,
            SimulationPosition targetPosition,
            String factKey,
            NpcGoalStatus status
    ) {
        this.id =
                Objects.requireNonNull(
                        id,
                        "id"
                );

        this.owner =
                Objects.requireNonNull(
                        owner,
                        "owner"
                );

        this.type =
                Objects.requireNonNull(
                        type,
                        "type"
                );

        Objects.requireNonNull(
                description,
                "description"
        );

        this.description =
                description.trim();

        if (this.description.isEmpty()) {
            throw new IllegalArgumentException(
                    "Goal description cannot be empty"
            );
        }

        if (!Double.isFinite(
                priority
        )
                || priority < 0.0
                || priority > 1.0) {

            throw new IllegalArgumentException(
                    "Goal priority must be between 0.0 and 1.0"
            );
        }

        if (createdTick < 0) {
            throw new IllegalArgumentException(
                    "createdTick cannot be negative"
            );
        }

        this.priority =
                priority;

        this.createdTick =
                createdTick;

        this.targetNpc =
                targetNpc;

        this.targetPosition =
                targetPosition;

        if (factKey == null
                || factKey.isBlank()) {

            this.factKey =
                    null;

        } else {

            this.factKey =
                    factKey.trim();
        }

        this.status =
                Objects.requireNonNull(
                        status,
                        "status"
                );
    }

    public NpcGoalId id() {
        return id;
    }

    public NpcId owner() {
        return owner;
    }

    public NpcGoalType type() {
        return type;
    }

    public String description() {
        return description;
    }

    public double priority() {
        return priority;
    }

    public long createdTick() {
        return createdTick;
    }

    public NpcGoalStatus status() {
        return status;
    }

    public NpcId targetNpc() {
        return targetNpc;
    }

    public Optional<NpcId> targetNpcOptional() {
        return Optional.ofNullable(
                targetNpc
        );
    }

    public SimulationPosition targetPosition() {
        return targetPosition;
    }

    public Optional<SimulationPosition> targetPositionOptional() {
        return Optional.ofNullable(
                targetPosition
        );
    }

    public String factKey() {
        return factKey;
    }

    public Optional<String> factKeyOptional() {
        return Optional.ofNullable(
                factKey
        );
    }

    public boolean isTerminal() {
        return status == NpcGoalStatus.COMPLETED
                || status == NpcGoalStatus.FAILED
                || status == NpcGoalStatus.CANCELLED;
    }

    public void activate() {

        if (status == NpcGoalStatus.PENDING) {
            status =
                    NpcGoalStatus.ACTIVE;
        }
    }

    public void complete() {

        if (!isTerminal()) {
            status =
                    NpcGoalStatus.COMPLETED;
        }
    }

    public void fail() {

        if (!isTerminal()) {
            status =
                    NpcGoalStatus.FAILED;
        }
    }

    public void cancel() {

        if (!isTerminal()) {
            status =
                    NpcGoalStatus.CANCELLED;
        }
    }
}