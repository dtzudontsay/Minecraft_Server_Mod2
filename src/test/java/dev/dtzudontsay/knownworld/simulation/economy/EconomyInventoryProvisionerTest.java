
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public final class EconomyInventoryProvisionerTest {

    @TempDir
    Path temporaryDirectory;

    private static final SettlementId WINTERFELL =
            new SettlementId(10L);

    private static final ResourceId GRAIN =
            new ResourceId("grain");

    private InventoryManager manager() {
        return new InventoryManager(
                ResourceCatalog.benchmarkDefaults()
        );
    }

    private EconomyReferenceValidator validator() {
        return new EconomyReferenceValidator(
                id -> id.value() == 1L,
                id -> id.value() == 10L,
                id -> false,
                id -> id.value() == 30L
        );
    }

    @Test
    void createsEmptyInventoryForValidNpc() {
        InventoryManager manager = manager();

        EconomyInventoryProvisioner provisioner =
                new EconomyInventoryProvisioner(
                        manager, validator()
                );

        ResourceInventory inventory =
                provisioner.ensureEmptyInventory(
                        EconomicOwner.npc(new NpcId(1L)),
                        WINTERFELL
                );

        assertEquals(1, manager.all().size());
        assertEquals(0L, inventory.quantity(GRAIN));
        assertTrue(inventory.balances().isEmpty());
    }

    @Test
    void repeatedProvisioningUsesSameInventory() {
        InventoryManager manager = manager();

        EconomyInventoryProvisioner provisioner =
                new EconomyInventoryProvisioner(
                        manager, validator()
                );

        EconomicOwner owner =
                EconomicOwner.npc(new NpcId(1L));

        ResourceInventory first =
                provisioner.ensureEmptyInventory(
                        owner, WINTERFELL
                );

        ResourceInventory second =
                provisioner.ensureEmptyInventory(
                        owner, WINTERFELL
                );

        assertEquals(first.id(), second.id());
        assertEquals(1, manager.all().size());
        assertEquals(2L, manager.nextId());
    }

    @Test
    void rejectsInvalidOwnerBeforeAllocatingId() {
        InventoryManager manager = manager();

        EconomyInventoryProvisioner provisioner =
                new EconomyInventoryProvisioner(
                        manager, validator()
                );

        assertThrows(
                IllegalStateException.class,
                () -> provisioner.ensureEmptyInventory(
                        EconomicOwner.npc(new NpcId(99L)),
                        WINTERFELL
                )
        );

        assertTrue(manager.all().isEmpty());
        assertEquals(1L, manager.nextId());
    }

    @Test
    void rejectsInvalidLocationBeforeAllocatingId() {
        InventoryManager manager = manager();

        EconomyInventoryProvisioner provisioner =
                new EconomyInventoryProvisioner(
                        manager, validator()
                );

        assertThrows(
                IllegalStateException.class,
                () -> provisioner.ensureEmptyInventory(
                        EconomicOwner.npc(new NpcId(1L)),
                        new SettlementId(999L)
                )
        );

        assertTrue(manager.all().isEmpty());
        assertEquals(1L, manager.nextId());
    }

    @Test
    void acceptsRealHoldingOwner() {
        InventoryManager manager = manager();

        EconomyInventoryProvisioner provisioner =
                new EconomyInventoryProvisioner(
                        manager, validator()
                );

        ResourceInventory inventory =
                provisioner.ensureEmptyInventory(
                        EconomicOwner.holding(
                                new HoldingId(30L)
                        ),
                        WINTERFELL
                );

        assertEquals(
                EconomicOwner.Kind.HOLDING,
                inventory.owner().kind()
        );

        assertEquals(30L, inventory.owner().id());
        assertTrue(inventory.balances().isEmpty());
    }

    @Test
    void rejectsUnknownHolding() {
        InventoryManager manager = manager();

        EconomyInventoryProvisioner provisioner =
                new EconomyInventoryProvisioner(
                        manager, validator()
                );

        assertThrows(
                IllegalStateException.class,
                () -> provisioner.ensureEmptyInventory(
                        EconomicOwner.holding(
                                new HoldingId(31L)
                        ),
                        WINTERFELL
                )
        );

        assertTrue(manager.all().isEmpty());
    }

    @Test
    void differentOwnersHaveDifferentInventories() {
        InventoryManager manager = manager();

        EconomyInventoryProvisioner provisioner =
                new EconomyInventoryProvisioner(
                        manager, validator()
                );

        ResourceInventory npcInventory =
                provisioner.ensureEmptyInventory(
                        EconomicOwner.npc(new NpcId(1L)),
                        WINTERFELL
                );

        ResourceInventory holdingInventory =
                provisioner.ensureEmptyInventory(
                        EconomicOwner.holding(
                                new HoldingId(30L)
                        ),
                        WINTERFELL
                );

        assertNotEquals(
                npcInventory.id(),
                holdingInventory.id()
        );

        assertEquals(2, manager.all().size());
    }

    @Test
    void emptyProvisionedInventorySurvivesReload()
            throws IOException {

        ResourceCatalog catalog =
                ResourceCatalog.benchmarkDefaults();

        InventoryManager original =
                new InventoryManager(catalog);

        EconomyInventoryProvisioner provisioner =
                new EconomyInventoryProvisioner(
                        original, validator()
                );

        ResourceInventory created =
                provisioner.ensureEmptyInventory(
                        EconomicOwner.holding(
                                new HoldingId(30L)
                        ),
                        WINTERFELL
                );

        InventoryPersistence persistence =
                new InventoryPersistence(
                        temporaryDirectory
                );

        persistence.save(original);

        InventoryManager loaded =
                persistence.load(catalog);

        ResourceInventory restored =
                loaded.require(created.id());

        assertEquals(
                created.owner(),
                restored.owner()
        );

        assertEquals(
                WINTERFELL,
                restored.location()
        );

        assertTrue(restored.balances().isEmpty());

        assertDoesNotThrow(
                () -> validator().validate(loaded)
        );
    }

    @Test
    void reProvisionAfterReloadDoesNotDuplicate()
            throws IOException {

        ResourceCatalog catalog =
                ResourceCatalog.benchmarkDefaults();

        InventoryManager original =
                new InventoryManager(catalog);

        EconomicOwner owner =
                EconomicOwner.holding(
                        new HoldingId(30L)
                );

        new EconomyInventoryProvisioner(
                original, validator()
        ).ensureEmptyInventory(
                owner, WINTERFELL
        );

        InventoryPersistence persistence =
                new InventoryPersistence(
                        temporaryDirectory
                );

        persistence.save(original);

        InventoryManager loaded =
                persistence.load(catalog);

        ResourceInventory existing =
                loaded.all().iterator().next();

        ResourceInventory again =
                new EconomyInventoryProvisioner(
                        loaded, validator()
                ).ensureEmptyInventory(
                        owner, WINTERFELL
                );

        assertEquals(existing.id(), again.id());
        assertEquals(1, loaded.all().size());
    }
}
