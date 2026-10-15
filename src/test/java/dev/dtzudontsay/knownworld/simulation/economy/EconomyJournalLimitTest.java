
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public final class EconomyJournalLimitTest {

    @TempDir
    Path directory;

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private InventoryManager manager(int limit) {
        return new InventoryManager(
                ResourceCatalog.benchmarkDefaults(),
                limit
        );
    }

    private ResourceInventory inventory(
            InventoryManager manager,
            long npc
    ) {
        return manager.create(
                EconomicOwner.npc(new NpcId(npc)),
                new SettlementId(1L)
        );
    }

    @Test
    void acceptsOperationsUntilLimit() {
        InventoryManager manager = manager(2);
        ResourceInventory inventory = inventory(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 10L, "first"
        );

        manager.produce(
                inventory.id(), GRAIN, 20L, "second"
        );

        assertEquals(2L, manager.transactionCount());
        assertEquals(30L, inventory.quantity(GRAIN));
        assertTrue(manager.journalFull());
        assertEquals(0, manager.remainingJournalCapacity());
    }

    @Test
    void rejectsProductionWhenFullWithoutChangingBalance() {
        InventoryManager manager = manager(1);
        ResourceInventory inventory = inventory(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 10L, "first"
        );

        assertThrows(
                IllegalStateException.class,
                () -> manager.produce(
                        inventory.id(), GRAIN, 20L, "second"
                )
        );

        assertEquals(10L, inventory.quantity(GRAIN));
        assertEquals(1L, manager.transactionCount());
    }

    @Test
    void rejectsConsumptionWhenFullWithoutChangingBalance() {
        InventoryManager manager = manager(1);
        ResourceInventory inventory = inventory(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 10L, "first"
        );

        assertThrows(
                IllegalStateException.class,
                () -> manager.consume(
                        inventory.id(), GRAIN, 5L, "food"
                )
        );

        assertEquals(10L, inventory.quantity(GRAIN));
        assertEquals(1L, manager.transactionCount());
    }

    @Test
    void rejectsTransferWhenFullWithoutPartialMutation() {
        InventoryManager manager = manager(1);
        ResourceInventory first = inventory(manager, 1L);
        ResourceInventory second = inventory(manager, 2L);

        manager.produce(
                first.id(), GRAIN, 10L, "first"
        );

        assertThrows(
                IllegalStateException.class,
                () -> manager.transfer(
                        first.id(), second.id(),
                        GRAIN, 5L, "trade"
                )
        );

        assertEquals(10L, first.quantity(GRAIN));
        assertEquals(0L, second.quantity(GRAIN));
        assertEquals(1L, manager.transactionCount());
    }

    @Test
    void invalidJournalLimitIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> manager(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> manager(-1)
        );
    }

    @Test
    void defaultConstructorRetainsCompatibility() {
        InventoryManager manager =
                new InventoryManager(
                        ResourceCatalog.benchmarkDefaults()
                );

        assertEquals(
                InventoryManager.DEFAULT_MAX_JOURNAL_ENTRIES,
                manager.maxJournalEntries()
        );

        assertEquals(
                InventoryManager.DEFAULT_MAX_JOURNAL_ENTRIES,
                manager.remainingJournalCapacity()
        );
    }

    @Test
    void checkpointCapturesAllCurrentSessionOperations()
            throws IOException {

        ResourceCatalog catalog =
                ResourceCatalog.benchmarkDefaults();

        InventoryManager manager =
                new InventoryManager(catalog, 5);

        ResourceInventory source = inventory(manager, 1L);
        ResourceInventory target = inventory(manager, 2L);

        manager.produce(
                source.id(), GRAIN, 100L, "harvest"
        );

        manager.transfer(
                source.id(), target.id(),
                GRAIN, 20L, "allocation"
        );

        var snapshot = EconomyJournalContinuity.capture(
                manager,
                java.util.List.of(),
                10
        );

        assertDoesNotThrow(
                () -> EconomyCheckpointGuard.verifyCaptured(
                        manager, snapshot
                )
        );

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(snapshot);

        var loaded = store.read(catalog);

        assertDoesNotThrow(
                () -> EconomyCheckpointGuard.verifyPersisted(
                        snapshot, loaded
                )
        );
    }

    @Test
    void checkpointRejectsMissingSessionTransactions() {
        InventoryManager manager = manager(5);
        ResourceInventory inventory = inventory(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 10L, "harvest"
        );

        var snapshot = EconomyJournalContinuity.capture(
                manager,
                java.util.List.of(),
                1
        );

        // The one operation fits and should validate.
        EconomyCheckpointGuard.verifyCaptured(
                manager, snapshot
        );

        manager.produce(
                inventory.id(), GRAIN, 5L, "second"
        );

        assertThrows(
                IllegalStateException.class,
                () -> EconomyCheckpointGuard.verifyCaptured(
                        manager, snapshot
                )
        );
    }

    @Test
    void rejectedOperationDoesNotConsumeSequence() {
        InventoryManager manager = manager(3);
        ResourceInventory inventory = inventory(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 10L, "first"
        );

        assertThrows(
                IllegalStateException.class,
                () -> manager.consume(
                        inventory.id(), GRAIN, 100L, "invalid"
                )
        );

        manager.produce(
                inventory.id(), GRAIN, 5L, "second"
        );

        assertEquals(2L, manager.transactionCount());
        assertEquals(
                2L,
                manager.transactions().get(1).sequence()
        );
    }

    @Test
    void restoredInventoryBeginsWithFreshCapacity()
            throws IOException {

        ResourceCatalog catalog =
                ResourceCatalog.benchmarkDefaults();

        InventoryManager original =
                new InventoryManager(catalog, 1);

        ResourceInventory inventory = inventory(original, 1L);

        original.produce(
                inventory.id(), GRAIN, 10L, "harvest"
        );

        InventoryPersistence persistence =
                new InventoryPersistence(directory);

        persistence.save(original);

        InventoryManager loaded =
                persistence.load(catalog);

        assertEquals(10L,
                loaded.require(inventory.id()).quantity(GRAIN));

        assertEquals(0L, loaded.transactionCount());

        assertFalse(loaded.journalFull());
    }
}
