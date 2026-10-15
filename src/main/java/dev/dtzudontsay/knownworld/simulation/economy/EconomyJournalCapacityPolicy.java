
package dev.dtzudontsay.knownworld.simulation.economy;

import java.util.Objects;

/**
 * Determines when the economy needs a durable
 * checkpoint before continuing to accumulate
 * transaction history.
 *
 * This policy is advisory in Batch 21.7.
 * It does not discard journal entries.
 */
public record EconomyJournalCapacityPolicy(
        int warningThreshold,
        int rotationThreshold
) {

    public enum State {
        NORMAL,
        WARNING,
        ROTATION_REQUIRED
    }

    public EconomyJournalCapacityPolicy {
        if (warningThreshold <= 0
                || rotationThreshold <= warningThreshold) {
            throw new IllegalArgumentException(
                    "Require 0 < warning < rotation"
            );
        }
    }

    public static EconomyJournalCapacityPolicy defaults() {
        return new EconomyJournalCapacityPolicy(
                50_000,
                100_000
        );
    }

    public State evaluate(InventoryManager manager) {
        Objects.requireNonNull(manager, "manager");

        return evaluate(manager.transactionCount());
    }

    public State evaluate(long count) {
        if (count < 0L) {
            throw new IllegalArgumentException(
                    "Negative transaction count"
            );
        }

        if (count >= rotationThreshold) {
            return State.ROTATION_REQUIRED;
        }

        if (count >= warningThreshold) {
            return State.WARNING;
        }

        return State.NORMAL;
    }

    public void requireCapacityForNext(long count) {
        if (count < 0L) {
            throw new IllegalArgumentException(
                    "Negative transaction count"
            );
        }

        if (count >= rotationThreshold) {
            throw new IllegalStateException(
                    "Durable economy checkpoint rotation required"
            );
        }
    }
}
