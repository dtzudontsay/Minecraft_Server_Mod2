
package dev.dtzudontsay.knownworld.simulation.economy;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Batch 22.2 checkpoint validation.
 *
 * Verifies that a captured snapshot preserves the
 * authoritative manager balances and includes the
 * current session's journal as a suffix.
 *
 * Does not mutate or compact the journal.
 */
public final class EconomyCheckpointGuard {

    private EconomyCheckpointGuard() {
    }

    public static void verifyCaptured(
            InventoryManager manager,
            EconomySnapshotStore.Snapshot snapshot
    ) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(snapshot, "snapshot");

        synchronized (manager) {
            EconomySnapshotStore.Snapshot current =
                    EconomySnapshotStore.capture(manager);

            if (current.nextInventoryId()
                    != snapshot.nextInventoryId()) {
                throw new IllegalStateException(
                        "Checkpoint next-ID mismatch"
                );
            }

            if (!current.inventories()
                    .equals(snapshot.inventories())) {
                throw new IllegalStateException(
                        "Checkpoint inventory mismatch"
                );
            }

            /*
             * Snapshot v1 may contain historical
             * transactions before the current session.
             *
             * With retention, part of the current
             * session may have been compacted.
             *
             * Here we require exact preservation if
             * the whole session fits the snapshot.
             */
            List<EconomicTransaction> session =
                    current.transactions();

            if (session.size() > snapshot.transactions().size()) {
                throw new IllegalStateException(
                        "Checkpoint discarded current session records"
                );
            }

            int offset = snapshot.transactions().size()
                    - session.size();

            for (int i = 0; i < session.size(); i++) {
                EconomicTransaction expected = session.get(i);
                EconomicTransaction actual =
                        snapshot.transactions().get(offset + i);

                if (expected.kind() != actual.kind()
                        || !Objects.equals(
                        expected.source(), actual.source()
                )
                        || !Objects.equals(
                        expected.destination(), actual.destination()
                )
                        || !expected.resource().equals(actual.resource())
                        || expected.quantity() != actual.quantity()
                        || !expected.cause().equals(actual.cause())) {

                    throw new IllegalStateException(
                            "Checkpoint omitted or changed "
                                    + "a current-session transaction"
                    );
                }
            }
        }
    }

    /**
     * Verify a persisted snapshot matches the
     * in-memory captured snapshot.
     *
     * Does not compare current mutable manager state.
     */
    public static void verifyPersisted(
            EconomySnapshotStore.Snapshot expected,
            EconomySnapshotStore.Snapshot persisted
    ) throws IOException {

        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(persisted, "persisted");

        if (!expected.equals(persisted)) {
            throw new IOException(
                    "Persisted economic checkpoint differs "
                            + "from the captured snapshot"
            );
        }
    }
}
