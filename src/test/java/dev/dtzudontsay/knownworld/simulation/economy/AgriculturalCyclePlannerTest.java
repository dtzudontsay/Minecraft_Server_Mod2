
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class AgriculturalCyclePlannerTest {

    private AgriculturalCyclePlanner.Inputs inputs(
            InventoryId farm,
            InventoryId household,
            int allocation
    ) {
        return new AgriculturalCyclePlanner.Inputs(
                farm,
                household,
                new AgriculturalYieldModel.Inputs(
                        10L,
                        5L,
                        1000L,
                        2000L,
                        1000,
                        1000,
                        1000
                ),
                new HouseholdFoodDemandModel.Inputs(
                        5L,
                        10L,
                        800L
                ),
                allocation
        );
    }

    @Test
    void calculatesHarvestAndFoodRequirement() {
        var planned = AgriculturalCyclePlanner.plan(
                inputs(
                        new InventoryId(1L),
                        new InventoryId(2L),
                        500
                )
        );

        assertEquals(10000L, planned.yield().harvestKg());
        assertEquals(40L, planned.demand().requiredKg());
        assertEquals(5000L, planned.cycle().distributionKg());
    }

    @Test
    void fullAllocationUsesEntireHarvest() {
        var planned = AgriculturalCyclePlanner.plan(
                inputs(
                        new InventoryId(1L),
                        new InventoryId(2L),
                        1000
                )
        );

        assertEquals(
                planned.yield().harvestKg(),
                planned.cycle().distributionKg()
        );
    }

    @Test
    void zeroAllocationDistributesNothing() {
        var planned = AgriculturalCyclePlanner.plan(
                inputs(
                        new InventoryId(1L),
                        new InventoryId(2L),
                        0
                )
        );

        assertEquals(0L, planned.cycle().distributionKg());
    }

    @Test
    void rejectsExcessiveAllocation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> inputs(
                        new InventoryId(1L),
                        new InventoryId(2L),
                        1001
                )
        );
    }

    @Test
    void rejectsNegativeAllocation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> inputs(
                        new InventoryId(1L),
                        new InventoryId(2L),
                        -1
                )
        );
    }

    @Test
    void planningNeverMutatesInventories() {
        InventoryManager manager = new InventoryManager(
                ResourceCatalog.benchmarkDefaults()
        );

        var farm = manager.create(
                EconomicOwner.npc(new NpcId(1L)),
                new SettlementId(1L)
        );

        var household = manager.create(
                EconomicOwner.npc(new NpcId(2L)),
                new SettlementId(1L)
        );

        AgriculturalCyclePlanner.plan(
                inputs(farm.id(), household.id(), 500)
        );

        assertEquals(
                0L,
                manager.totalQuantity(new ResourceId("grain"))
        );

        assertEquals(0L, manager.transactionCount());
    }

    @Test
    void calculatedPlanCanExecute() {
        InventoryManager manager = new InventoryManager(
                ResourceCatalog.benchmarkDefaults()
        );

        var farm = manager.create(
                EconomicOwner.npc(new NpcId(1L)),
                new SettlementId(1L)
        );

        var household = manager.create(
                EconomicOwner.npc(new NpcId(2L)),
                new SettlementId(1L)
        );

        var planned = AgriculturalCyclePlanner.plan(
                inputs(farm.id(), household.id(), 500)
        );

        var result = AgriculturalFoodCycle.execute(
                manager,
                planned.cycle()
        );

        assertEquals(10000L, result.harvestedKg());
        assertEquals(5000L, result.distributedKg());
        assertEquals(40L, result.consumedKg());
        assertEquals(0L, result.shortageKg());

        assertEquals(5000L, result.farmStockKg());
        assertEquals(4960L, result.householdStockKg());

        assertEquals(3L, manager.transactionCount());
    }

    @Test
    void plannedIdsRemainUnchanged() {
        var farm = new InventoryId(12L);
        var household = new InventoryId(25L);

        var planned = AgriculturalCyclePlanner.plan(
                inputs(farm, household, 500)
        );

        assertEquals(farm, planned.cycle().farm());
        assertEquals(household, planned.cycle().household());
    }

    @Test
    void unfavorableSeasonReducesPlannedHarvest() {
        var normal = inputs(
                new InventoryId(1L),
                new InventoryId(2L),
                500
        );

        var unfavorable = new AgriculturalCyclePlanner.Inputs(
                normal.farm(),
                normal.household(),
                new AgriculturalYieldModel.Inputs(
                        10L, 5L,
                        1000L, 2000L,
                        1000, 1000, 250
                ),
                normal.foodDemand(),
                500
        );

        assertTrue(
                AgriculturalCyclePlanner.plan(unfavorable)
                        .yield().harvestKg()
                        < AgriculturalCyclePlanner.plan(normal)
                        .yield().harvestKg()
        );
    }

    @Test
    void identicalPlansHaveIdenticalResults() {
        var inputs = inputs(
                new InventoryId(1L),
                new InventoryId(2L),
                500
        );

        assertEquals(
                AgriculturalCyclePlanner.plan(inputs),
                AgriculturalCyclePlanner.plan(inputs)
        );
    }
}
