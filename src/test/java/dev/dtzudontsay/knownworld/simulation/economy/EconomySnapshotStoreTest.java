
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

public final class EconomySnapshotStoreTest {

    @TempDir
    Path directory;

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private ResourceCatalog catalog() {
        return ResourceCatalog.benchmarkDefaults();
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
    void snapshotPreservesBalancesAndJournal()
            throws IOException {

        ResourceCatalog catalog = catalog();

        InventoryManager manager =
                new InventoryManager(catalog);

        ResourceInventory farm = create(manager, 1L);
        ResourceInventory household = create(manager, 2L);

        manager.produce(
                farm.id(), GRAIN, 200L, "harvest"
        );

        manager.transfer(
                farm.id(),
                household.id(),
                GRAIN,
                50L,
                "allocation"
        );

        manager.consume(
                household.id(),
                GRAIN,
                10L,
                "meals"
        );

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(EconomySnapshotStore.capture(manager));

        EconomySnapshotStore.Snapshot snapshot =
                store.read(catalog);

        assertEquals(2, snapshot.inventories().size());
        assertEquals(3, snapshot.transactions().size());
        assertEquals("harvest",
                snapshot.transactions().get(0).cause());

        InventoryManager restored =
                EconomySnapshotStore.restore(
                        snapshot, catalog
                );

        assertEquals(150L,
                restored.require(farm.id()).quantity(GRAIN));

        assertEquals(40L,
                restored.require(household.id()).quantity(GRAIN));

        assertEquals(190L,
                restored.totalQuantity(GRAIN));

        assertEquals(3L, restored.nextId());

        // Restored balances are authoritative.
        // The new manager begins a new journal session.
        assertEquals(0L, restored.transactionCount());
    }

    @Test
    void snapshotPreservesNonAsciiTransactionCauses()
            throws IOException {

        InventoryManager manager =
                new InventoryManager(catalog());

        ResourceInventory inventory = create(manager, 1L);

        manager.produce(
                inventory.id(),
                GRAIN,
                25L,
                "récolte / mùa gặt"
        );

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(EconomySnapshotStore.capture(manager));

        assertEquals(
                "récolte / mùa gặt",
                store.read(catalog())
                        .transactions().get(0).cause()
        );
    }

    @Test
    void corruptSnapshotIsRejected() throws IOException {
        InventoryManager manager =
                new InventoryManager(catalog());

        ResourceInventory inventory = create(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 100L, "harvest"
        );

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(EconomySnapshotStore.capture(manager));

        String content = Files.readString(
                store.file(), StandardCharsets.UTF_8
        );

        Files.writeString(
                store.file(),
                content.replace(
                        "\t100\n", "\t999\n"
                ),
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> store.read(catalog())
        );
    }

    @Test
    void missingSnapshotDoesNotSilentlyReset()
            throws IOException {

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        assertFalse(Files.exists(store.file()));

        assertThrows(
                IOException.class,
                () -> store.read(catalog())
        );
    }

    @Test
    void emptySnapshotRoundTrips() throws IOException {
        InventoryManager manager =
                new InventoryManager(catalog());

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(EconomySnapshotStore.capture(manager));

        EconomySnapshotStore.Snapshot restored =
                store.read(catalog());

        assertTrue(restored.inventories().isEmpty());
        assertTrue(restored.transactions().isEmpty());
        assertEquals(1L, restored.nextInventoryId());
    }

    @Test
    void snapshotIsIndependentOfLaterMutations() {
        InventoryManager manager =
                new InventoryManager(catalog());

        ResourceInventory inventory = create(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 10L, "first"
        );

        EconomySnapshotStore.Snapshot snapshot =
                EconomySnapshotStore.capture(manager);

        manager.produce(
                inventory.id(), GRAIN, 20L, "second"
        );

        assertEquals(
                10L,
                snapshot.inventories().get(0)
                        .balances().get(GRAIN)
        );

        assertEquals(1, snapshot.transactions().size());
        assertEquals(30L, inventory.quantity(GRAIN));
    }

    @Test
    void snapshotRejectsNonconsecutiveTransactions() {
        EconomicTransaction transaction =
                new EconomicTransaction(
                        2L,
                        EconomicTransaction.Kind.PRODUCTION,
                        null,
                        new InventoryId(1L),
                        GRAIN,
                        10L,
                        "harvest"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> new EconomySnapshotStore.Snapshot(
                        1L,
                        List.of(),
                        List.of(transaction)
                )
        );
    }

    @Test
    void snapshotFileLeavesNoTemporaryFile()
            throws IOException {

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(EconomySnapshotStore.capture(
                new InventoryManager(catalog())
        ));

        assertFalse(Files.exists(
                directory.resolve(
                        "economy_snapshot_v1.tsv.tmp"
                )
        ));
    }

    @Test
    void snapshotCanBeWrittenTwice() throws IOException {
        InventoryManager manager =
                new InventoryManager(catalog());

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(EconomySnapshotStore.capture(manager));

        ResourceInventory inventory = create(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 15L, "harvest"
        );

        store.write(EconomySnapshotStore.capture(manager));

        EconomySnapshotStore.Snapshot latest =
                store.read(catalog());

        assertEquals(1, latest.inventories().size());
        assertEquals(1, latest.transactions().size());
    }
}
