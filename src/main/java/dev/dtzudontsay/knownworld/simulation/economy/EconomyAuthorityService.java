
package dev.dtzudontsay.knownworld.simulation.economy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Batch 22.4.
 *
 * Economy authority selection and verified journal
 * checkpoint rotation.
 *
 * Legacy authority remains backward compatible.
 * Snapshot authority requires explicit activation.
 */
public final class EconomyAuthorityService {

    public enum Mode {
        LEGACY_COMPATIBILITY,
        SNAPSHOT_PRIMARY
    }

    /**
     * The current economy session.
     *
     * Unlike the former record, its committed journal
     * baseline may advance after verified saves.
     *
     * Callers must not replace the InventoryManager.
     */
    public static final class Loaded {

        private final Mode mode;
        private final InventoryManager manager;
        private final String description;

        private List<EconomicTransaction> previousTransactions;

        public Loaded(
                Mode mode,
                InventoryManager manager,
                String description,
                List<EconomicTransaction> previousTransactions
        ) {
            this.mode = Objects.requireNonNull(
                    mode, "mode"
            );

            this.manager = Objects.requireNonNull(
                    manager, "manager"
            );

            this.description = Objects.requireNonNull(
                    description, "description"
            );

            this.previousTransactions = List.copyOf(
                    previousTransactions
            );
        }

        public Loaded(
                Mode mode,
                InventoryManager manager,
                String description
        ) {
            this(
                    mode,
                    manager,
                    description,
                    List.of()
            );
        }

        public Mode mode() {
            return mode;
        }

        public InventoryManager manager() {
            return manager;
        }

        public String description() {
            return description;
        }

        /**
         * Immutable copy of the latest committed
         * historical transaction baseline.
         */
        public List<EconomicTransaction> previousTransactions() {
            synchronized (manager) {
                return previousTransactions;
            }
        }

        /**
         * Called under the manager lock, and only
         * after a successful verified snapshot commit.
         */
        private void advanceCommittedBaseline(
                List<EconomicTransaction> committed
        ) {
            if (!Thread.holdsLock(manager)) {
                throw new IllegalStateException(
                        "Committed baseline update requires "
                                + "the inventory manager monitor"
                );
            }

            previousTransactions = List.copyOf(committed);
        }
    }

    private EconomyAuthorityService() {
    }

    public static Loaded load(
            Path directory,
            ResourceCatalog catalog
    ) throws IOException {

        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(catalog, "catalog");

        EconomyAuthorityMarker marker =
                new EconomyAuthorityMarker(directory);

        if (Files.exists(directory.resolve(
                "economy_authority.tsv.tmp"
        ))) {
            throw new IOException(
                    "Interrupted authority-marker operation detected"
            );
        }

        if (!marker.exists()) {
            EconomySaveSelector.Result selected =
                    EconomySaveSelector.load(
                            directory,
                            catalog
                    );

            return new Loaded(
                    Mode.LEGACY_COMPATIBILITY,
                    selected.manager(),
                    selected.selection().name()
            );
        }

        marker.read();

        /*
         * Do not automatically recover from an
         * invalid primary snapshot.
         */
        EconomySnapshotStore.Snapshot snapshot =
                new EconomySnapshotStore(directory)
                        .read(catalog);

        InventoryManager manager =
                EconomySnapshotStore.restore(
                        snapshot,
                        catalog
                );

        return new Loaded(
                Mode.SNAPSHOT_PRIMARY,
                manager,
                "EXPLICIT_SNAPSHOT_AUTHORITY",
                snapshot.transactions()
        );
    }

    /**
     * Explicit offline snapshot-authority activation.
     *
     * The caller must stop the server and back up
     * the complete world before activation.
     */
    public static void activateMatchingSnapshot(
            Path directory,
            ResourceCatalog catalog
    ) throws IOException {

        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(catalog, "catalog");

        EconomyAuthorityMarker marker =
                new EconomyAuthorityMarker(directory);

        if (marker.exists()) {
            throw new IOException(
                    "Snapshot authority already selected"
            );
        }

        EconomySaveSelector.Result selected =
                EconomySaveSelector.load(
                        directory,
                        catalog
                );

        if (selected.selection()
                != EconomySaveSelector.Selection
                .LEGACY_WITH_MATCHING_SNAPSHOT) {
            throw new IOException(
                    "Activation requires matching "
                            + "legacy and snapshot saves"
            );
        }

        EconomySnapshotStore.Snapshot snapshot =
                new EconomySnapshotStore(directory)
                        .read(catalog);

        if (snapshot.nextInventoryId()
                != selected.manager().nextId()) {
            throw new IOException(
                    "Snapshot next-ID mismatch"
            );
        }

        marker.activateSnapshot();
    }

    /**
     * Save through the selected authoritative format.
     *
     * In snapshot-primary mode:
     *
     * 1. Hold the manager lock.
     * 2. Capture pending journal records.
     * 3. Commit and verify the snapshot.
     * 4. Rotate exactly those committed records.
     * 5. Advance the committed historical baseline.
     *
     * A failed commit leaves the journal and baseline
     * unchanged.
     */
    public static void save(
            Path directory,
            ResourceCatalog catalog,
            Loaded loaded
    ) throws IOException {

        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(loaded, "loaded");

        EconomyAuthorityMarker marker =
                new EconomyAuthorityMarker(directory);

        if (loaded.mode() == Mode.SNAPSHOT_PRIMARY) {

            if (!marker.exists()) {
                throw new IOException(
                        "Snapshot authority marker disappeared"
                );
            }

            marker.read();

            InventoryManager manager = loaded.manager();

            synchronized (manager) {
                if (manager.journalGeneration()
                        == Long.MAX_VALUE) {
                    throw new IOException(
                            "Journal generation space exhausted"
                    );
                }

                /*
                 * Exact journal state that this
                 * checkpoint must include.
                 */
                List<EconomicTransaction> pending =
                        manager.transactions();

                /*
                 * This method performs a verified
                 * snapshot commit and throws if
                 * publication or verification fails.
                 *
                 * The current journal is not changed
                 * by the committer itself.
                 */
                EconomySnapshotStore.Snapshot committed =
                        EconomySnapshotCommitter.commit(
                                directory,
                                catalog,
                                manager,
                                loaded.previousTransactions()
                        );

                /*
                 * The manager monitor has been held
                 * continuously since pending was read.
                 *
                 * Thus no other thread can append an
                 * economic transaction during commit.
                 *
                 * Move the committed baseline forward
                 * and rotate the exactly matched journal.
                 */
                manager.rotateCommittedJournal(pending);

                loaded.advanceCommittedBaseline(
                        committed.transactions()
                );
            }

            return;
        }

        if (marker.exists()) {
            throw new IOException(
                    "Authority changed during a legacy session"
            );
        }

        /*
         * Preserve the existing no-divergence rule:
         * matching legacy and snapshot files are
         * read-only until snapshot activation.
         */
        if ("LEGACY_WITH_MATCHING_SNAPSHOT".equals(
                loaded.description()
        )) {
            return;
        }

        InventoryPersistence persistence =
                new InventoryPersistence(directory);

        if (!persistence.exists()
                && loaded.manager().all().isEmpty()) {
            return;
        }

        persistence.save(loaded.manager());
    }
}
