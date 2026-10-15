
package dev.dtzudontsay.knownworld.simulation.economy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Batch 21.7.
 *
 * Read-only preflight for migrating legacy inventory
 * persistence to the combined snapshot format.
 *
 * Does not write, migrate, repair or delete files.
 */
public final class EconomyMigrationAssessment {

    public enum Status {
        EMPTY_WORLD,
        LEGACY_ONLY,
        SNAPSHOT_ONLY,
        MATCHING,
        CONFLICT,
        INVALID_DATA
    }

    public record Report(
            Status status,
            int legacyInventories,
            int snapshotInventories,
            String explanation
    ) {
        public Report {
            Objects.requireNonNull(status);
            Objects.requireNonNull(explanation);
        }

        public boolean safeToProceedAutomatically() {
            return status == Status.EMPTY_WORLD
                    || status == Status.MATCHING;
        }
    }

    private EconomyMigrationAssessment() {
    }

    public static Report assess(
            Path directory,
            ResourceCatalog catalog
    ) {
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(catalog, "catalog");

        InventoryPersistence legacy =
                new InventoryPersistence(directory);

        EconomySnapshotStore snapshot =
                new EconomySnapshotStore(directory);

        boolean legacyExists = legacy.exists();

        boolean snapshotExists =
                java.nio.file.Files.exists(snapshot.file());

        if (!legacyExists && !snapshotExists) {
            return new Report(
                    Status.EMPTY_WORLD, 0, 0,
                    "Neither save format exists"
            );
        }

        InventoryManager oldManager = null;
        EconomySnapshotStore.Snapshot newSnapshot = null;

        try {
            if (legacyExists) {
                oldManager = legacy.load(catalog);
            }

            if (snapshotExists) {
                newSnapshot = snapshot.read(catalog);
            }
        } catch (IOException | RuntimeException exception) {
            return new Report(
                    Status.INVALID_DATA, -1, -1,
                    "Preflight rejected saved data: "
                            + exception.getMessage()
            );
        }

        if (legacyExists && !snapshotExists) {
            return new Report(
                    Status.LEGACY_ONLY,
                    oldManager.all().size(), 0,
                    "Legacy save requires explicit migration"
            );
        }

        if (!legacyExists) {
            return new Report(
                    Status.SNAPSHOT_ONLY,
                    0, newSnapshot.inventories().size(),
                    "Only the combined snapshot exists"
            );
        }

        boolean same =
                canonical(oldManager).equals(
                        canonical(newSnapshot)
                )
                        && oldManager.nextId()
                        == newSnapshot.nextInventoryId();

        return new Report(
                same ? Status.MATCHING : Status.CONFLICT,
                oldManager.all().size(),
                newSnapshot.inventories().size(),
                same
                        ? "Inventory data and next ID match"
                        : "Save formats disagree; manual review required"
        );
    }

    private static Map<InventoryId, String> canonical(
            InventoryManager manager
    ) {
        Map<InventoryId, String> result = new TreeMap<>();

        for (ResourceInventory inventory : manager.all()) {
            result.put(
                    inventory.id(),
                    encode(
                            inventory.owner(),
                            inventory.location().value(),
                            inventory.balances()
                    )
            );
        }

        return result;
    }

    private static Map<InventoryId, String> canonical(
            EconomySnapshotStore.Snapshot snapshot
    ) {
        Map<InventoryId, String> result = new TreeMap<>();

        for (var inventory : snapshot.inventories()) {
            result.put(
                    inventory.id(),
                    encode(
                            inventory.owner(),
                            inventory.location().value(),
                            inventory.balances()
                    )
            );
        }

        return result;
    }

    private static String encode(
            EconomicOwner owner,
            long location,
            Map<ResourceId, Long> balances
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append(owner.kind())
                .append(':')
                .append(owner.id())
                .append('@')
                .append(location);

        new TreeMap<>(balances).forEach(
                (resource, quantity) ->
                        builder.append('|')
                                .append(resource.value())
                                .append('=')
                                .append(quantity)
        );

        return builder.toString();
    }
}
