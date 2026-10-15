
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class SettlementFoodCycleTest {

    @TempDir
    Path directory;

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private InventoryManager manager() {
        return new InventoryManager(
                ResourceCatalog.benchmarkDefaults()
        );
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

    /**
     * Creates deterministic agricultural yield inputs.
     *
     * Deliberately NOT named "yield", because yield
     * is a restricted identifier in modern Java.
     */
    private AgriculturalYieldModel.Inputs yieldInputs(
            long kg
    ) {
        return new AgriculturalYieldModel.Inputs(
                1L,
                1L,
                kg,
                kg,
                1000,
                1000,
                1000
        );
    }

    private AgriculturalInterval interval(
            long start,
            long end
    ) {
        return new AgriculturalInterval(
                start,
                end,
                30L
        );
    }

    private SettlementFoodCycle.Household household(
            ResourceInventory inventory,
            long population
    ) {
        return new SettlementFoodCycle.Household(
                inventory.id(),
                population,
                1000L
        );
    }

    @Test
    void intervalCrossingHarvestBoundaryProducesGrain() {
        assertTrue(
                interval(29L, 30L).harvestDue()
        );

        assertFalse(
                interval(30L, 31L).harvestDue()
        );

        assertFalse(
                interval(0L, 29L).harvestDue()
        );

        assertTrue(
                interval(59L, 60L).harvestDue()
        );
    }

    @Test
    void invalidIntervalsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> interval(10L, 10L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> interval(-1L, 5L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> interval(0L, 31L)
        );
    }

    @Test
    void threeHouseholdsReceiveProportionalRations() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory a =
                create(manager, 2L, 1L);

        ResourceInventory b =
                create(manager, 3L, 1L);

        ResourceInventory c =
                create(manager, 4L, 1L);

        manager.produce(
                farm.id(),
                GRAIN,
                80L,
                "test:existing_grain"
        );

        SettlementFoodCycle.Result result =
                SettlementFoodCycle.execute(
                        manager,
                        new SettlementFoodCycle.Inputs(
                                farm.id(),
                                interval(0L, 1L),
                                yieldInputs(0L),
                                0L,
                                List.of(
                                        household(a, 50L),
                                        household(b, 30L),
                                        household(c, 20L)
                                )
                        )
                );

        assertEquals(
                80L,
                result.distributedKg()
        );

        assertEquals(
                80L,
                result.consumedKg()
        );

        assertEquals(
                20L,
                result.shortageKg()
        );

        assertEquals(
                40L,
                result.households()
                        .get(0)
                        .allocationKg()
        );

        assertEquals(
                24L,
                result.households()
                        .get(1)
                        .allocationKg()
        );

        assertEquals(
                16L,
                result.households()
                        .get(2)
                        .allocationKg()
        );

        assertEquals(
                0L,
                farm.quantity(GRAIN)
        );
    }

    @Test
    void harvestOccursOnlyOnBoundary() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        SettlementFoodCycle.Result before =
                SettlementFoodCycle.execute(
                        manager,
                        new SettlementFoodCycle.Inputs(
                                farm.id(),
                                interval(0L, 29L),
                                yieldInputs(100L),
                                0L,
                                List.of()
                        )
                );

        assertEquals(
                0L,
                before.harvestedKg()
        );

        SettlementFoodCycle.Result harvest =
                SettlementFoodCycle.execute(
                        manager,
                        new SettlementFoodCycle.Inputs(
                                farm.id(),
                                interval(29L, 30L),
                                yieldInputs(100L),
                                0L,
                                List.of()
                        )
                );

        assertEquals(
                100L,
                harvest.harvestedKg()
        );

        assertEquals(
                100L,
                farm.quantity(GRAIN)
        );
    }

    @Test
    void farmReservesAreProtected() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        manager.produce(
                farm.id(),
                GRAIN,
                100L,
                "initial_stock"
        );

        SettlementFoodCycle.Result result =
                SettlementFoodCycle.execute(
                        manager,
                        new SettlementFoodCycle.Inputs(
                                farm.id(),
                                interval(0L, 1L),
                                yieldInputs(0L),
                                40L,
                                List.of(
                                        household(household, 100L)
                                )
                        )
                );

        assertEquals(
                60L,
                result.distributedKg()
        );

        assertEquals(
                40L,
                farm.quantity(GRAIN)
        );

        assertEquals(
                40L,
                result.shortageKg()
        );
    }

    @Test
    void householdExistingStockReducesAllocation() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        manager.produce(
                farm.id(),
                GRAIN,
                100L,
                "farm_stock"
        );

        manager.produce(
                household.id(),
                GRAIN,
                30L,
                "household_stock"
        );

        SettlementFoodCycle.Result result =
                SettlementFoodCycle.execute(
                        manager,
                        new SettlementFoodCycle.Inputs(
                                farm.id(),
                                interval(0L, 1L),
                                yieldInputs(0L),
                                0L,
                                List.of(
                                        household(household, 50L)
                                )
                        )
                );

        assertEquals(
                20L,
                result.distributedKg()
        );

        assertEquals(
                50L,
                result.consumedKg()
        );

        assertEquals(
                80L,
                farm.quantity(GRAIN)
        );
    }

    @Test
    void deterministicTieBreakingUsesInventoryId() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory a =
                create(manager, 2L, 1L);

        ResourceInventory b =
                create(manager, 3L, 1L);

        manager.produce(
                farm.id(),
                GRAIN,
                1L,
                "limited_food"
        );

        SettlementFoodCycle.Result result =
                SettlementFoodCycle.execute(
                        manager,
                        new SettlementFoodCycle.Inputs(
                                farm.id(),
                                interval(0L, 1L),
                                yieldInputs(0L),
                                0L,
                                List.of(
                                        household(b, 1L),
                                        household(a, 1L)
                                )
                        )
                );

        assertEquals(
                a.id(),
                result.households()
                        .get(0)
                        .inventory()
        );

        assertEquals(
                1L,
                result.households()
                        .get(0)
                        .allocationKg()
        );

        assertEquals(
                0L,
                result.households()
                        .get(1)
                        .allocationKg()
        );
    }

    @Test
    void journalCapacityFailureLeavesStocksUnchanged() {
        InventoryManager manager =
                new InventoryManager(
                        ResourceCatalog.benchmarkDefaults(),
                        1
                );

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        assertThrows(
                IllegalStateException.class,
                () -> SettlementFoodCycle.execute(
                        manager,
                        new SettlementFoodCycle.Inputs(
                                farm.id(),
                                interval(29L, 30L),
                                yieldInputs(100L),
                                0L,
                                List.of(
                                        household(household, 50L)
                                )
                        )
                )
        );

        assertEquals(
                0L,
                farm.quantity(GRAIN)
        );

        assertEquals(
                0L,
                household.quantity(GRAIN)
        );

        assertEquals(
                0L,
                manager.transactionCount()
        );
    }

    @Test
    void invalidSettlementLocationIsRejected() {
        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 2L);

        assertThrows(
                IllegalArgumentException.class,
                () -> SettlementFoodCycle.execute(
                        manager,
                        new SettlementFoodCycle.Inputs(
                                farm.id(),
                                interval(29L, 30L),
                                yieldInputs(100L),
                                0L,
                                List.of(
                                        household(household, 50L)
                                )
                        )
                )
        );

        assertEquals(
                0L,
                manager.transactionCount()
        );
    }

    @Test
    void duplicateHouseholdsAreRejected() {
        InventoryId id =
                new InventoryId(2L);

        assertThrows(
                IllegalArgumentException.class,
                () -> new SettlementFoodCycle.Inputs(
                        new InventoryId(1L),
                        interval(0L, 1L),
                        yieldInputs(10L),
                        0L,
                        List.of(
                                new SettlementFoodCycle.Household(
                                        id,
                                        1L,
                                        1000L
                                ),
                                new SettlementFoodCycle.Household(
                                        id,
                                        1L,
                                        1000L
                                )
                        )
                )
        );
    }

    @Test
    void settlementCycleSurvivesSnapshotRoundTrip()
            throws IOException {

        InventoryManager manager = manager();

        ResourceInventory farm =
                create(manager, 1L, 1L);

        ResourceInventory household =
                create(manager, 2L, 1L);

        SettlementFoodCycle.Result result =
                SettlementFoodCycle.execute(
                        manager,
                        new SettlementFoodCycle.Inputs(
                                farm.id(),
                                interval(29L, 30L),
                                yieldInputs(100L),
                                0L,
                                List.of(
                                        household(household, 50L)
                                )
                        )
                );

        assertEquals(
                50L,
                result.farmRemainingKg()
        );

        EconomySnapshotStore store =
                new EconomySnapshotStore(directory);

        store.write(
                EconomySnapshotStore.capture(manager)
        );

        ResourceCatalog catalog =
                ResourceCatalog.benchmarkDefaults();

        InventoryManager restored =
                EconomySnapshotStore.restore(
                        store.read(catalog),
                        catalog
                );

        assertEquals(
                50L,
                restored.require(farm.id())
                        .quantity(GRAIN)
        );

        assertEquals(
                0L,
                restored.require(household.id())
                        .quantity(GRAIN)
        );
    }

    @Test
    void identicalInputsProduceIdenticalResults() {
        InventoryManager first = manager();
        InventoryManager second = manager();

        ResourceInventory firstFarm =
                create(first, 1L, 1L);

        ResourceInventory firstHousehold =
                create(first, 2L, 1L);

        ResourceInventory secondFarm =
                create(second, 1L, 1L);

        ResourceInventory secondHousehold =
                create(second, 2L, 1L);

        SettlementFoodCycle.Result firstResult =
                SettlementFoodCycle.execute(
                        first,
                        new SettlementFoodCycle.Inputs(
                                firstFarm.id(),
                                interval(29L, 30L),
                                yieldInputs(100L),
                                0L,
                                List.of(
                                        household(
                                                firstHousehold,
                                                50L
                                        )
                                )
                        )
                );

        SettlementFoodCycle.Result secondResult =
                SettlementFoodCycle.execute(
                        second,
                        new SettlementFoodCycle.Inputs(
                                secondFarm.id(),
                                interval(29L, 30L),
                                yieldInputs(100L),
                                0L,
                                List.of(
                                        household(
                                                secondHousehold,
                                                50L
                                        )
                                )
                        )
                );

        assertEquals(
                firstResult,
                secondResult
        );

        assertEquals(
                first.transactions(),
                second.transactions()
        );
    }
}
