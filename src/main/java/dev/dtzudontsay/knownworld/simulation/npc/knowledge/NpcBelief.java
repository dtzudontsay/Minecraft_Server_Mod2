package dev.dtzudontsay.knownworld.simulation.npc.knowledge;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;
import java.util.Optional;

/**
 * Something one NPC currently believes.
 *
 * A belief is NOT automatically objective truth.
 *
 * Example:
 *
 * npc #12 may believe:
 *
 *      "army:lannister:destination"
 *      =
 *      "riverrun"
 *
 * with confidence 0.7
 *
 * even if the army is actually marching somewhere else.
 */
public record NpcBelief(
        NpcId owner,
        String factKey,
        String value,
        double confidence,
        NpcId sourceNpc,
        long learnedTick
) {

    public NpcBelief {
        Objects.requireNonNull(
                owner,
                "owner"
        );

        Objects.requireNonNull(
                factKey,
                "factKey"
        );

        Objects.requireNonNull(
                value,
                "value"
        );

        factKey =
                factKey.trim();

        if (factKey.isEmpty()) {
            throw new IllegalArgumentException(
                    "factKey cannot be empty"
            );
        }

        if (!Double.isFinite(confidence)
                || confidence < 0.0
                || confidence > 1.0) {

            throw new IllegalArgumentException(
                    "confidence must be between 0.0 and 1.0"
            );
        }

        if (learnedTick < 0) {
            throw new IllegalArgumentException(
                    "learnedTick cannot be negative"
            );
        }
    }

    public Optional<NpcId> sourceNpcOptional() {
        return Optional.ofNullable(
                sourceNpc
        );
    }
}