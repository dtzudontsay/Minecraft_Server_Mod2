
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

public final class EconomyJournalRotationTest {

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

    private void activateSnapshot() throws IOException {
        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(),
                GRAIN,
                100L,
                "initial_harvest"
        );

        new InventoryPersistence(directory).save(manager);

        new EconomySnapshotStore(directory).write(
                EconomySnapshotStore.capture(manager)
        );

        EconomyAuthorityService.activateMatchingSnapshot(
                directory, catalog()
        );
    }

    private EconomyAuthorityService.Loaded load()
            throws IOException {
        return EconomyAuthorityService.load(
                directory, catalog()
        );
    }

    private void save(
            EconomyAuthorityService.Loaded loaded
    ) throws IOException {
        EconomyAuthorityService.save(
                directory, catalog(), loaded
        );
    }

    private InventoryId firstId(
            EconomyAuthorityService.Loaded loaded
    ) {
        return loaded.manager().all()
                .iterator().next().id();
    }

    @Test
    void verifiedSaveClearsOnlyWorkingJournal()
            throws IOException {

        activateSnapshot();

        var loaded = load();
        InventoryId id = firstId(loaded);

        loaded.manager().produce(
                id, GRAIN, 20L, "new_harvest"
        );

        assertEquals(
                1L,
                loaded.manager().transactionCount()
        );

        save(loaded);

        assertEquals(
                0L,
                loaded.manager().transactionCount()
        );

        assertEquals(
                1L,
                loaded.manager().journalGeneration()
        );

        assertEquals(
                120L,
                loaded.manager().totalQuantity(GRAIN)
        );

        assertEquals(
                2,
                loaded.previousTransactions().size()
        );
    }

    @Test
    void nextTransactionSequenceRestartsAfterRotation()
            throws IOException {

        activateSnapshot();

        var loaded = load();
        InventoryId id = firstId(loaded);

        loaded.manager().produce(
                id, GRAIN, 10L, "first"
        );

        save(loaded);

        loaded.manager().produce(
                id, GRAIN, 20L, "second"
        );

        assertEquals(
                1L,
                loaded.manager().transactions()
                        .get(0).sequence()
        );

        save(loaded);

        assertEquals(
                0L,
                loaded.manager().transactionCount()
        );

        assertEquals(
                2L,
                loaded.manager().journalGeneration()
        );

        assertEquals(
                130L,
                load().manager().totalQuantity(GRAIN)
        );
    }

    @Test
    void consecutiveCheckpointsPreserveHistory()
            throws IOException {

        activateSnapshot();

        var loaded = load();
        InventoryId id = firstId(loaded);

        loaded.manager().produce(
                id, GRAIN, 15L, "first_session_operation"
        );
        save(loaded);

        loaded.manager().consume(
                id, GRAIN, 5L, "second_session_operation"
        );
        save(loaded);

        var reloaded = load();

        assertEquals(
                110L,
                reloaded.manager().totalQuantity(GRAIN)
        );

        assertEquals(
                3,
                reloaded.previousTransactions().size()
        );

        assertEquals(
                "initial_harvest",
                reloaded.previousTransactions()
                        .get(0).cause()
        );

        assertEquals(
                "first_session_operation",
                reloaded.previousTransactions()
                        .get(1).cause()
        );

        assertEquals(
                "second_session_operation",
                reloaded.previousTransactions()
                        .get(2).cause()
        );
    }

    @Test
    void repeatedSaveDoesNotDuplicateHistory()
            throws IOException {

        activateSnapshot();

        var loaded = load();
        InventoryId id = firstId(loaded);

        loaded.manager().produce(
                id, GRAIN, 25L, "once"
        );

        save(loaded);
        save(loaded);
        save(loaded);

        assertEquals(
                2,
                load().previousTransactions().size()
        );

        assertEquals(
                125L,
                load().manager().totalQuantity(GRAIN)
        );
    }

    @Test
    void failedCommitLeavesJournalAndBaselineIntact()
            throws IOException {

        activateSnapshot();

        var loaded = load();
        InventoryId id = firstId(loaded);

        loaded.manager().produce(
                id, GRAIN, 50L, "unsaved"
        );

        List<EconomicTransaction> baseline =
                loaded.previousTransactions();

        Files.writeString(
                new EconomySnapshotStore(directory).file(),
                "CORRUPTED\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> save(loaded)
        );

        assertEquals(
                1L,
                loaded.manager().transactionCount()
        );

        assertEquals(
                0L,
                loaded.manager().journalGeneration()
        );

        assertEquals(
                baseline,
                loaded.previousTransactions()
        );

        assertEquals(
                150L,
                loaded.manager().totalQuantity(GRAIN)
        );
    }

    @Test
    void rotationRejectsMismatchedTransactions() {
        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager);

        manager.produce(
                inventory.id(), GRAIN, 10L, "first"
        );

        List<EconomicTransaction> captured =
                manager.transactions();

        manager.produce(
                inventory.id(), GRAIN, 5L, "second"
        );

        assertThrows(
                IllegalStateException.class,
                () -> manager.rotateCommittedJournal(
                        captured
                )
        );

        assertEquals(2L, manager.transactionCount());
        assertEquals(15L, inventory.quantity(GRAIN));
        assertEquals(0L, manager.journalGeneration());
    }

    @Test
    void multiSessionReloadPreservesCommittedHistory()
            throws IOException {

        activateSnapshot();

        var sessionOne = load();
        InventoryId id = firstId(sessionOne);

        sessionOne.manager().produce(
                id, GRAIN, 40L, "session_one"
        );
        save(sessionOne);

        var sessionTwo = load();

        sessionTwo.manager().consume(
                id, GRAIN, 10L, "session_two"
        );
        save(sessionTwo);

        var sessionThree = load();

        assertEquals(
                130L,
                sessionThree.manager()
                        .totalQuantity(GRAIN)
        );

        assertEquals(
                3,
                sessionThree.previousTransactions().size()
        );

        assertEquals(
                "session_two",
                sessionThree.previousTransactions()
                        .get(2).cause()
        );
    }

    @Test
    void snapshotPrimaryDoesNotModifyLegacySave()
            throws IOException {

        activateSnapshot();

        Path legacy =
                new InventoryPersistence(directory).file();

        byte[] original = Files.readAllBytes(legacy);

        var loaded = load();

        loaded.manager().produce(
                firstId(loaded),
                GRAIN,
                30L,
                "snapshot_only_harvest"
        );

        save(loaded);

        assertArrayEquals(
                original,
                Files.readAllBytes(legacy)
        );

        assertEquals(
                130L,
                load().manager().totalQuantity(GRAIN)
        );
    }

    @Test
    void historySnapshotIsImmutable() throws IOException {
        activateSnapshot();

        var loaded = load();
        List<EconomicTransaction> historical =
                loaded.previousTransactions();

        assertThrows(
                UnsupportedOperationException.class,
                () -> historical.clear()
        );

        assertEquals(1, historical.size());
    }

    @Test
    void normalSaveCreatesRecoveryGeneration()
            throws IOException {

        activateSnapshot();

        var loaded = load();

        loaded.manager().produce(
                firstId(loaded),
                GRAIN,
                10L,
                "checkpoint"
        );

        save(loaded);

        assertTrue(Files.exists(
                new EconomySnapshotRecovery(directory)
                        .backupFile()
        ));

        assertEquals(
                110L,
                load().manager().totalQuantity(GRAIN)
        );
    }
}
