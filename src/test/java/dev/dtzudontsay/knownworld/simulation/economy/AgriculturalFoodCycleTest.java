
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public final class AgriculturalFoodCycleTest {

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
            InventoryManager manager,
            long npc,
            long settlement
    ) {
        return manager.create(
                EconomicOwner.npc(new NpcId(npc)),
                new SettlementId(settlement)
        );
    }

    @Test
    void normalFarmHouseholdCycle() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        AgriculturalFoodCycle.Result result =
                AgriculturalFoodCycle.execute(
                        manager,
                        new AgriculturalFoodCycle.Plan(
                                farm.id(),
                                household.id(),
                                100L,
                                60L,
                                40L
                        )
                );

        assertEquals(100L, result.harvestedKg());
        assertEquals(60L, result.distributedKg());
        assertEquals(40L, result.consumedKg());
        assertEquals(0L, result.shortageKg());

        assertEquals(40L, farm.quantity(GRAIN));
        assertEquals(20L, household.quantity(GRAIN));

        assertEquals(3, result.recordedTransactions());
        assertEquals(3L, manager.transactionCount());

        assertTrue(result.foodRequirementMet());
    }

    @Test
    void insufficientHarvestCausesFoodShortage() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        var result = AgriculturalFoodCycle.execute(
                manager,
                new AgriculturalFoodCycle.Plan(
                        farm.id(),
                        household.id(),
                        20L,
                        50L,
                        50L
                )
        );

        assertEquals(20L, result.distributedKg());
        assertEquals(20L, result.consumedKg());
        assertEquals(30L, result.shortageKg());

        assertEquals(0L, farm.quantity(GRAIN));
        assertEquals(0L, household.quantity(GRAIN));

        assertFalse(result.foodRequirementMet());
    }

    @Test
    void householdConsumesPreviouslyStoredGrain() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        manager.produce(
                household.id(),
                GRAIN,
                30L,
                "test:initial_stock"
        );

        var result = AgriculturalFoodCycle.execute(
                manager,
                new AgriculturalFoodCycle.Plan(
                        farm.id(),
                        household.id(),
                        0L,
                        0L,
                        20L
                )
        );

        assertEquals(20L, result.consumedKg());
        assertEquals(0L, result.shortageKg());
        assertEquals(10L, household.quantity(GRAIN));
        assertEquals(1, result.recordedTransactions());
    }

    @Test
    void existingFarmStockSupportsDistribution() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        manager.produce(
                farm.id(),
                GRAIN,
                80L,
                "test:initial_stock"
        );

        var result = AgriculturalFoodCycle.execute(
                manager,
                new AgriculturalFoodCycle.Plan(
                        farm.id(),
                        household.id(),
                        0L,
                        50L,
                        30L
                )
        );

        assertEquals(50L, result.distributedKg());
        assertEquals(30L, result.consumedKg());
        assertEquals(30L, farm.quantity(GRAIN));
        assertEquals(20L, household.quantity(GRAIN));
    }

    @Test
    void zeroCycleDoesNotCreateTransactions() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        var result = AgriculturalFoodCycle.execute(
                manager,
                new AgriculturalFoodCycle.Plan(
                        farm.id(),
                        household.id(),
                        0L,
                        0L,
                        0L
                )
        );

        assertEquals(0, result.recordedTransactions());
        assertEquals(0L, manager.transactionCount());
        assertEquals(0L, result.shortageKg());
    }

    @Test
    void negativeInputsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AgriculturalFoodCycle.Plan(
                        new InventoryId(1L),
                        new InventoryId(2L),
                        -1L,
                        10L,
                        10L
                )
        );
    }

    @Test
    void sameInventoryCannotBeFarmAndHousehold() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AgriculturalFoodCycle.Plan(
                        new InventoryId(1L),
                        new InventoryId(1L),
                        10L,
                        10L,
                        10L
                )
        );
    }

    @Test
    void differentSettlementLocationsAreRejected() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 2L);

        assertThrows(
                IllegalArgumentException.class,
                () -> AgriculturalFoodCycle.execute(
                        manager,
                        new AgriculturalFoodCycle.Plan(
                                farm.id(),
                                household.id(),
                                50L,
                                30L,
                                20L
                        )
                )
        );

        assertEquals(0L, manager.transactionCount());
    }

    @Test
    void insufficientJournalCapacityRejectsWholeCycle() {
        InventoryManager manager =
                new InventoryManager(catalog(), 2);

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        assertThrows(
                IllegalStateException.class,
                () -> AgriculturalFoodCycle.execute(
                        manager,
                        new AgriculturalFoodCycle.Plan(
                                farm.id(),
                                household.id(),
                                100L,
                                50L,
                                25L
                        )
                )
        );

        assertEquals(0L, farm.quantity(GRAIN));
        assertEquals(0L, household.quantity(GRAIN));
        assertEquals(0L, manager.transactionCount());
    }

    @Test
    void overflowRejectsCycleBeforeMutation() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        manager.produce(
                farm.id(),
                GRAIN,
                Long.MAX_VALUE,
                "test:full_inventory"
        );

        assertThrows(
                ArithmeticException.class,
                () -> AgriculturalFoodCycle.execute(
                        manager,
                        new AgriculturalFoodCycle.Plan(
                                farm.id(),
                                household.id(),
                                1L,
                                1L,
                                1L
                        )
                )
        );

        assertEquals(
                Long.MAX_VALUE,
                farm.quantity(GRAIN)
        );

        assertEquals(0L, household.quantity(GRAIN));
        assertEquals(1L, manager.transactionCount());
    }

    @Test
    void journalRecordsActualEconomicOperations() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        AgriculturalFoodCycle.execute(
                manager,
                new AgriculturalFoodCycle.Plan(
                        farm.id(),
                        household.id(),
                        60L,
                        40L,
                        30L
                )
        );

        var transactions = manager.transactions();

        assertEquals(
                EconomicTransaction.Kind.PRODUCTION,
                transactions.get(0).kind()
        );

        assertEquals(
                EconomicTransaction.Kind.TRANSFER,
                transactions.get(1).kind()
        );

        assertEquals(
                EconomicTransaction.Kind.CONSUMPTION,
                transactions.get(2).kind()
        );

        assertEquals(
                "agriculture:grain_harvest",
                transactions.get(0).cause()
        );

        assertEquals(
                "household:grain_consumption",
                transactions.get(2).cause()
        );
    }

    @Test
    void cycleStateSurvivesSnapshotRoundTrip()
            throws IOException {

        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        AgriculturalFoodCycle.execute(
                manager,
                new AgriculturalFoodCycle.Plan(
                        farm.id(),
                        household.id(),
                        100L,
                        60L,
                        40L
                )
        );

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(
                EconomySnapshotStore.capture(manager)
        );

        var snapshot = store.read(catalog());

        InventoryManager restored =
                EconomySnapshotStore.restore(
                        snapshot, catalog()
                );

        assertEquals(
                40L,
                restored.require(farm.id()).quantity(GRAIN)
        );

        assertEquals(
                20L,
                restored.require(household.id()).quantity(GRAIN)
        );

        assertEquals(3, snapshot.transactions().size());
    }

    @Test
    void repeatedCyclesAccumulateRealBalances() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        var plan = new AgriculturalFoodCycle.Plan(
                farm.id(),
                household.id(),
                100L,
                60L,
                40L
        );

        AgriculturalFoodCycle.execute(manager, plan);
        AgriculturalFoodCycle.execute(manager, plan);

        assertEquals(80L, farm.quantity(GRAIN));
        assertEquals(40L, household.quantity(GRAIN));
        assertEquals(6L, manager.transactionCount());
    }

    @Test
    void previouslyStoredFoodCanEliminateShortage() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        manager.produce(
                household.id(),
                GRAIN,
                20L,
                "test:stored_food"
        );

        var result = AgriculturalFoodCycle.execute(
                manager,
                new AgriculturalFoodCycle.Plan(
                        farm.id(),
                        household.id(),
                        10L,
                        10L,
                        30L
                )
        );

        assertEquals(30L, result.consumedKg());
        assertEquals(0L, result.shortageKg());
        assertEquals(0L, household.quantity(GRAIN));
    }
}
