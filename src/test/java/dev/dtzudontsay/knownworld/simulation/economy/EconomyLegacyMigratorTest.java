
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public final class EconomyLegacyMigratorTest {

    @TempDir
    Path directory;

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private static final ResourceId COIN =
            new ResourceId("coin");

    private ResourceCatalog catalog() {
        return ResourceCatalog.benchmarkDefaults();
    }

    private InventoryManager populated() {
        InventoryManager manager =
                new InventoryManager(catalog());

        ResourceInventory farm = manager.create(
                EconomicOwner.npc(new NpcId(1L)),
                new SettlementId(1L)
        );

        ResourceInventory treasury = manager.create(
                EconomicOwner.settlement(
                        new SettlementId(1L)
                ),
                new SettlementId(1L)
        );

        manager.produce(
                farm.id(), GRAIN, 600L, "harvest"
        );

        manager.transfer(
                farm.id(),
                treasury.id(),
                GRAIN,
                200L,
                "rent"
        );

        manager.produce(
                treasury.id(),
                COIN,
                75L,
                "benchmark_initialization"
        );

        return manager;
    }

    @Test
    void migratesLegacyBalancesWithoutChangingSource()
            throws IOException {

        InventoryManager manager = populated();

        InventoryPersistence legacy =
                new InventoryPersistence(directory);

        legacy.save(manager);

        byte[] before = Files.readAllBytes(
                legacy.file()
        );

        EconomyLegacyMigrator.Result result =
                EconomyLegacyMigrator.migrate(
                        directory, catalog()
                );

        assertEquals(
                EconomyLegacyMigrator.Outcome.MIGRATED,
                result.outcome()
        );

        assertArrayEquals(
                before,
                Files.readAllBytes(legacy.file())
        );

        EconomySnapshotStore.Snapshot snapshot =
                new EconomySnapshotStore(directory)
                        .read(catalog());

        InventoryManager restored =
                EconomySnapshotStore.restore(
                        snapshot, catalog()
                );

        assertEquals(
                600L,
                restored.totalQuantity(GRAIN)
        );

        assertEquals(
                75L,
                restored.totalQuantity(COIN)
        );

        assertEquals(
                manager.nextId(),
                restored.nextId()
        );

        assertEquals(2, restored.all().size());

        assertTrue(snapshot.transactions().isEmpty());
    }

    @Test
    void repeatMigrationIsIdempotent()
            throws IOException {

        new InventoryPersistence(directory).save(
                populated()
        );

        EconomyLegacyMigrator.Result first =
                EconomyLegacyMigrator.migrate(
                        directory, catalog()
                );

        assertEquals(
                EconomyLegacyMigrator.Outcome.MIGRATED,
                first.outcome()
        );

        byte[] snapshotBefore = Files.readAllBytes(
                new EconomySnapshotStore(directory).file()
        );

        EconomyLegacyMigrator.Result second =
                EconomyLegacyMigrator.migrate(
                        directory, catalog()
                );

        assertEquals(
                EconomyLegacyMigrator.Outcome.ALREADY_MATCHING,
                second.outcome()
        );

        assertArrayEquals(
                snapshotBefore,
                Files.readAllBytes(
                        new EconomySnapshotStore(directory).file()
                )
        );
    }

    @Test
    void refusesMissingLegacySave() {
        assertThrows(
                IOException.class,
                () -> EconomyLegacyMigrator.migrate(
                        directory, catalog()
                )
        );

        assertFalse(Files.exists(
                new EconomySnapshotStore(directory).file()
        ));
    }

    @Test
    void refusesCorruptLegacySave()
            throws IOException {

        InventoryPersistence legacy =
                new InventoryPersistence(directory);

        Files.writeString(
                legacy.file(),
                "CORRUPT\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> EconomyLegacyMigrator.migrate(
                        directory, catalog()
                )
        );

        assertFalse(Files.exists(
                new EconomySnapshotStore(directory).file()
        ));
    }

    @Test
    void refusesConflictingSnapshot()
            throws IOException {

        InventoryManager original = populated();

        new InventoryPersistence(directory).save(
                original
        );

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(
                EconomySnapshotStore.capture(original)
        );

        ResourceInventory farm =
                original.all().iterator().next();

        original.produce(
                farm.id(), GRAIN, 50L, "later_harvest"
        );

        store.write(
                EconomySnapshotStore.capture(original)
        );

        byte[] before = Files.readAllBytes(
                store.file()
        );

        assertThrows(
                IOException.class,
                () -> EconomyLegacyMigrator.migrate(
                        directory, catalog()
                )
        );

        assertArrayEquals(
                before,
                Files.readAllBytes(store.file())
        );
    }

    @Test
    void refusesOrphanedBackup()
            throws IOException {

        new InventoryPersistence(directory).save(
                populated()
        );

        Path backup = new EconomySnapshotRecovery(
                directory
        ).backupFile();

        Files.writeString(
                backup,
                "RECOVERY_REQUIRES_REVIEW\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> EconomyLegacyMigrator.migrate(
                        directory, catalog()
                )
        );
    }

    @Test
    void migratesEmptyLegacyEconomy()
            throws IOException {

        InventoryManager empty =
                new InventoryManager(catalog());

        new InventoryPersistence(directory).save(empty);

        EconomyLegacyMigrator.Result result =
                EconomyLegacyMigrator.migrate(
                        directory, catalog()
                );

        assertEquals(
                EconomyLegacyMigrator.Outcome.MIGRATED,
                result.outcome()
        );

        assertEquals(0, result.inventoryCount());

        assertTrue(
                new EconomySnapshotStore(directory)
                        .read(catalog())
                        .inventories()
                        .isEmpty()
        );
    }

    @Test
    void rejectsExistingInvalidSnapshot()
            throws IOException {

        new InventoryPersistence(directory).save(
                populated()
        );

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        Files.writeString(
                store.file(),
                "INVALID_SNAPSHOT\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> EconomyLegacyMigrator.migrate(
                        directory, catalog()
                )
        );

        assertEquals(
                "INVALID_SNAPSHOT\n",
                Files.readString(
                        store.file(),
                        StandardCharsets.UTF_8
                )
        );
    }

    @Test
    void migrationPreservesOwnerAndLocation()
            throws IOException {

        InventoryManager manager = populated();

        new InventoryPersistence(directory).save(
                manager
        );

        EconomyLegacyMigrator.migrate(
                directory, catalog()
        );

        InventoryManager restored =
                EconomySnapshotStore.restore(
                        new EconomySnapshotStore(directory)
                                .read(catalog()),
                        catalog()
                );

        for (ResourceInventory original : manager.all()) {
            ResourceInventory loaded =
                    restored.require(original.id());

            assertEquals(
                    original.owner(),
                    loaded.owner()
            );

            assertEquals(
                    original.location(),
                    loaded.location()
            );

            assertEquals(
                    original.balances(),
                    loaded.balances()
            );
        }
    }
}
