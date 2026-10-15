
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

public final class InventoryPersistenceTest {

    @TempDir
    Path tempDirectory;

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private static final ResourceId COIN =
            new ResourceId("coin");

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
    void saveLoadPreservesBalances() throws IOException {
        InventoryManager original =
                new InventoryManager(catalog());

        ResourceInventory first = create(original, 1L);
        ResourceInventory second = create(original, 2L);

        original.produce(
                first.id(), GRAIN, 500L, "harvest"
        );

        original.transfer(
                first.id(), second.id(),
                GRAIN, 120L, "trade"
        );

        original.produce(
                second.id(), COIN, 50L, "starting_funds"
        );

        InventoryPersistence persistence =
                new InventoryPersistence(tempDirectory);

        persistence.save(original);

        InventoryManager loaded =
                persistence.load(catalog());

        assertEquals(
                380L,
                loaded.require(first.id()).quantity(GRAIN)
        );

        assertEquals(
                120L,
                loaded.require(second.id()).quantity(GRAIN)
        );

        assertEquals(
                50L,
                loaded.require(second.id()).quantity(COIN)
        );

        assertEquals(
                original.totalQuantity(GRAIN),
                loaded.totalQuantity(GRAIN)
        );
    }

    @Test
    void saveLoadPreservesOwnershipAndLocation()
            throws IOException {

        InventoryManager original =
                new InventoryManager(catalog());

        ResourceInventory inventory = original.create(
                EconomicOwner.npc(new NpcId(42L)),
                new SettlementId(7L)
        );

        InventoryPersistence persistence =
                new InventoryPersistence(tempDirectory);

        persistence.save(original);

        ResourceInventory loaded =
                persistence.load(catalog())
                        .require(inventory.id());

        assertEquals(
                EconomicOwner.Kind.NPC,
                loaded.owner().kind()
        );

        assertEquals(42L, loaded.owner().id());
        assertEquals(7L, loaded.location().value());
    }

    @Test
    void nextIdSurvivesSaveLoad() throws IOException {
        InventoryManager original =
                new InventoryManager(catalog());

        create(original, 1L);
        create(original, 2L);

        InventoryPersistence persistence =
                new InventoryPersistence(tempDirectory);

        persistence.save(original);

        InventoryManager loaded =
                persistence.load(catalog());

        ResourceInventory next = create(loaded, 3L);

        assertEquals(3L, next.id().value());
    }

    @Test
    void missingSaveCreatesEmptyEconomy() throws IOException {
        InventoryPersistence persistence =
                new InventoryPersistence(tempDirectory);

        InventoryManager loaded =
                persistence.load(catalog());

        assertTrue(loaded.all().isEmpty());
        assertEquals(1L, loaded.nextId());
        assertFalse(persistence.exists());
    }

    @Test
    void emptyEconomyRoundTrips() throws IOException {
        InventoryPersistence persistence =
                new InventoryPersistence(tempDirectory);

        InventoryManager original =
                new InventoryManager(catalog());

        persistence.save(original);

        InventoryManager loaded =
                persistence.load(catalog());

        assertTrue(loaded.all().isEmpty());
        assertEquals(1L, loaded.nextId());
        assertTrue(persistence.exists());
    }

    @Test
    void corruptedSaveIsRejected() throws IOException {
        InventoryPersistence persistence =
                new InventoryPersistence(tempDirectory);

        InventoryManager original =
                new InventoryManager(catalog());

        ResourceInventory inventory = create(original, 1L);

        original.produce(
                inventory.id(), GRAIN, 500L, "harvest"
        );

        persistence.save(original);

        String content = Files.readString(
                persistence.file(),
                StandardCharsets.UTF_8
        );

        Files.writeString(
                persistence.file(),
                content.replace(
                        "\t500\n",
                        "\t999\n"
                ),
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> persistence.load(catalog())
        );
    }

    @Test
    void malformedHeaderIsRejected() throws IOException {
        InventoryPersistence persistence =
                new InventoryPersistence(tempDirectory);

        Files.writeString(
                persistence.file(),
                "INVALID_HEADER\nNEXT_ID\t1\nCHECKSUM\tbad\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> persistence.load(catalog())
        );
    }

    @Test
    void truncatedFileIsRejected() throws IOException {
        InventoryPersistence persistence =
                new InventoryPersistence(tempDirectory);

        Files.writeString(
                persistence.file(),
                "KNOWNWORLD_ECONOMY_INVENTORIES\t1\n"
                        + "NEXT_ID\t2\n"
                        + "INVENTORY\t1\tNPC\t1\t1\n",
                StandardCharsets.UTF_8
        );

        assertThrows(
                IOException.class,
                () -> persistence.load(catalog())
        );
    }

    @Test
    void saveDoesNotLeaveTemporaryFile() throws IOException {
        InventoryPersistence persistence =
                new InventoryPersistence(tempDirectory);

        persistence.save(
                new InventoryManager(catalog())
        );

        assertFalse(Files.exists(
                tempDirectory.resolve(
                        "economy_inventories.tsv.tmp"
                )
        ));
    }

    @Test
    void loadedInventoriesRemainOperational()
            throws IOException {

        InventoryManager original =
                new InventoryManager(catalog());

        ResourceInventory source = create(original, 1L);
        ResourceInventory target = create(original, 2L);

        original.produce(
                source.id(), GRAIN, 100L, "harvest"
        );

        InventoryPersistence persistence =
                new InventoryPersistence(tempDirectory);

        persistence.save(original);

        InventoryManager loaded =
                persistence.load(catalog());

        loaded.transfer(
                source.id(),
                target.id(),
                GRAIN,
                40L,
                "post_load_trade"
        );

        assertEquals(60L,
                loaded.require(source.id()).quantity(GRAIN));

        assertEquals(40L,
                loaded.require(target.id()).quantity(GRAIN));

        assertEquals(100L,
                loaded.totalQuantity(GRAIN));
    }
}
