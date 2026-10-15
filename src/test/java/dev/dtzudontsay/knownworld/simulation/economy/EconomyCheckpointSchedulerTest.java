
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public final class EconomyCheckpointSchedulerTest {

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

    @Test
    void schedulerWaitsForConfiguredTickInterval()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 10L, "harvest"
        );

        AtomicInteger saves = new AtomicInteger();

        EconomyCheckpointScheduler scheduler =
                new EconomyCheckpointScheduler(
                        EconomyAuthorityService.Mode
                                .SNAPSHOT_PRIMARY,
                        manager,
                        3,
                        1,
                        () -> {
                            saves.incrementAndGet();
                            manager.rotateCommittedJournal(
                                    manager.transactions()
                            );
                        }
                );

        assertEquals(
                EconomyCheckpointScheduler.Decision.NOT_DUE,
                scheduler.tick()
        );

        assertEquals(
                EconomyCheckpointScheduler.Decision.NOT_DUE,
                scheduler.tick()
        );

        assertEquals(0, saves.get());

        assertEquals(
                EconomyCheckpointScheduler.Decision
                        .CHECKPOINT_REQUIRED,
                scheduler.tick()
        );

        assertEquals(1, saves.get());
        assertEquals(0L, manager.transactionCount());
        assertEquals(1L, scheduler.successfulCheckpoints());
    }

    @Test
    void belowThresholdDoesNotSave()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 10L, "harvest"
        );

        AtomicInteger saves = new AtomicInteger();

        EconomyCheckpointScheduler scheduler =
                new EconomyCheckpointScheduler(
                        EconomyAuthorityService.Mode
                                .SNAPSHOT_PRIMARY,
                        manager,
                        1,
                        2,
                        () -> saves.incrementAndGet()
                );

        assertEquals(
                EconomyCheckpointScheduler.Decision
                        .BELOW_THRESHOLD,
                scheduler.tick()
        );

        assertEquals(0, saves.get());
    }

    @Test
    void legacyModeNeverPerformsScheduledCheckpoint()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 100L, "harvest"
        );

        AtomicInteger saves = new AtomicInteger();

        EconomyCheckpointScheduler scheduler =
                new EconomyCheckpointScheduler(
                        EconomyAuthorityService.Mode
                                .LEGACY_COMPATIBILITY,
                        manager,
                        1,
                        1,
                        () -> saves.incrementAndGet()
                );

        for (int i = 0; i < 10; i++) {
            assertEquals(
                    EconomyCheckpointScheduler.Decision
                            .LEGACY_MODE,
                    scheduler.tick()
            );
        }

        assertEquals(0, saves.get());
        assertEquals(0L, scheduler.successfulCheckpoints());
        assertEquals(100L, inventory.quantity(GRAIN));
    }

    @Test
    void failedCheckpointPreservesJournal()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 25L, "harvest"
        );

        EconomyCheckpointScheduler scheduler =
                new EconomyCheckpointScheduler(
                        EconomyAuthorityService.Mode
                                .SNAPSHOT_PRIMARY,
                        manager,
                        1,
                        1,
                        () -> {
                            throw new IOException(
                                    "Simulated disk failure"
                            );
                        }
                );

        assertThrows(
                IOException.class,
                scheduler::tick
        );

        assertEquals(1L, manager.transactionCount());
        assertEquals(25L, inventory.quantity(GRAIN));
        assertEquals(0L, manager.journalGeneration());
        assertEquals(0L, scheduler.successfulCheckpoints());
    }

    @Test
    void fakeSuccessfulSaveIsRejected()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 10L, "harvest"
        );

        EconomyCheckpointScheduler scheduler =
                new EconomyCheckpointScheduler(
                        EconomyAuthorityService.Mode
                                .SNAPSHOT_PRIMARY,
                        manager,
                        1,
                        1,
                        () -> {
                            // Deliberately does not rotate.
                        }
                );

        assertThrows(
                IOException.class,
                scheduler::tick
        );

        assertEquals(1L, manager.transactionCount());
        assertEquals(0L, scheduler.successfulCheckpoints());
    }

    @Test
    void realSnapshotSaveRotatesAtThreshold()
            throws IOException {

        InventoryManager original = manager();
        ResourceInventory inventory = create(original);

        original.produce(
                inventory.id(), GRAIN, 100L, "initial"
        );

        new InventoryPersistence(directory).save(original);

        new EconomySnapshotStore(directory).write(
                EconomySnapshotStore.capture(original)
        );

        EconomyAuthorityService.activateMatchingSnapshot(
                directory, catalog()
        );

        EconomyAuthorityService.Loaded loaded =
                EconomyAuthorityService.load(
                        directory, catalog()
                );

        InventoryId id = loaded.manager().all()
                .iterator().next().id();

        loaded.manager().produce(
                id, GRAIN, 20L, "new_harvest"
        );

        EconomyCheckpointScheduler scheduler =
                new EconomyCheckpointScheduler(
                        loaded.mode(),
                        loaded.manager(),
                        1,
                        1,
                        () -> EconomyAuthorityService.save(
                                directory,
                                catalog(),
                                loaded
                        )
                );

        assertEquals(
                EconomyCheckpointScheduler.Decision
                        .CHECKPOINT_REQUIRED,
                scheduler.tick()
        );

        assertEquals(0L, loaded.manager().transactionCount());

        assertEquals(
                120L,
                EconomyAuthorityService.load(
                        directory, catalog()
                ).manager().totalQuantity(GRAIN)
        );

        assertEquals(2,
                EconomyAuthorityService.load(
                        directory, catalog()
                ).previousTransactions().size()
        );

        assertTrue(Files.exists(
                new EconomySnapshotRecovery(directory)
                        .backupFile()
        ));
    }

    @Test
    void invalidSchedulerConfigurationIsRejected() {
        InventoryManager manager = manager();

        assertThrows(
                IllegalArgumentException.class,
                () -> new EconomyCheckpointScheduler(
                        EconomyAuthorityService.Mode
                                .SNAPSHOT_PRIMARY,
                        manager,
                        0,
                        1,
                        () -> {}
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new EconomyCheckpointScheduler(
                        EconomyAuthorityService.Mode
                                .SNAPSHOT_PRIMARY,
                        manager,
                        1,
                        10_001,
                        () -> {}
                )
        );
    }

    @Test
    void schedulerCanCheckpointMoreThanOnce()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        EconomyCheckpointScheduler scheduler =
                new EconomyCheckpointScheduler(
                        EconomyAuthorityService.Mode
                                .SNAPSHOT_PRIMARY,
                        manager,
                        1,
                        1,
                        () -> manager.rotateCommittedJournal(
                                manager.transactions()
                        )
                );

        manager.produce(
                inventory.id(), GRAIN, 5L, "first"
        );

        scheduler.tick();

        manager.produce(
                inventory.id(), GRAIN, 7L, "second"
        );

        scheduler.tick();

        assertEquals(2L, scheduler.successfulCheckpoints());
        assertEquals(2L, manager.journalGeneration());
        assertEquals(12L, inventory.quantity(GRAIN));
    }

    @Test
    void inspectReportsCheckpointDecision() {
        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        EconomyCheckpointScheduler scheduler =
                new EconomyCheckpointScheduler(
                        EconomyAuthorityService.Mode
                                .SNAPSHOT_PRIMARY,
                        manager,
                        200,
                        2,
                        () -> {}
                );

        assertEquals(
                EconomyCheckpointScheduler.Decision
                        .BELOW_THRESHOLD,
                scheduler.inspect()
        );

        manager.produce(
                inventory.id(), GRAIN, 1L, "first"
        );

        manager.produce(
                inventory.id(), GRAIN, 1L, "second"
        );

        assertEquals(
                EconomyCheckpointScheduler.Decision
                        .CHECKPOINT_REQUIRED,
                scheduler.inspect()
        );
    }
}
