
package dev.dtzudontsay.knownworld.simulation.economy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Batch 22.3.
 *
 * Creates, validates, publishes and verifies one
 * combined economic checkpoint.
 *
 * Journal entries are NOT cleared by this class.
 *
 * A current session that does not fit completely
 * into the retained snapshot history is rejected
 * before any snapshot file is written.
 */
public final class EconomySnapshotCommitter {

    private EconomySnapshotCommitter() {
    }

    public static EconomySnapshotStore.Snapshot commit(
            Path directory,
            ResourceCatalog catalog,
            InventoryManager manager,
            List<EconomicTransaction> previousTransactions
    ) throws IOException {

        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(
                previousTransactions, "previousTransactions"
        );

        synchronized (manager) {
            int currentCount = manager.transactions().size();

            if (currentCount >
                    EconomyJournalContinuity
                            .DEFAULT_RETAINED_TRANSACTIONS) {

                throw new IOException(
                        "Economic checkpoint rejected: "
                                + currentCount
                                + " current-session transactions exceed "
                                + "snapshot retention capacity "
                                + EconomyJournalContinuity
                                .DEFAULT_RETAINED_TRANSACTIONS
                                + ". No journal entries were discarded."
                );
            }

            EconomySnapshotStore.Snapshot captured;

            try {
                captured = EconomyJournalContinuity.capture(
                        manager,
                        previousTransactions
                );

                EconomyCheckpointGuard.verifyCaptured(
                        manager, captured
                );
            } catch (RuntimeException exception) {
                throw new IOException(
                        "Economic checkpoint validation failed",
                        exception
                );
            }

            /*
             * The recovery store first validates the
             * previous primary, preserves it as a
             * backup where applicable, and atomically
             * publishes the new primary.
             */
            EconomySnapshotRecovery recovery =
                    new EconomySnapshotRecovery(directory);

            recovery.save(captured, catalog);

            /*
             * Verify the actual published primary.
             * Successful publication alone is not
             * considered proof of valid contents.
             */
            EconomySnapshotStore.Snapshot persisted =
                    new EconomySnapshotStore(directory)
                            .read(catalog);

            EconomyCheckpointGuard.verifyPersisted(
                    captured,
                    persisted
            );

            return persisted;
        }
    }
}
