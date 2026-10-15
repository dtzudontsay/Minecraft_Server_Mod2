
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class EconomySnapshotCommitterTest {

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

    private EconomySnapshotStore store() {
        return new EconomySnapshotStore(directory);
    }

    @Test
    void verifiedCommitPreservesBalances()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 200L, "harvest"
        );

        var committed = EconomySnapshotCommitter.commit(
                directory, catalog(), manager, List.of()
        );

        assertEquals(
                200L,
                EconomySnapshotStore.restore(
                        committed, catalog()
                ).totalQuantity(GRAIN)
        );

        assertEquals(1, committed.transactions().size());

        assertEquals(committed, store().read(catalog()));

        // Commit never clears the working journal.
        assertEquals(1L, manager.transactionCount());
    }

    @Test
    void secondCommitPreservesPreviousGeneration()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 100L, "first"
        );

        EconomySnapshotCommitter.commit(
                directory, catalog(), manager, List.of()
        );

        byte[] previous = Files.readAllBytes(
                store().file()
        );

        manager.produce(
                inventory.id(), GRAIN, 20L, "second"
        );

        EconomySnapshotCommitter.commit(
                directory, catalog(), manager, List.of()
        );

        assertArrayEquals(
                previous,
                Files.readAllBytes(
                        new EconomySnapshotRecovery(directory)
                                .backupFile()
                )
        );

        assertEquals(
                120L,
                EconomySnapshotStore.restore(
                        store().read(catalog()),
                        catalog()
                ).totalQuantity(GRAIN)
        );
    }

    @Test
    void corruptPrimaryIsNotOverwritten()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 100L, "first"
        );

        EconomySnapshotCommitter.commit(
                directory, catalog(), manager, List.of()
        );

        Files.writeString(
                store().file(),
                "CORRUPT\n",
                StandardCharsets.UTF_8
        );

        manager.produce(
                inventory.id(), GRAIN, 10L, "second"
        );

        assertThrows(
                IOException.class,
                () -> EconomySnapshotCommitter.commit(
                        directory, catalog(), manager, List.of()
                )
        );

        assertEquals(
                "CORRUPT\n",
                Files.readString(
                        store().file(),
                        StandardCharsets.UTF_8
                )
        );

        assertEquals(2L, manager.transactionCount());
        assertEquals(110L, inventory.quantity(GRAIN));
    }

    @Test
    void checkpointGuardRejectsTruncatedSession() {
        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 10L, "first"
        );

        manager.produce(
                inventory.id(), GRAIN, 10L, "second"
        );

        var truncated = EconomyJournalContinuity.capture(
                manager, List.of(), 1
        );

        assertThrows(
                IllegalStateException.class,
                () -> EconomyCheckpointGuard.verifyCaptured(
                        manager, truncated
                )
        );
    }

    @Test
    void failedCommitDoesNotClearJournal()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 50L, "first"
        );

        Files.writeString(
                store().file(),
                "INVALID_PRIMARY\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> EconomySnapshotCommitter.commit(
                        directory, catalog(), manager, List.of()
                )
        );

        assertEquals(1L, manager.transactionCount());
        assertEquals(50L, inventory.quantity(GRAIN));
    }

    @Test
    void repeatedCommitDoesNotDuplicateRecords()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 10L, "harvest"
        );

        EconomySnapshotCommitter.commit(
                directory, catalog(), manager, List.of()
        );

        EconomySnapshotCommitter.commit(
                directory, catalog(), manager, List.of()
        );

        assertEquals(
                1,
                store().read(catalog()).transactions().size()
        );
    }

    @Test
    void historicalEntriesArePreserved()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        EconomicTransaction older =
                new EconomicTransaction(
                        1L,
                        EconomicTransaction.Kind.PRODUCTION,
                        null,
                        inventory.id(),
                        GRAIN,
                        5L,
                        "previous_session"
                );

        manager.produce(
                inventory.id(), GRAIN, 20L, "current_session"
        );

        var committed = EconomySnapshotCommitter.commit(
                directory,
                catalog(),
                manager,
                List.of(older)
        );

        assertEquals(2, committed.transactions().size());
        assertEquals(
                "previous_session",
                committed.transactions().get(0).cause()
        );
        assertEquals(
                "current_session",
                committed.transactions().get(1).cause()
        );
    }

    @Test
    void rejectsSessionLargerThanRetainedLimit()
            throws IOException {

        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        int limit =
                EconomyJournalContinuity
                        .DEFAULT_RETAINED_TRANSACTIONS;

        for (int i = 0; i <= limit; i++) {
            manager.produce(
                    inventory.id(),
                    GRAIN,
                    1L,
                    "operation_" + i
            );
        }

        assertEquals(
                limit + 1L,
                manager.transactionCount()
        );

        assertThrows(
                IOException.class,
                () -> EconomySnapshotCommitter.commit(
                        directory, catalog(), manager, List.of()
                )
        );

        assertFalse(Files.exists(store().file()));

        assertEquals(
                limit + 1L,
                manager.transactionCount()
        );
    }

    @Test
    void checkedSnapshotDifferenceIsDetected() {
        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 10L, "harvest"
        );

        var expected = EconomyJournalContinuity.capture(
                manager, List.of()
        );

        manager.produce(
                inventory.id(), GRAIN, 10L, "more"
        );

        var changed = EconomyJournalContinuity.capture(
                manager, List.of()
        );

        assertThrows(
                IOException.class,
                () -> EconomyCheckpointGuard.verifyPersisted(
                        expected, changed
                )
        );
    }
}
