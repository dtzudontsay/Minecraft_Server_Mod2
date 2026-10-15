
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

public final class EconomySnapshotRecoveryTest {

    @TempDir
    Path directory;

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private ResourceCatalog catalog() {
        return ResourceCatalog.benchmarkDefaults();
    }

    private InventoryManager manager() {
        return new InventoryManager(catalog());
    }

    private ResourceInventory create(
            InventoryManager manager
    ) {
        return manager.create(
                EconomicOwner.npc(new NpcId(1L)),
                new SettlementId(1L)
        );
    }

    private long grain(
            EconomySnapshotStore.Snapshot snapshot
    ) {
        return snapshot.inventories()
                .get(0)
                .balances()
                .getOrDefault(GRAIN, 0L);
    }

    @Test
    void firstSaveCreatesPrimaryWithoutBackup()
            throws IOException {

        EconomySnapshotRecovery recovery =
                new EconomySnapshotRecovery(directory);

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 100L, "harvest"
        );

        recovery.save(
                EconomySnapshotStore.capture(manager),
                catalog()
        );

        assertTrue(Files.exists(recovery.primaryFile()));
        assertFalse(Files.exists(recovery.backupFile()));

        var result = recovery.readBest(catalog());

        assertEquals(
                EconomySnapshotRecovery.Source.PRIMARY,
                result.source()
        );

        assertEquals(100L, grain(result.snapshot()));
    }

    @Test
    void secondSaveRetainsPreviousGeneration()
            throws IOException {

        EconomySnapshotRecovery recovery =
                new EconomySnapshotRecovery(directory);

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 100L, "first_harvest"
        );

        recovery.save(
                EconomySnapshotStore.capture(manager),
                catalog()
        );

        manager.produce(
                inventory.id(), GRAIN, 50L, "second_harvest"
        );

        recovery.save(
                EconomySnapshotStore.capture(manager),
                catalog()
        );

        assertTrue(Files.exists(recovery.backupFile()));

        assertEquals(
                150L,
                grain(recovery.readBest(catalog()).snapshot())
        );
    }

    @Test
    void corruptPrimaryFallsBackWithoutOverwritingIt()
            throws IOException {

        EconomySnapshotRecovery recovery =
                new EconomySnapshotRecovery(directory);

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 100L, "first"
        );

        recovery.save(
                EconomySnapshotStore.capture(manager),
                catalog()
        );

        manager.produce(
                inventory.id(), GRAIN, 50L, "second"
        );

        recovery.save(
                EconomySnapshotStore.capture(manager),
                catalog()
        );

        String corrupt = "CORRUPTED SNAPSHOT\n";

        Files.writeString(
                recovery.primaryFile(),
                corrupt,
                StandardCharsets.UTF_8
        );

        var result = recovery.readBest(catalog());

        assertEquals(
                EconomySnapshotRecovery.Source.BACKUP,
                result.source()
        );

        assertEquals(100L, grain(result.snapshot()));

        assertEquals(
                corrupt,
                Files.readString(
                        recovery.primaryFile(),
                        StandardCharsets.UTF_8
                )
        );
    }

    @Test
    void refusesToSaveOverCorruptPrimary()
            throws IOException {

        EconomySnapshotRecovery recovery =
                new EconomySnapshotRecovery(directory);

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 100L, "first"
        );

        recovery.save(
                EconomySnapshotStore.capture(manager),
                catalog()
        );

        Files.writeString(
                recovery.primaryFile(),
                "BROKEN\n",
                StandardCharsets.UTF_8
        );

        manager.produce(
                inventory.id(), GRAIN, 50L, "second"
        );

        assertThrows(
                IOException.class,
                () -> recovery.save(
                        EconomySnapshotStore.capture(manager),
                        catalog()
                )
        );

        assertEquals(
                "BROKEN\n",
                Files.readString(
                        recovery.primaryFile(),
                        StandardCharsets.UTF_8
                )
        );
    }

    @Test
    void explicitRestoreRepairsPrimary()
            throws IOException {

        EconomySnapshotRecovery recovery =
                new EconomySnapshotRecovery(directory);

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 100L, "first"
        );

        recovery.save(
                EconomySnapshotStore.capture(manager),
                catalog()
        );

        manager.produce(
                inventory.id(), GRAIN, 50L, "second"
        );

        recovery.save(
                EconomySnapshotStore.capture(manager),
                catalog()
        );

        Files.writeString(
                recovery.primaryFile(),
                "BROKEN\n",
                StandardCharsets.UTF_8
        );

        recovery.restoreBackupAsPrimary(catalog());

        var result = recovery.readBest(catalog());

        assertEquals(
                EconomySnapshotRecovery.Source.PRIMARY,
                result.source()
        );

        assertEquals(100L, grain(result.snapshot()));
    }

    @Test
    void missingPrimaryAndBackupIsAnError() {
        EconomySnapshotRecovery recovery =
                new EconomySnapshotRecovery(directory);

        assertThrows(
                IOException.class,
                () -> recovery.readBest(catalog())
        );
    }

    @Test
    void twoInvalidGenerationsAreRejected()
            throws IOException {

        EconomySnapshotRecovery recovery =
                new EconomySnapshotRecovery(directory);

        Files.writeString(
                recovery.primaryFile(),
                "INVALID PRIMARY\n",
                StandardCharsets.UTF_8
        );

        Files.writeString(
                recovery.backupFile(),
                "INVALID BACKUP\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> recovery.readBest(catalog())
        );
    }

    @Test
    void missingBackupCannotBeRestored()
            throws IOException {

        EconomySnapshotRecovery recovery =
                new EconomySnapshotRecovery(directory);

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 10L, "first"
        );

        recovery.save(
                EconomySnapshotStore.capture(manager),
                catalog()
        );

        assertThrows(
                IOException.class,
                () -> recovery.restoreBackupAsPrimary(
                        catalog()
                )
        );

        assertEquals(
                10L,
                grain(recovery.readBest(catalog()).snapshot())
        );
    }

    @Test
    void successfulSavesLeaveNoBackupTemporaryFile()
            throws IOException {

        EconomySnapshotRecovery recovery =
                new EconomySnapshotRecovery(directory);

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 10L, "first"
        );

        recovery.save(
                EconomySnapshotStore.capture(manager),
                catalog()
        );

        manager.produce(
                inventory.id(), GRAIN, 10L, "second"
        );

        recovery.save(
                EconomySnapshotStore.capture(manager),
                catalog()
        );

        assertFalse(Files.exists(
                directory.resolve(
                        "economy_snapshot_v1.tsv.bak.tmp"
                )
        ));
    }
}
