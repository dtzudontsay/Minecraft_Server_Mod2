
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class MultiFarmSettlementFoodCycleTest {

    private static final SettlementId SETTLEMENT =
            new SettlementId(1L);

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private InventoryManager manager() {
        return new InventoryManager(
                ResourceCatalog.benchmarkDefaults()
        );
    }

    private ResourceInventory farm(
            InventoryManager manager,
            long holding
    ) {
        return manager.create(
                EconomicOwner.holding(new HoldingId(holding)),
                SETTLEMENT
        );
    }

    private ResourceInventory household(
            InventoryManager manager,
            long npc
    ) {
        return manager.create(
                EconomicOwner.npc(new NpcId(npc)),
                SETTLEMENT
        );
    }

    private MultiFarmSettlementFoodCycle.Farm farmInput(
            long holding,
            ResourceInventory inventory,
            long yieldKg,
            long reserveKg
    ) {
        return new MultiFarmSettlementFoodCycle.Farm(
                new HoldingId(holding),
                inventory.id(),
                30L,
                new AgriculturalYieldModel.Inputs(
                        1L, 1L,
                        yieldKg, yieldKg,
                        1000, 1000, 1000
                ),
                reserveKg
        );
    }

    private SettlementFoodCycle.Household householdInput(
            ResourceInventory inventory,
            long demandKg
    ) {
        return new SettlementFoodCycle.Household(
                inventory.id(),
                demandKg,
                1000L
        );
    }

    private MultiFarmSettlementFoodCycle.Inputs inputs(
            long start,
            long end,
            List<MultiFarmSettlementFoodCycle.Farm> farms,
            List<SettlementFoodCycle.Household> households
    ) {
        return new MultiFarmSettlementFoodCycle.Inputs(
                SETTLEMENT,
                start,
                end,
                farms,
                households
        );
    }

    @Test
    void twoFarmsSupplyOneHouseholdOnce() {
        InventoryManager manager = manager();

        var first = farm(manager, 1L);
        var second = farm(manager, 2L);
        var home = household(manager, 1L);

        var result = MultiFarmSettlementFoodCycle.execute(
                manager,
                inputs(
                        29L, 30L,
                        List.of(
                                farmInput(1L, first, 60L, 0L),
                                farmInput(2L, second, 40L, 0L)
                        ),
                        List.of(householdInput(home, 80L))
                )
        );

        assertEquals(100L, result.harvestedKg());
        assertEquals(80L, result.distributedKg());
        assertEquals(80L, result.consumedKg());
        assertEquals(0L, result.shortageKg());

        assertEquals(0L, first.quantity(GRAIN));
        assertEquals(20L, second.quantity(GRAIN));
        assertEquals(0L, home.quantity(GRAIN));

        assertEquals(2, result.transfers().size());
        assertEquals(5, result.journalEntries());

        long consumptions = manager.transactions().stream()
                .filter(t ->
                        t.kind()
                                == EconomicTransaction.Kind.CONSUMPTION
                )
                .count();

        assertEquals(1L, consumptions);
    }

    @Test
    void twoHouseholdsShareScarceGrainProportionally() {
        InventoryManager manager = manager();

        var first = farm(manager, 1L);
        var second = farm(manager, 2L);

        var a = household(manager, 1L);
        var b = household(manager, 2L);

        var result = MultiFarmSettlementFoodCycle.execute(
                manager,
                inputs(
                        29L, 30L,
                        List.of(
                                farmInput(1L, first, 40L, 0L),
                                farmInput(2L, second, 20L, 0L)
                        ),
                        List.of(
                                householdInput(a, 50L),
                                householdInput(b, 50L)
                        )
                )
        );

        assertEquals(60L, result.distributedKg());
        assertEquals(60L, result.consumedKg());
        assertEquals(40L, result.shortageKg());

        assertEquals(
                30L,
                result.households().get(0).allocatedKg()
        );

        assertEquals(
                30L,
                result.households().get(1).allocatedKg()
        );

        assertEquals(3, result.transfers().size());

        assertEquals(
                first.id(),
                result.transfers().get(0).farm()
        );

        assertEquals(
                first.id(),
                result.transfers().get(1).farm()
        );

        assertEquals(
                second.id(),
                result.transfers().get(2).farm()
        );
    }

    @Test
    void protectedFarmReservesCannotBeDistributed() {
        InventoryManager manager = manager();

        var first = farm(manager, 1L);
        var second = farm(manager, 2L);
        var home = household(manager, 1L);

        manager.produce(
                first.id(), GRAIN, 100L, "initial:first"
        );

        manager.produce(
                second.id(), GRAIN, 10L, "initial:second"
        );

        var result = MultiFarmSettlementFoodCycle.execute(
                manager,
                inputs(
                        30L, 31L,
                        List.of(
                                farmInput(1L, first, 0L, 50L),
                                farmInput(2L, second, 0L, 0L)
                        ),
                        List.of(householdInput(home, 100L))
                )
        );

        assertEquals(0L, result.harvestedKg());
        assertEquals(60L, result.distributedKg());
        assertEquals(40L, result.shortageKg());
        assertEquals(50L, first.quantity(GRAIN));
        assertEquals(0L, second.quantity(GRAIN));
    }

    @Test
    void existingHouseholdFoodReducesNewAllocation() {
        InventoryManager manager = manager();

        var first = farm(manager, 1L);
        var second = farm(manager, 2L);
        var home = household(manager, 1L);

        manager.produce(
                home.id(), GRAIN, 15L, "existing_food"
        );

        var result = MultiFarmSettlementFoodCycle.execute(
                manager,
                inputs(
                        29L, 30L,
                        List.of(
                                farmInput(1L, first, 60L, 0L),
                                farmInput(2L, second, 0L, 0L)
                        ),
                        List.of(householdInput(home, 50L))
                )
        );

        assertEquals(35L, result.distributedKg());
        assertEquals(50L, result.consumedKg());
        assertEquals(0L, result.shortageKg());

        assertEquals(25L, first.quantity(GRAIN));
        assertEquals(0L, home.quantity(GRAIN));
    }

    @Test
    void noHarvestOccursBetweenHarvestBoundaries() {
        InventoryManager manager = manager();

        var first = farm(manager, 1L);
        var second = farm(manager, 2L);
        var home = household(manager, 1L);

        var result = MultiFarmSettlementFoodCycle.execute(
                manager,
                inputs(
                        30L, 31L,
                        List.of(
                                farmInput(1L, first, 100L, 0L),
                                farmInput(2L, second, 200L, 0L)
                        ),
                        List.of(householdInput(home, 10L))
                )
        );

        assertEquals(0L, result.harvestedKg());
        assertEquals(0L, result.consumedKg());
        assertEquals(10L, result.shortageKg());
        assertEquals(0, result.journalEntries());
    }

    @Test
    void planningDoesNotMutateInventories() {
        InventoryManager manager = manager();

        var first = farm(manager, 1L);
        var second = farm(manager, 2L);
        var home = household(manager, 1L);

        var result = MultiFarmSettlementFoodCycle.plan(
                manager,
                inputs(
                        29L, 30L,
                        List.of(
                                farmInput(1L, first, 60L, 0L),
                                farmInput(2L, second, 40L, 0L)
                        ),
                        List.of(householdInput(home, 80L))
                )
        );

        assertEquals(100L, result.harvestedKg());
        assertEquals(80L, result.distributedKg());

        assertEquals(0L, first.quantity(GRAIN));
        assertEquals(0L, second.quantity(GRAIN));
        assertEquals(0L, home.quantity(GRAIN));
        assertEquals(0L, manager.transactionCount());
    }

    @Test
    void insufficientJournalCapacityRejectsWholeCycle() {
        InventoryManager manager = new InventoryManager(
                ResourceCatalog.benchmarkDefaults(),
                2
        );

        var first = farm(manager, 1L);
        var second = farm(manager, 2L);
        var home = household(manager, 1L);

        assertThrows(
                IllegalStateException.class,
                () -> MultiFarmSettlementFoodCycle.execute(
                        manager,
                        inputs(
                                29L, 30L,
                                List.of(
                                        farmInput(1L, first, 60L, 0L),
                                        farmInput(2L, second, 40L, 0L)
                                ),
                                List.of(householdInput(home, 80L))
                        )
                )
        );

        assertEquals(0L, manager.transactionCount());
        assertEquals(0L, first.quantity(GRAIN));
        assertEquals(0L, second.quantity(GRAIN));
        assertEquals(0L, home.quantity(GRAIN));
    }

    @Test
    void farmOverflowRejectsBeforeAnyMutation() {
        InventoryManager manager = manager();

        var first = farm(manager, 1L);
        var second = farm(manager, 2L);
        var home = household(manager, 1L);

        manager.produce(
                second.id(),
                GRAIN,
                Long.MAX_VALUE,
                "initial_full_stock"
        );

        long previousCount = manager.transactionCount();

        assertThrows(
                ArithmeticException.class,
                () -> MultiFarmSettlementFoodCycle.execute(
                        manager,
                        inputs(
                                29L, 30L,
                                List.of(
                                        farmInput(1L, first, 10L, 0L),
                                        farmInput(2L, second, 1L, 0L)
                                ),
                                List.of(householdInput(home, 10L))
                        )
                )
        );

        assertEquals(0L, first.quantity(GRAIN));
        assertEquals(Long.MAX_VALUE,
                second.quantity(GRAIN));
        assertEquals(previousCount,
                manager.transactionCount());
    }

    @Test
    void reversedInputOrderProducesIdenticalTransactions() {
        InventoryManager firstManager = manager();
        InventoryManager secondManager = manager();

        var firstA = farm(firstManager, 1L);
        var firstB = farm(firstManager, 2L);
        var firstHome = household(firstManager, 1L);

        var secondA = farm(secondManager, 1L);
        var secondB = farm(secondManager, 2L);
        var secondHome = household(secondManager, 1L);

        var firstResult = MultiFarmSettlementFoodCycle.execute(
                firstManager,
                inputs(
                        29L, 30L,
                        List.of(
                                farmInput(1L, firstA, 60L, 0L),
                                farmInput(2L, firstB, 40L, 0L)
                        ),
                        List.of(householdInput(firstHome, 80L))
                )
        );

        var secondResult = MultiFarmSettlementFoodCycle.execute(
                secondManager,
                inputs(
                        29L, 30L,
                        List.of(
                                farmInput(2L, secondB, 40L, 0L),
                                farmInput(1L, secondA, 60L, 0L)
                        ),
                        List.of(householdInput(secondHome, 80L))
                )
        );

        assertEquals(firstResult, secondResult);
        assertEquals(
                firstManager.transactions(),
                secondManager.transactions()
        );
    }

    @Test
    void duplicateHoldingOrHouseholdIsRejected() {
        InventoryManager manager = manager();

        var first = farm(manager, 1L);
        var second = farm(manager, 2L);
        var home = household(manager, 1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> inputs(
                        29L, 30L,
                        List.of(
                                farmInput(1L, first, 10L, 0L),
                                farmInput(1L, second, 10L, 0L)
                        ),
                        List.of(householdInput(home, 10L))
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> inputs(
                        29L, 30L,
                        List.of(
                                farmInput(1L, first, 10L, 0L),
                                farmInput(2L, second, 10L, 0L)
                        ),
                        List.of(
                                householdInput(home, 10L),
                                householdInput(home, 10L)
                        )
                )
        );
    }

    @Test
    void sameSettlementLocationIsMandatory() {
        InventoryManager manager = manager();

        var first = farm(manager, 1L);
        var second = farm(manager, 2L);

        var distant = manager.create(
                EconomicOwner.npc(new NpcId(3L)),
                new SettlementId(2L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> MultiFarmSettlementFoodCycle.execute(
                        manager,
                        inputs(
                                29L, 30L,
                                List.of(
                                        farmInput(1L, first, 10L, 0L),
                                        farmInput(2L, second, 10L, 0L)
                                ),
                                List.of(
                                        householdInput(distant, 10L)
                                )
                        )
                )
        );

        assertEquals(0L, manager.transactionCount());
    }
}
