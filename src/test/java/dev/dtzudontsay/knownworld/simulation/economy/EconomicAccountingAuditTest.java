
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class EconomicAccountingAuditTest {

    @TempDir
    Path tempDirectory;

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private InventoryManager manager() {
        return new InventoryManager(
                ResourceCatalog.benchmarkDefaults()
        );
    }

    private ResourceInventory create(
            InventoryManager manager,
            long npcId
    ) {
        return manager.create(
                EconomicOwner.npc(new NpcId(npcId)),
                new SettlementId(1L)
        );
    }

    @Test
    void journalRecordsSuccessfulOperations() {
        InventoryManager manager = manager();

        ResourceInventory first = create(manager, 1L);
        ResourceInventory second = create(manager, 2L);

        manager.produce(
                first.id(), GRAIN, 100L, "harvest"
        );

        manager.transfer(
                first.id(), second.id(),
                GRAIN, 35L, "trade"
        );

        manager.consume(
                second.id(), GRAIN, 10L, "food"
        );

        List<EconomicTransaction> journal =
                manager.transactions();

        assertEquals(3, journal.size());

        assertEquals(
                EconomicTransaction.Kind.PRODUCTION,
                journal.get(0).kind()
        );

        assertEquals(
                EconomicTransaction.Kind.TRANSFER,
                journal.get(1).kind()
        );

        assertEquals(
                EconomicTransaction.Kind.CONSUMPTION,
                journal.get(2).kind()
        );

        assertEquals(1L, journal.get(0).sequence());
        assertEquals(2L, journal.get(1).sequence());
        assertEquals(3L, journal.get(2).sequence());

        assertEquals("harvest", journal.get(0).cause());
        assertEquals("trade", journal.get(1).cause());
        assertEquals("food", journal.get(2).cause());
    }

    @Test
    void auditReconcilesProductionTransfersAndConsumption() {
        InventoryManager manager = manager();

        ResourceInventory farm = create(manager, 1L);
        ResourceInventory household = create(manager, 2L);

        var checkpoint =
                EconomicAccountingAudit.checkpoint(manager);

        manager.produce(
                farm.id(), GRAIN, 500L, "harvest"
        );

        manager.transfer(
                farm.id(), household.id(),
                GRAIN, 120L, "food_allocation"
        );

        manager.consume(
                household.id(), GRAIN, 20L, "daily_food"
        );

        assertDoesNotThrow(
                () -> EconomicAccountingAudit.verify(
                        manager, checkpoint
                )
        );

        assertEquals(380L, farm.quantity(GRAIN));
        assertEquals(100L, household.quantity(GRAIN));
        assertEquals(480L, manager.totalQuantity(GRAIN));
    }

    @Test
    void rejectedTransactionDoesNotEnterJournal() {
        InventoryManager manager = manager();

        ResourceInventory first = create(manager, 1L);
        ResourceInventory second = create(manager, 2L);

        manager.produce(
                first.id(), GRAIN, 10L, "harvest"
        );

        long beforeCount = manager.transactionCount();

        assertThrows(
                IllegalStateException.class,
                () -> manager.transfer(
                        first.id(), second.id(),
                        GRAIN, 20L, "invalid_trade"
                )
        );

        assertEquals(
                beforeCount, manager.transactionCount()
        );

        assertEquals(10L, first.quantity(GRAIN));
        assertEquals(0L, second.quantity(GRAIN));
    }

    @Test
    void checkpointCanBeCreatedAfterPreviousActivity() {
        InventoryManager manager = manager();

        ResourceInventory inventory = create(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 100L, "old_harvest"
        );

        var checkpoint =
                EconomicAccountingAudit.checkpoint(manager);

        manager.consume(
                inventory.id(), GRAIN, 15L, "food"
        );

        assertDoesNotThrow(
                () -> EconomicAccountingAudit.verify(
                        manager, checkpoint
                )
        );

        assertEquals(85L, inventory.quantity(GRAIN));
    }

    @Test
    void newlyCreatedEmptyInventoryIsPermitted() {
        InventoryManager manager = manager();

        var checkpoint =
                EconomicAccountingAudit.checkpoint(manager);

        ResourceInventory inventory = create(manager, 1L);

        assertDoesNotThrow(
                () -> EconomicAccountingAudit.verify(
                        manager, checkpoint
                )
        );

        assertTrue(inventory.balances().isEmpty());
    }

    @Test
    void journalSupportsNewInventoriesAfterCheckpoint() {
        InventoryManager manager = manager();

        var checkpoint =
                EconomicAccountingAudit.checkpoint(manager);

        ResourceInventory inventory = create(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 30L, "new_harvest"
        );

        assertDoesNotThrow(
                () -> EconomicAccountingAudit.verify(
                        manager, checkpoint
                )
        );
    }

    @Test
    void loadedInventoriesBeginNewJournalSession()
            throws IOException {

        ResourceCatalog catalog =
                ResourceCatalog.benchmarkDefaults();

        InventoryManager original =
                new InventoryManager(catalog);

        ResourceInventory source = create(original, 1L);
        ResourceInventory target = create(original, 2L);

        original.produce(
                source.id(), GRAIN, 100L, "harvest"
        );

        InventoryPersistence persistence =
                new InventoryPersistence(tempDirectory);

        persistence.save(original);

        InventoryManager loaded =
                persistence.load(catalog);

        assertEquals(0L, loaded.transactionCount());
        assertEquals(
                100L, loaded.require(source.id()).quantity(GRAIN)
        );

        var checkpoint =
                EconomicAccountingAudit.checkpoint(loaded);

        loaded.transfer(
                source.id(), target.id(),
                GRAIN, 40L, "post_load_trade"
        );

        assertDoesNotThrow(
                () -> EconomicAccountingAudit.verify(
                        loaded, checkpoint
                )
        );

        assertEquals(1L, loaded.transactionCount());
    }

    @Test
    void transactionSnapshotIsImmutable() {
        InventoryManager manager = manager();

        ResourceInventory inventory = create(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 10L, "harvest"
        );

        List<EconomicTransaction> snapshot =
                manager.transactions();

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.clear()
        );
    }

    @Test
    void invalidTransactionRecordsAreRejected() {
        InventoryId one = new InventoryId(1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> new EconomicTransaction(
                        1L,
                        EconomicTransaction.Kind.TRANSFER,
                        one, one, GRAIN, 5L, "transfer"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new EconomicTransaction(
                        1L,
                        EconomicTransaction.Kind.PRODUCTION,
                        one, null, GRAIN, 5L, "production"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new EconomicTransaction(
                        1L,
                        EconomicTransaction.Kind.CONSUMPTION,
                        one, null, GRAIN, 0L, "food"
                )
        );
    }
}
