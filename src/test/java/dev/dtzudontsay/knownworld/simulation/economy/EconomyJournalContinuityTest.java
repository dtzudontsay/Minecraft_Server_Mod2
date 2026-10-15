
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class EconomyJournalContinuityTest {

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
                100L,
                "initial_harvest"
        );

        return manager;
    }

    private void activate() throws IOException {
        InventoryManager manager = populatedManager();

        new InventoryPersistence(directory).save(manager);

        new EconomySnapshotStore(directory).write(
                EconomySnapshotStore.capture(manager)
        );

        EconomyAuthorityService.activateMatchingSnapshot(
                directory, catalog()
        );
    }

    private EconomicTransaction transaction(
            long sequence,
            String cause
    ) {
        return new EconomicTransaction(
                sequence,
                EconomicTransaction.Kind.PRODUCTION,
                null,
                new InventoryId(1L),
                GRAIN,
                1L,
                cause
        );
    }

    @Test
    void emptyHistoryRemainsEmpty() {
        assertTrue(
                EconomyJournalContinuity.merge(
                        List.of(),
                        List.of()
                ).isEmpty()
        );
    }

    @Test
    void previousAndCurrentHistoryAreCombined() {
        List<EconomicTransaction> result =
                EconomyJournalContinuity.merge(
                        List.of(
                                transaction(1L, "old_1"),
                                transaction(2L, "old_2")
                        ),
                        List.of(
                                transaction(1L, "new_1"),
                                transaction(2L, "new_2")
                        ),
                        10
                );

        assertEquals(4, result.size());
        assertEquals("old_1", result.get(0).cause());
        assertEquals("new_2", result.get(3).cause());
        assertEquals(4L, result.get(3).sequence());
    }

    @Test
    void retentionDropsOldestRecordsFirst() {
        List<EconomicTransaction> result =
                EconomyJournalContinuity.merge(
                        List.of(
                                transaction(1L, "old_1"),
                                transaction(2L, "old_2"),
                                transaction(3L, "old_3")
                        ),
                        List.of(
                                transaction(1L, "new_1"),
                                transaction(2L, "new_2")
                        ),
                        3
                );

        assertEquals(3, result.size());
        assertEquals("old_3", result.get(0).cause());
        assertEquals("new_1", result.get(1).cause());
        assertEquals("new_2", result.get(2).cause());
        assertEquals(1L, result.get(0).sequence());
        assertEquals(3L, result.get(2).sequence());
    }

    @Test
    void largeCurrentSessionRetainsMostRecentRecords() {
        List<EconomicTransaction> result =
                EconomyJournalContinuity.merge(
                        List.of(transaction(1L, "old")),
                        List.of(
                                transaction(1L, "new_1"),
                                transaction(2L, "new_2"),
                                transaction(3L, "new_3"),
                                transaction(4L, "new_4")
                        ),
                        2
                );

        assertEquals(2, result.size());
        assertEquals("new_3", result.get(0).cause());
        assertEquals("new_4", result.get(1).cause());
    }

    @Test
    void invalidSequenceIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EconomyJournalContinuity.merge(
                        List.of(transaction(2L, "bad")),
                        List.of(),
                        10
                )
        );
    }

    @Test
    void invalidRetentionLimitIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> EconomyJournalContinuity.merge(
                        List.of(),
                        List.of(),
                        0
                )
        );
    }

    @Test
    void snapshotCapturePreservesCurrentBalances() {
        InventoryManager manager = populatedManager();

        EconomySnapshotStore.Snapshot snapshot =
                EconomyJournalContinuity.capture(
                        manager,
                        List.of(
                                transaction(1L, "old_record")
                        ),
                        10
                );

        assertEquals(100L,
                snapshot.inventories()
                        .get(0)
                        .balances()
                        .get(GRAIN));

        assertEquals(2, snapshot.transactions().size());
        assertEquals(
                "old_record",
                snapshot.transactions().get(0).cause()
        );
    }

    @Test
    void transactionsSurviveMultipleServerSessions()
            throws IOException {

        activate();

        var firstSession = EconomyAuthorityService.load(
                directory, catalog()
        );

        InventoryId id = firstSession.manager()
                .all().iterator().next().id();

        firstSession.manager().produce(
                id, GRAIN, 20L, "session_one"
        );

        EconomyAuthorityService.save(
                directory, catalog(), firstSession
        );

        var secondSession = EconomyAuthorityService.load(
                directory, catalog()
        );

        assertEquals(2,
                secondSession.previousTransactions().size());

        assertEquals(
                "session_one",
                secondSession.previousTransactions()
                        .get(1).cause()
        );

        secondSession.manager().consume(
                id, GRAIN, 5L, "session_two"
        );

        EconomyAuthorityService.save(
                directory, catalog(), secondSession
        );

        var thirdSession = EconomyAuthorityService.load(
                directory, catalog()
        );

        assertEquals(
                115L,
                thirdSession.manager().totalQuantity(GRAIN)
        );

        assertEquals(
                3,
                thirdSession.previousTransactions().size()
        );

        assertEquals(
                "session_two",
                thirdSession.previousTransactions()
                        .get(2).cause()
        );
    }

    @Test
    void repeatedSaveDoesNotDuplicateTransactions()
            throws IOException {

        activate();

        var loaded = EconomyAuthorityService.load(
                directory, catalog()
        );

        InventoryId id = loaded.manager()
                .all().iterator().next().id();

        loaded.manager().produce(
                id, GRAIN, 25L, "single_operation"
        );

        EconomyAuthorityService.save(
                directory, catalog(), loaded
        );

        EconomyAuthorityService.save(
                directory, catalog(), loaded
        );

        var reloaded = EconomyAuthorityService.load(
                directory, catalog()
        );

        assertEquals(
                2,
                reloaded.previousTransactions().size()
        );

        assertEquals(
                125L,
                reloaded.manager().totalQuantity(GRAIN)
        );
    }

    @Test
    void returnedHistoryCannotBeModified() {
        List<EconomicTransaction> merged =
                EconomyJournalContinuity.merge(
                        List.of(),
                        List.of(transaction(1L, "test")),
                        10
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> merged.clear()
        );
    }
}
