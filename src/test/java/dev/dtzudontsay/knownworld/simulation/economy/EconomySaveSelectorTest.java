
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

public final class EconomySaveSelectorTest {

    @TempDir
    Path directory;

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private ResourceCatalog catalog() {
        return ResourceCatalog.benchmarkDefaults();
    }

    private InventoryManager populated() {
        InventoryManager manager =
                new InventoryManager(catalog());

        ResourceInventory inventory = manager.create(
                EconomicOwner.npc(new NpcId(1L)),
                new SettlementId(1L)
        );

        manager.produce(
                inventory.id(),
                GRAIN,
                300L,
                "test_harvest"
        );

        return manager;
    }

    @Test
    void emptyWorldSelectsEmptyManager()
            throws IOException {

        var result = EconomySaveSelector.load(
                directory, catalog()
        );

        assertEquals(
                EconomySaveSelector.Selection.NEW_EMPTY,
                result.selection()
        );

        assertTrue(result.manager().all().isEmpty());

        assertFalse(Files.exists(
                new InventoryPersistence(directory).file()
        ));
    }

    @Test
    void legacyOnlySelectsLegacy()
            throws IOException {

        InventoryManager original = populated();

        new InventoryPersistence(directory).save(original);

        var result = EconomySaveSelector.load(
                directory, catalog()
        );

        assertEquals(
                EconomySaveSelector.Selection.LEGACY,
                result.selection()
        );

        assertEquals(
                300L,
                result.manager().totalQuantity(GRAIN)
        );
    }

    @Test
    void matchingSavesSelectLegacyWithoutWriting()
            throws IOException {

        InventoryManager original = populated();

        InventoryPersistence legacy =
                new InventoryPersistence(directory);

        EconomySnapshotStore snapshot =
                new EconomySnapshotStore(directory);

        legacy.save(original);

        snapshot.write(
                EconomySnapshotStore.capture(original)
        );

        byte[] beforeLegacy = Files.readAllBytes(
                legacy.file()
        );

        byte[] beforeSnapshot = Files.readAllBytes(
                snapshot.file()
        );

        var result = EconomySaveSelector.load(
                directory, catalog()
        );

        assertEquals(
                EconomySaveSelector.Selection
                        .LEGACY_WITH_MATCHING_SNAPSHOT,
                result.selection()
        );

        assertArrayEquals(
                beforeLegacy,
                Files.readAllBytes(legacy.file())
        );

        assertArrayEquals(
                beforeSnapshot,
                Files.readAllBytes(snapshot.file())
        );
    }

    @Test
    void snapshotOnlyIsRejected() throws IOException {
        InventoryManager manager = populated();

        new EconomySnapshotStore(directory).write(
                EconomySnapshotStore.capture(manager)
        );

        assertThrows(
                IOException.class,
                () -> EconomySaveSelector.load(
                        directory, catalog()
                )
        );
    }

    @Test
    void conflictingSavesAreRejected()
            throws IOException {

        InventoryManager manager = populated();

        new InventoryPersistence(directory).save(manager);

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(
                EconomySnapshotStore.capture(manager)
        );

        ResourceInventory inventory =
                manager.all().iterator().next();

        manager.produce(
                inventory.id(), GRAIN, 50L, "second"
        );

        store.write(
                EconomySnapshotStore.capture(manager)
        );

        assertThrows(
                IOException.class,
                () -> EconomySaveSelector.load(
                        directory, catalog()
                )
        );
    }

    @Test
    void corruptLegacySaveIsRejected() throws IOException {
        InventoryPersistence persistence =
                new InventoryPersistence(directory);

        Files.writeString(
                persistence.file(),
                "INVALID\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> EconomySaveSelector.load(
                        directory, catalog()
                )
        );
    }

    @Test
    void orphanedBackupIsRejected() throws IOException {
        Files.writeString(
                directory.resolve("economy_snapshot_v1.tsv.bak"),
                "ORPHANED\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> EconomySaveSelector.load(
                        directory, catalog()
                )
        );
    }

    @Test
    void temporaryFilesAreRejected() throws IOException {
        Files.writeString(
                directory.resolve("economy_inventories.tsv.tmp"),
                "INTERRUPTED\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> EconomySaveSelector.load(
                        directory, catalog()
                )
        );
    }

    @Test
    void selectionDoesNotCreateAnySaveFiles()
            throws IOException {

        EconomySaveSelector.load(
                directory, catalog()
        );

        assertFalse(Files.exists(
                new InventoryPersistence(directory).file()
        ));

        assertFalse(Files.exists(
                new EconomySnapshotStore(directory).file()
        ));
    }
}
