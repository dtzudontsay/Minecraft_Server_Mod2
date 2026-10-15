
package dev.dtzudontsay.knownworld.simulation.economy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Batch 22.1.
 *
 * Authoritative save selection plus bounded
 * cross-session transaction continuity.
 *
 * Legacy saves remain authoritative unless
 * snapshot mode was explicitly activated.
 */
public final class EconomyAuthorityService {

    public enum Mode {
        LEGACY_COMPATIBILITY,
        SNAPSHOT_PRIMARY
    }

    public record Loaded(
            Mode mode,
            InventoryManager manager,
            String description,
            List<EconomicTransaction> previousTransactions
    ) {
        public Loaded {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(manager, "manager");
            Objects.requireNonNull(description, "description");
            previousTransactions = List.copyOf(
                    previousTransactions
            );
        }

        /**
         * Preserve the old three-argument constructor
         * for existing callers and tests.
         */
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

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        /*
         * A corrupted or missing primary does not
         * trigger automatic fallback to a backup.
         */
        EconomySnapshotStore.Snapshot snapshot =
                store.read(catalog);

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
     * Explicit offline activation of snapshot authority.
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
     * Save using the active authoritative format.
     *
     * In snapshot mode, combine previously persisted
     * records and this session's journal.
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

            EconomySnapshotRecovery recovery =
                    new EconomySnapshotRecovery(directory);

            EconomySnapshotStore.Snapshot snapshot =
                    EconomyJournalContinuity.capture(
                            loaded.manager(),
                            loaded.previousTransactions()
                    );

            recovery.save(snapshot, catalog);
            return;
        }

        if (marker.exists()) {
            throw new IOException(
                    "Authority changed during a legacy session"
            );
        }

        /*
         * Preserve the Batch 21.9 restriction.
         *
         * Matched legacy and snapshot files must
         * not silently diverge.
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

        persistence.save(
                loaded.manager()
        );
    }
}
