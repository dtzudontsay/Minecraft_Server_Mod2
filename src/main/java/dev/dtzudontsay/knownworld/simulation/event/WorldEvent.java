package dev.dtzudontsay.knownworld.simulation.event;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;
import java.util.Optional;

/**
 * Objective event that occurred in the simulated world.
 *
 * This is different from a memory or belief.
 *
 * WorldEvent:
 *      what actually happened according to the simulation.
 *
 * NpcMemory:
 *      what one person remembers experiencing.
 *
 * NpcBelief:
 *      what one person currently thinks is true.
 */
public record WorldEvent(
        WorldEventId id,
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

    public WorldEvent {
        Objects.requireNonNull(
                id,
                "id"
        );

        Objects.requireNonNull(
                type,
                "type"
        );

        Objects.requireNonNull(
                summary,
                "summary"
        );

        Objects.requireNonNull(
                position,
                "position"
        );

        summary =
                summary.trim();

        if (summary.isEmpty()) {
            throw new IllegalArgumentException(
                    "Event summary cannot be empty"
            );
        }

        if (occurredTick < 0) {
            throw new IllegalArgumentException(
                    "occurredTick cannot be negative"
            );
        }

        if (!Double.isFinite(
                importance
        )
                || importance < 0.0
                || importance > 1.0) {

            throw new IllegalArgumentException(
                    "Event importance must be between 0.0 and 1.0"
            );
        }

        if (factKey != null) {
            factKey =
                    factKey.trim();

            if (factKey.isEmpty()) {
                factKey =
                        null;
            }
        }

        if (factValue != null) {
            factValue =
                    factValue.trim();

            if (factValue.isEmpty()) {
                factValue =
                        null;
            }
        }

        if ((factKey == null)
                != (factValue == null)) {

            throw new IllegalArgumentException(
                    "factKey and factValue must either both exist or both be null"
            );
        }
    }

    public Optional<NpcId> actorNpcOptional() {
        return Optional.ofNullable(
                actorNpc
        );
    }

    public Optional<NpcId> subjectNpcOptional() {
        return Optional.ofNullable(
                subjectNpc
        );
    }

    public Optional<String> factKeyOptional() {
        return Optional.ofNullable(
                factKey
        );
    }

    public Optional<String> factValueOptional() {
        return Optional.ofNullable(
                factValue
        );
    }

    public boolean containsFact() {
        return factKey != null
                && factValue != null;
    }
}