
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public final class EconomyMigrationAssessmentTest {

    @TempDir
    Path directory;

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private ResourceCatalog catalog() {
        return ResourceCatalog.benchmarkDefaults();
    }

    private InventoryManager populatedManager() {
        InventoryManager manager =
                new InventoryManager(catalog());

        ResourceInventory inventory = manager.create(
                EconomicOwner.npc(new NpcId(1L)),
                new SettlementId(1L)
        );

        manager.produce(
                inventory.id(),
                GRAIN,
                125L,
                "initial_harvest"
        );

        return manager;
    }

    @Test
    void emptyWorldIsDetected() {
        var report = EconomyMigrationAssessment.assess(
                directory, catalog()
        );

        assertEquals(
                EconomyMigrationAssessment.Status.EMPTY_WORLD,
                report.status()
        );

        assertTrue(report.safeToProceedAutomatically());
    }

    @Test
    void legacyOnlyRequiresMigration() throws IOException {
        new InventoryPersistence(directory).save(
                populatedManager()
        );

        var report = EconomyMigrationAssessment.assess(
                directory, catalog()
        );

        assertEquals(
                EconomyMigrationAssessment.Status.LEGACY_ONLY,
                report.status()
        );

        assertFalse(report.safeToProceedAutomatically());
        assertEquals(1, report.legacyInventories());
    }

    @Test
    void snapshotOnlyIsIdentified() throws IOException {
        InventoryManager manager = populatedManager();

        new EconomySnapshotStore(directory).write(
                EconomySnapshotStore.capture(manager)
        );

        var report = EconomyMigrationAssessment.assess(
                directory, catalog()
        );

        assertEquals(
                EconomyMigrationAssessment.Status.SNAPSHOT_ONLY,
                report.status()
        );

        assertFalse(report.safeToProceedAutomatically());
    }

    @Test
    void matchingSaveRepresentationsAreDetected()
            throws IOException {

        InventoryManager manager = populatedManager();

        new InventoryPersistence(directory).save(manager);

        new EconomySnapshotStore(directory).write(
                EconomySnapshotStore.capture(manager)
        );

        var report = EconomyMigrationAssessment.assess(
                directory, catalog()
        );

        assertEquals(
                EconomyMigrationAssessment.Status.MATCHING,
                report.status()
        );

        assertTrue(report.safeToProceedAutomatically());
    }

    @Test
    void divergentBalancesAreRejected() throws IOException {
        InventoryManager manager = populatedManager();

        new InventoryPersistence(directory).save(manager);

        new EconomySnapshotStore(directory).write(
                EconomySnapshotStore.capture(manager)
        );

        ResourceInventory inventory =
                manager.all().iterator().next();

        manager.produce(
                inventory.id(),
                GRAIN,
                50L,
                "later_harvest"
        );

        new EconomySnapshotStore(directory).write(
                EconomySnapshotStore.capture(manager)
        );

        var report = EconomyMigrationAssessment.assess(
                directory, catalog()
        );

        assertEquals(
                EconomyMigrationAssessment.Status.CONFLICT,
                report.status()
        );

        assertFalse(report.safeToProceedAutomatically());
    }

    @Test
    void corruptionFailsClosed() throws IOException {
        InventoryManager manager = populatedManager();

        new InventoryPersistence(directory).save(manager);

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(
                EconomySnapshotStore.capture(manager)
        );

        Files.writeString(
                store.file(),
                "CORRUPTED\n"
        );

        var report = EconomyMigrationAssessment.assess(
                directory, catalog()
        );

        assertEquals(
                EconomyMigrationAssessment.Status.INVALID_DATA,
                report.status()
        );

        assertFalse(report.safeToProceedAutomatically());
    }

    @Test
    void preflightNeverWritesFiles() throws IOException {
        new InventoryPersistence(directory).save(
                populatedManager()
        );

        Path legacy = new InventoryPersistence(
                directory
        ).file();

        byte[] before = Files.readAllBytes(legacy);

        EconomyMigrationAssessment.assess(
                directory, catalog()
        );

        assertArrayEquals(
                before,
                Files.readAllBytes(legacy)
        );

        assertFalse(Files.exists(
                new EconomySnapshotStore(directory).file()
        ));
    }

    @Test
    void journalThresholdsAreExplicit() {
        EconomyJournalCapacityPolicy policy =
                new EconomyJournalCapacityPolicy(5, 10);

        assertEquals(
                EconomyJournalCapacityPolicy.State.NORMAL,
                policy.evaluate(4)
        );

        assertEquals(
                EconomyJournalCapacityPolicy.State.WARNING,
                policy.evaluate(5)
        );

        assertEquals(
                EconomyJournalCapacityPolicy.State.ROTATION_REQUIRED,
                policy.evaluate(10)
        );

        assertThrows(
                IllegalStateException.class,
                () -> policy.requireCapacityForNext(10)
        );
    }

    @Test
    void invalidJournalThresholdsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EconomyJournalCapacityPolicy(10, 10)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new EconomyJournalCapacityPolicy(0, 100)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> EconomyJournalCapacityPolicy.defaults()
                        .evaluate(-1)
        );
    }
}
