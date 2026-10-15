
package dev.dtzudontsay.knownworld.simulation.economy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Batch 21.8.
 *
 * Explicit migration from the legacy inventory file
 * to the combined economic snapshot format.
 *
 * Not invoked automatically by EconomyRuntime.
 *
 * Requirements:
 * - The server must be stopped.
 * - No other process may write economic files.
 * - The legacy file remains authoritative.
 * - Original legacy bytes are never modified.
 */
public final class EconomyLegacyMigrator {

    public enum Outcome {
        MIGRATED,
        ALREADY_MATCHING
    }

    public record Result(
            Outcome outcome,
            int inventoryCount,
            long nextInventoryId,
            Path legacyFile,
            Path snapshotFile
    ) {
        public Result {
            Objects.requireNonNull(outcome);
            Objects.requireNonNull(legacyFile);
            Objects.requireNonNull(snapshotFile);
        }
    }

    private EconomyLegacyMigrator() {
    }

    public static Result migrate(
            Path directory,
            ResourceCatalog catalog
    ) throws IOException {

        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(catalog, "catalog");

        InventoryPersistence legacy =
                new InventoryPersistence(directory);

        EconomySnapshotStore snapshotStore =
                new EconomySnapshotStore(directory);

        EconomySnapshotRecovery recovery =
                new EconomySnapshotRecovery(directory);

        /*
         * A legacy save is mandatory.
         *
         * An empty world must not be converted into
         * an apparently initialized economy.
         */
        if (!Files.isRegularFile(legacy.file())) {
            throw new IOException(
                    "Cannot migrate: legacy economy save "
                            + "does not exist: " + legacy.file()
            );
        }

        /*
         * A backup without a primary represents an
         * ambiguous interrupted/recovery state.
         *
         * Do not create a new primary over it.
         */
        if (!Files.exists(snapshotStore.file())
                && Files.exists(recovery.backupFile())) {
            throw new IOException(
                    "Orphaned snapshot backup found. "
                            + "Manual recovery review required."
            );
        }

        EconomyMigrationAssessment.Report before =
                EconomyMigrationAssessment.assess(
                        directory, catalog
                );

        if (before.status()
                == EconomyMigrationAssessment.Status.MATCHING) {

            InventoryManager manager = loadLegacy(
                    legacy, catalog
            );

            return new Result(
                    Outcome.ALREADY_MATCHING,
                    manager.all().size(),
                    manager.nextId(),
                    legacy.file(),
                    snapshotStore.file()
            );
        }

        if (before.status()
                != EconomyMigrationAssessment.Status.LEGACY_ONLY) {
            throw new IOException(
                    "Migration refused: " + before.status()
                            + " - " + before.explanation()
            );
        }

        /*
         * Load the authoritative legacy balances.
         *
         * This creates a new manager whose journal
         * is empty, because old transaction history
         * is not present in the legacy format.
         */
        InventoryManager manager = loadLegacy(
                legacy, catalog
        );

        if (manager.transactionCount() != 0L) {
            throw new IOException(
                    "Legacy migration unexpectedly "
                            + "contains transaction history"
            );
        }

        byte[] originalLegacyBytes =
                Files.readAllBytes(legacy.file());

        EconomySnapshotStore.Snapshot snapshot =
                EconomySnapshotStore.capture(manager);

        /*
         * This call creates and validates the new
         * snapshot. The existing legacy save is not
         * a destination for any write operation.
         */
        recovery.save(snapshot, catalog);

        EconomyMigrationAssessment.Report after =
                EconomyMigrationAssessment.assess(
                        directory, catalog
                );

        if (after.status()
                != EconomyMigrationAssessment.Status.MATCHING) {
            throw new IOException(
                    "Post-migration verification failed: "
                            + after.status()
                            + " - " + after.explanation()
            );
        }

        /*
         * Re-read original source bytes after the
         * migration to detect unexpected modification.
         */
        if (!java.util.Arrays.equals(
                originalLegacyBytes,
                Files.readAllBytes(legacy.file())
        )) {
            throw new IOException(
                    "Legacy economy file changed "
                            + "during migration"
            );
        }

        EconomySnapshotStore.Snapshot verified =
                snapshotStore.read(catalog);

        if (!verified.transactions().isEmpty()) {
            throw new IOException(
                    "Migrated snapshot contains "
                            + "unexpected historical transactions"
            );
        }

        return new Result(
                Outcome.MIGRATED,
                manager.all().size(),
                manager.nextId(),
                legacy.file(),
                snapshotStore.file()
        );
    }

    private static InventoryManager loadLegacy(
            InventoryPersistence persistence,
            ResourceCatalog catalog
    ) throws IOException {

        /*
         * Legacy load returns an empty manager when
         * no file exists. The caller already requires
         * the file to exist; check once again before
         * trusting the result.
         */
        if (!Files.isRegularFile(persistence.file())) {
            throw new IOException(
                    "Legacy save disappeared during migration"
            );
        }

        return persistence.load(catalog);
    }
}
