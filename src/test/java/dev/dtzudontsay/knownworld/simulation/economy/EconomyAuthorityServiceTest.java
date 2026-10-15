
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

public final class EconomyAuthorityServiceTest {

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
                inventory.id(), GRAIN, 100L, "harvest"
        );

        return manager;
    }

    private void prepareMatching() throws IOException {
        InventoryManager manager = populated();

        new InventoryPersistence(directory).save(manager);

        new EconomySnapshotStore(directory).write(
                EconomySnapshotStore.capture(manager)
        );
    }

    @Test
    void emptyWorldRemainsLegacyCompatible()
            throws IOException {

        var loaded = EconomyAuthorityService.load(
                directory, catalog()
        );

        assertEquals(
                EconomyAuthorityService.Mode.LEGACY_COMPATIBILITY,
                loaded.mode()
        );

        assertTrue(loaded.manager().all().isEmpty());

        assertFalse(Files.exists(
                new EconomyAuthorityMarker(directory).file()
        ));
    }

    @Test
    void matchingFilesDoNotActivateAutomatically()
            throws IOException {

        prepareMatching();

        var loaded = EconomyAuthorityService.load(
                directory, catalog()
        );

        assertEquals(
                EconomyAuthorityService.Mode.LEGACY_COMPATIBILITY,
                loaded.mode()
        );

        assertFalse(
                new EconomyAuthorityMarker(directory).exists()
        );
    }

    @Test
    void explicitActivationSelectsSnapshot()
            throws IOException {

        prepareMatching();

        EconomyAuthorityService.activateMatchingSnapshot(
                directory, catalog()
        );

        var loaded = EconomyAuthorityService.load(
                directory, catalog()
        );

        assertEquals(
                EconomyAuthorityService.Mode.SNAPSHOT_PRIMARY,
                loaded.mode()
        );

        assertEquals(
                100L,
                loaded.manager().totalQuantity(GRAIN)
        );
    }

    @Test
    void activationRefusesLegacyOnly()
            throws IOException {

        new InventoryPersistence(directory).save(
                populated()
        );

        assertThrows(
                IOException.class,
                () -> EconomyAuthorityService
                        .activateMatchingSnapshot(
                                directory, catalog()
                        )
        );
    }

    @Test
    void activationRefusesConflictingFiles()
            throws IOException {

        prepareMatching();

        InventoryManager changed = populated();

        ResourceInventory inventory =
                changed.all().iterator().next();

        changed.produce(
                inventory.id(), GRAIN, 50L, "extra"
        );

        new EconomySnapshotStore(directory).write(
                EconomySnapshotStore.capture(changed)
        );

        assertThrows(
                IOException.class,
                () -> EconomyAuthorityService
                        .activateMatchingSnapshot(
                                directory, catalog()
                        )
        );
    }

    @Test
    void snapshotSaveLeavesLegacyUntouched()
            throws IOException {

        prepareMatching();

        EconomyAuthorityService.activateMatchingSnapshot(
                directory, catalog()
        );

        Path legacy =
                new InventoryPersistence(directory).file();

        byte[] before = Files.readAllBytes(legacy);

        var loaded = EconomyAuthorityService.load(
                directory, catalog()
        );

        InventoryId id =
                loaded.manager().all()
                        .iterator().next().id();

        loaded.manager().produce(
                id, GRAIN, 25L, "new_harvest"
        );

        EconomyAuthorityService.save(
                directory, catalog(), loaded
        );

        assertArrayEquals(
                before,
                Files.readAllBytes(legacy)
        );

        var reloaded = EconomyAuthorityService.load(
                directory, catalog()
        );

        assertEquals(
                125L,
                reloaded.manager().totalQuantity(GRAIN)
        );
    }

    @Test
    void snapshotMarkerWithMissingSnapshotFailsClosed()
            throws IOException {

        prepareMatching();

        EconomyAuthorityService.activateMatchingSnapshot(
                directory, catalog()
        );

        Files.delete(
                new EconomySnapshotStore(directory).file()
        );

        assertThrows(
                IOException.class,
                () -> EconomyAuthorityService.load(
                        directory, catalog()
                )
        );
    }

    @Test
    void corruptMarkerFailsClosed() throws IOException {
        prepareMatching();

        Path marker =
                new EconomyAuthorityMarker(directory).file();

        Files.writeString(
                marker,
                "UNKNOWN_AUTHORITY\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> EconomyAuthorityService.load(
                        directory, catalog()
                )
        );
    }

    @Test
    void secondSnapshotSaveCreatesRecoveryBackup()
            throws IOException {

        prepareMatching();

        EconomyAuthorityService.activateMatchingSnapshot(
                directory, catalog()
        );

        var loaded = EconomyAuthorityService.load(
                directory, catalog()
        );

        InventoryId id = loaded.manager()
                .all().iterator().next().id();

        loaded.manager().produce(
                id, GRAIN, 20L, "first"
        );

        EconomyAuthorityService.save(
                directory, catalog(), loaded
        );

        loaded.manager().produce(
                id, GRAIN, 10L, "second"
        );

        EconomyAuthorityService.save(
                directory, catalog(), loaded
        );

        assertTrue(Files.exists(
                new EconomySnapshotRecovery(directory)
                        .backupFile()
        ));

        assertEquals(
                130L,
                EconomyAuthorityService.load(
                        directory, catalog()
                ).manager().totalQuantity(GRAIN)
        );
    }

    @Test
    void snapshotPrimaryCanLoadWithoutLegacyFile()
            throws IOException {

        prepareMatching();

        EconomyAuthorityService.activateMatchingSnapshot(
                directory, catalog()
        );

        Files.delete(
                new InventoryPersistence(directory).file()
        );

        var loaded = EconomyAuthorityService.load(
                directory, catalog()
        );

        assertEquals(
                EconomyAuthorityService.Mode.SNAPSHOT_PRIMARY,
                loaded.mode()
        );

        assertEquals(
                100L,
                loaded.manager().totalQuantity(GRAIN)
        );
    }

    @Test
    void corruptSnapshotPrimaryDoesNotSilentlyUseBackup()
            throws IOException {

        prepareMatching();

        EconomyAuthorityService.activateMatchingSnapshot(
                directory, catalog()
        );

        Files.writeString(
                new EconomySnapshotStore(directory).file(),
                "CORRUPT\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> EconomyAuthorityService.load(
                        directory, catalog()
                )
        );
    }

    @Test
    void legacySessionCannotSaveAfterActivation()
            throws IOException {

        prepareMatching();

        var oldSession = EconomyAuthorityService.load(
                directory, catalog()
        );

        EconomyAuthorityService.activateMatchingSnapshot(
                directory, catalog()
        );

        assertThrows(
                IOException.class,
                () -> EconomyAuthorityService.save(
                        directory, catalog(), oldSession
                )
        );
    }
}
