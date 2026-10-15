
package dev.dtzudontsay.knownworld.simulation.economy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Batch 21.9.
 *
 * Selects the authoritative economy save without
 * modifying save files or performing migration.
 *
 * Legacy inventory persistence remains authoritative.
 *
 * Snapshot-only, conflicting and invalid states
 * require operator intervention.
 */
public final class EconomySaveSelector {

    public enum Selection {
        NEW_EMPTY,
        LEGACY,
        LEGACY_WITH_MATCHING_SNAPSHOT
    }

    public record Result(
            Selection selection,
            InventoryManager manager,
            EconomyMigrationAssessment.Report assessment
    ) {
        public Result {
            Objects.requireNonNull(selection, "selection");
            Objects.requireNonNull(manager, "manager");
            Objects.requireNonNull(assessment, "assessment");
        }
    }

    private EconomySaveSelector() {
    }

    public static Result load(
            Path directory,
            ResourceCatalog catalog
    ) throws IOException {

        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(catalog, "catalog");

        InventoryPersistence legacy =
                new InventoryPersistence(directory);

        EconomySnapshotStore snapshot =
                new EconomySnapshotStore(directory);

        /*
         * Check for orphaned recovery artifacts.
         *
         * A backup without a primary may indicate
         * an interrupted recovery or manual change.
         */
        Path backup = directory.resolve(
                "economy_snapshot_v1.tsv.bak"
        );

        Path primary = snapshot.file();

        if (Files.exists(backup) && !Files.exists(primary)) {
            throw new IOException(
                    "Orphaned economy snapshot backup: "
                            + backup
            );
        }

        /*
         * Temporary files may represent interrupted
         * writes. Refuse to decide their fate here.
         */
        if (Files.exists(directory.resolve(
                "economy_inventories.tsv.tmp"
        )) || Files.exists(directory.resolve(
                "economy_snapshot_v1.tsv.tmp"
        )) || Files.exists(directory.resolve(
                "economy_snapshot_v1.tsv.bak.tmp"
        )) || Files.exists(directory.resolve(
                "economy_snapshot_v1.tsv.recovery.tmp"
        ))) {
            throw new IOException(
                    "Unresolved temporary economy save files "
                            + "require manual inspection"
            );
        }

        EconomyMigrationAssessment.Report assessment =
                EconomyMigrationAssessment.assess(
                        directory, catalog
                );

        return switch (assessment.status()) {
            case EMPTY_WORLD -> new Result(
                    Selection.NEW_EMPTY,
                    new InventoryManager(catalog),
                    assessment
            );

            case LEGACY_ONLY -> new Result(
                    Selection.LEGACY,
                    requireLegacy(legacy, catalog),
                    assessment
            );

            case MATCHING -> new Result(
                    Selection.LEGACY_WITH_MATCHING_SNAPSHOT,
                    requireLegacy(legacy, catalog),
                    assessment
            );

            case SNAPSHOT_ONLY, CONFLICT, INVALID_DATA ->
                    throw new IOException(
                            "Automatic economy save selection "
                                    + "refused: " + assessment.status()
                                    + " - " + assessment.explanation()
                    );
        };
    }

    private static InventoryManager requireLegacy(
            InventoryPersistence persistence,
            ResourceCatalog catalog
    ) throws IOException {

        if (!Files.isRegularFile(persistence.file())) {
            throw new IOException(
                    "Legacy economy save disappeared "
                            + "during selection"
            );
        }

        return persistence.load(catalog);
    }
}
