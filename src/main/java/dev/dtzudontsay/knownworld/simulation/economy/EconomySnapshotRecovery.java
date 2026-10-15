
package dev.dtzudontsay.knownworld.simulation.economy;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Batch 21.6.
 *
 * Manages one previous validated snapshot generation.
 *
 * The primary snapshot remains managed by
 * EconomySnapshotStore.
 *
 * Recovery never silently changes a damaged primary.
 *
 * This is an isolated persistence component, not yet
 * connected to EconomyRuntime.
 */
public final class EconomySnapshotRecovery {

    public enum Source {
        PRIMARY,
        BACKUP
    }

    public record RecoveryResult(
            EconomySnapshotStore.Snapshot snapshot,
            Source source
    ) {
        public RecoveryResult {
            Objects.requireNonNull(snapshot, "snapshot");
            Objects.requireNonNull(source, "source");
        }
    }

    private final EconomySnapshotStore primary;
    private final Path backupFile;
    private final Path backupTemp;

    public EconomySnapshotRecovery(Path directory) {
        Objects.requireNonNull(directory, "directory");

        this.primary = new EconomySnapshotStore(directory);

        this.backupFile = directory.resolve(
                "economy_snapshot_v1.tsv.bak"
        );

        this.backupTemp = directory.resolve(
                "economy_snapshot_v1.tsv.bak.tmp"
        );
    }

    public Path primaryFile() {
        return primary.file();
    }

    public Path backupFile() {
        return backupFile;
    }

    /**
     * Writes a new primary snapshot.
     *
     * If a primary already exists, it must be valid.
     * The valid old primary is backed up before the
     * new primary is written.
     *
     * A corrupt primary is never overwritten here.
     */
    public synchronized void save(
            EconomySnapshotStore.Snapshot snapshot,
            ResourceCatalog catalog
    ) throws IOException {

        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(catalog, "catalog");

        if (Files.exists(primary.file())) {
            // Fail closed on corrupt existing snapshots.
            primary.read(catalog);

            atomicCopy(
                    primary.file(),
                    backupTemp,
                    backupFile
            );
        }

        primary.write(snapshot);

        // Verify the final file, not only the input data.
        primary.read(catalog);
    }

    /**
     * Load a validated snapshot.
     *
     * If primary validation fails, a valid backup can
     * be returned without altering either file.
     *
     * The Source field makes fallback observable.
     */
    public synchronized RecoveryResult readBest(
            ResourceCatalog catalog
    ) throws IOException {

        Objects.requireNonNull(catalog, "catalog");

        IOException primaryFailure;

        try {
            return new RecoveryResult(
                    primary.read(catalog),
                    Source.PRIMARY
            );
        } catch (IOException exception) {
            primaryFailure = exception;
        }

        if (!Files.isRegularFile(backupFile)) {
            throw new IOException(
                    "Primary economy snapshot unavailable "
                            + "and no recovery backup exists",
                    primaryFailure
            );
        }

        try {
            return new RecoveryResult(
                    readBackup(catalog),
                    Source.BACKUP
            );
        } catch (IOException backupFailure) {
            IOException failure = new IOException(
                    "Both primary and backup economy "
                            + "snapshots are invalid",
                    primaryFailure
            );

            failure.addSuppressed(backupFailure);

            throw failure;
        }
    }

    /**
     * Restore backup bytes to the primary file.
     *
     * Only call after the operator has chosen recovery.
     *
     * This operation can overwrite a corrupt primary;
     * preserve a forensic copy before invoking it.
     */
    public synchronized void restoreBackupAsPrimary(
            ResourceCatalog catalog
    ) throws IOException {

        Objects.requireNonNull(catalog, "catalog");

        // Verify backup before modifying the primary.
        readBackup(catalog);

        Path temporary = primary.file().resolveSibling(
                "economy_snapshot_v1.tsv.recovery.tmp"
        );

        atomicCopy(
                backupFile,
                temporary,
                primary.file()
        );

        primary.read(catalog);
    }

    private EconomySnapshotStore.Snapshot readBackup(
            ResourceCatalog catalog
    ) throws IOException {

        // EconomySnapshotStore uses a fixed filename.
        // Validate a temporary read-only directory copy
        // rather than changing the original file.
        //
        // This method intentionally does not overwrite
        // the primary or the backup.
        Path validationDir = Files.createTempDirectory(
                "knownworld-economy-verify-"
        );

        Path validationFile = validationDir.resolve(
                "economy_snapshot_v1.tsv"
        );

        try {
            Files.copy(
                    backupFile,
                    validationFile
            );

            return new EconomySnapshotStore(
                    validationDir
            ).read(catalog);

        } finally {
            Files.deleteIfExists(validationFile);
            Files.deleteIfExists(validationDir);
        }
    }

    /**
     * Atomically publish a copied file.
     *
     * Existing destination remains unchanged if
     * the atomic move is unsupported.
     */
    private static void atomicCopy(
            Path source,
            Path temporary,
            Path destination
    ) throws IOException {

        Files.createDirectories(
                destination.getParent()
        );

        try {
            Files.copy(
                    source,
                    temporary,
                    StandardCopyOption.REPLACE_EXISTING
            );

            try {
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ex) {
                throw new IOException(
                        "Atomic snapshot recovery "
                                + "replacement unsupported",
                        ex
                );
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
