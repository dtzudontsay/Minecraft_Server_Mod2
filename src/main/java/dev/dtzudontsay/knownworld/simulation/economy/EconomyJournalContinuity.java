
package dev.dtzudontsay.knownworld.simulation.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Batch 22.1.
 *
 * Combines previously persisted transactions with
 * transactions from the current manager session.
 *
 * The resulting journal is bounded and renumbered
 * starting at sequence 1 for Snapshot format v1.
 *
 * Compacted transactions cannot be recovered from
 * this bounded snapshot history.
 */
public final class EconomyJournalContinuity {

    public static final int DEFAULT_RETAINED_TRANSACTIONS =
            10_000;

    private EconomyJournalContinuity() {
    }

    public static List<EconomicTransaction> merge(
            List<EconomicTransaction> previous,
            List<EconomicTransaction> current,
            int retentionLimit
    ) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");

        if (retentionLimit <= 0) {
            throw new IllegalArgumentException(
                    "Retention limit must be positive"
            );
        }

        validateSequences(previous);
        validateSequences(current);

        /*
         * Retain references to immutable transaction
         * records without combining sequence spaces.
         */
        int previousStart = Math.max(
                0,
                previous.size() - retentionLimit
        );

        long spaceForPrevious =
                Math.max(0L,
                        (long) retentionLimit - current.size());

        previousStart = (int) Math.max(
                previousStart,
                previous.size() - spaceForPrevious
        );

        int currentStart = Math.max(
                0,
                current.size() - retentionLimit
        );

        List<EconomicTransaction> selected =
                new ArrayList<>();

        for (int i = previousStart;
             i < previous.size(); i++) {
            selected.add(previous.get(i));
        }

        for (int i = currentStart;
             i < current.size(); i++) {
            selected.add(current.get(i));
        }

        if (selected.size() > retentionLimit) {
            selected = new ArrayList<>(
                    selected.subList(
                            selected.size() - retentionLimit,
                            selected.size()
                    )
            );
        }

        List<EconomicTransaction> result =
                new ArrayList<>(selected.size());

        long sequence = 1L;

        for (EconomicTransaction transaction : selected) {
            result.add(new EconomicTransaction(
                    sequence++,
                    transaction.kind(),
                    transaction.source(),
                    transaction.destination(),
                    transaction.resource(),
                    transaction.quantity(),
                    transaction.cause()
            ));
        }

        return List.copyOf(result);
    }

    public static List<EconomicTransaction> merge(
            List<EconomicTransaction> previous,
            List<EconomicTransaction> current
    ) {
        return merge(
                previous,
                current,
                DEFAULT_RETAINED_TRANSACTIONS
        );
    }

    public static EconomySnapshotStore.Snapshot capture(
            InventoryManager manager,
            List<EconomicTransaction> previous,
            int retentionLimit
    ) {
        Objects.requireNonNull(manager, "manager");

        /*
         * Inventory balances and current-session
         * journal are captured under one manager lock.
         */
        EconomySnapshotStore.Snapshot current =
                EconomySnapshotStore.capture(manager);

        return new EconomySnapshotStore.Snapshot(
                current.nextInventoryId(),
                current.inventories(),
                merge(
                        previous,
                        current.transactions(),
                        retentionLimit
                )
        );
    }

    public static EconomySnapshotStore.Snapshot capture(
            InventoryManager manager,
            List<EconomicTransaction> previous
    ) {
        return capture(
                manager,
                previous,
                DEFAULT_RETAINED_TRANSACTIONS
        );
    }

    private static void validateSequences(
            List<EconomicTransaction> transactions
    ) {
        long expected = 1L;

        for (EconomicTransaction transaction :
                transactions) {

            Objects.requireNonNull(
                    transaction,
                    "transaction"
            );

            if (transaction.sequence() != expected++) {
                throw new IllegalArgumentException(
                        "Transaction sequences must be "
                                + "consecutive starting at 1"
                );
            }
        }
    }
}
