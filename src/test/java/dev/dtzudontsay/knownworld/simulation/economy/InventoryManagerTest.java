
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class InventoryManagerTest {

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private static final ResourceId COIN =
            new ResourceId("coin");

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
    void productionIncreasesStock() {
        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 100L, "harvest"
        );

        assertEquals(100L, inventory.quantity(GRAIN));
        assertEquals(100L, manager.totalQuantity(GRAIN));
    }

    @Test
    void consumptionDecreasesStock() {
        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager, 1L);

        manager.produce(
                inventory.id(), GRAIN, 100L, "harvest"
        );

        manager.consume(
                inventory.id(), GRAIN, 35L, "food"
        );

        assertEquals(65L, inventory.quantity(GRAIN));
    }

    @Test
    void transferConservesResources() {
        InventoryManager manager = manager();
        ResourceInventory source = create(manager, 1L);
        ResourceInventory destination = create(manager, 2L);

        manager.produce(
                source.id(), GRAIN, 100L, "harvest"
        );

        manager.transfer(
                source.id(), destination.id(),
                GRAIN, 40L, "purchase"
        );

        assertEquals(60L, source.quantity(GRAIN));
        assertEquals(40L, destination.quantity(GRAIN));
        assertEquals(100L, manager.totalQuantity(GRAIN));
    }

    @Test
    void insufficientBalanceLeavesStateUnchanged() {
        InventoryManager manager = manager();
        ResourceInventory source = create(manager, 1L);
        ResourceInventory destination = create(manager, 2L);

        manager.produce(
                source.id(), GRAIN, 10L, "harvest"
        );

        assertThrows(
                IllegalStateException.class,
                () -> manager.transfer(
                        source.id(), destination.id(),
                        GRAIN, 20L, "purchase"
                )
        );

        assertEquals(10L, source.quantity(GRAIN));
        assertEquals(0L, destination.quantity(GRAIN));
    }

    @Test
    void overflowLeavesStateUnchanged() {
        InventoryManager manager = manager();
        ResourceInventory source = create(manager, 1L);
        ResourceInventory destination = create(manager, 2L);

        manager.produce(
                source.id(), COIN, 100L, "initial_funds"
        );

        manager.produce(
                destination.id(), COIN,
                Long.MAX_VALUE, "initial_funds"
        );

        assertThrows(
                ArithmeticException.class,
                () -> manager.transfer(
                        source.id(), destination.id(),
                        COIN, 1L, "payment"
                )
        );

        assertEquals(100L, source.quantity(COIN));
        assertEquals(
                Long.MAX_VALUE,
                destination.quantity(COIN)
        );
    }

    @Test
    void rejectsUnknownResources() {
        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager, 1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.produce(
                        inventory.id(),
                        new ResourceId("unknown_resource"),
                        1L,
                        "test"
                )
        );
    }

    @Test
    void rejectsInvalidOperations() {
        InventoryManager manager = manager();
        ResourceInventory inventory = create(manager, 1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.produce(
                        inventory.id(), GRAIN, -1L, "harvest"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.produce(
                        inventory.id(), GRAIN, 1L, ""
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.transfer(
                        inventory.id(), inventory.id(),
                        GRAIN, 1L, "self_transfer"
                )
        );
    }

    @Test
    void inventoryIdentityIsStable() {
        InventoryManager manager = manager();

        ResourceInventory first = create(manager, 1L);
        ResourceInventory second = create(manager, 2L);

        assertEquals(1L, first.id().value());
        assertEquals(2L, second.id().value());

        assertEquals(
                EconomicOwner.Kind.NPC,
                first.owner().kind()
        );
        assertEquals(1L, first.owner().id());
    }

    @Test
    void duplicateResourceDefinitionsAreRejected() {
        ResourceCatalog catalog =
                ResourceCatalog.benchmarkDefaults();

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.register(
                        new ResourceDefinition(
                                GRAIN, "Other grain", "kilogram"
                        )
                )
        );
    }
}
