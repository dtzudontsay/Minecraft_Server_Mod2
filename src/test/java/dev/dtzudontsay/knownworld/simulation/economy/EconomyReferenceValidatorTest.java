
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class EconomyReferenceValidatorTest {

    private EconomyReferenceValidator validator() {
        return new EconomyReferenceValidator(
                id -> id.value() == 1L,
                id -> id.value() == 10L,
                id -> id.value() == 20L,
                id -> id.value() == 30L
        );
    }

    private ResourceInventory inventory(
            EconomicOwner owner,
            long settlementId
    ) {
        InventoryManager manager = new InventoryManager(
                ResourceCatalog.benchmarkDefaults()
        );

        return manager.create(
                owner,
                new SettlementId(settlementId)
        );
    }

    @Test
    void acceptsExistingNpcOwner() {
        assertDoesNotThrow(
                () -> validator().validate(
                        inventory(
                                EconomicOwner.npc(new NpcId(1L)),
                                10L
                        )
                )
        );
    }

    @Test
    void rejectsMissingNpcOwner() {
        assertThrows(
                IllegalStateException.class,
                () -> validator().validate(
                        inventory(
                                EconomicOwner.npc(new NpcId(2L)),
                                10L
                        )
                )
        );
    }

    @Test
    void rejectsMissingSettlementLocation() {
        assertThrows(
                IllegalStateException.class,
                () -> validator().validate(
                        inventory(
                                EconomicOwner.npc(new NpcId(1L)),
                                11L
                        )
                )
        );
    }

    @Test
    void acceptsExistingSettlementOwner() {
        assertDoesNotThrow(
                () -> validator().validate(
                        inventory(
                                EconomicOwner.settlement(
                                        new SettlementId(10L)
                                ),
                                10L
                        )
                )
        );
    }

    @Test
    void acceptsExistingOrganizationOwner() {
        assertDoesNotThrow(
                () -> validator().validate(
                        inventory(
                                EconomicOwner.organization(
                                        new OrganizationId(20L)
                                ),
                                10L
                        )
                )
        );
    }

    @Test
    void rejectsMissingOrganizationOwner() {
        assertThrows(
                IllegalStateException.class,
                () -> validator().validate(
                        inventory(
                                EconomicOwner.organization(
                                        new OrganizationId(21L)
                                ),
                                10L
                        )
                )
        );
    }

    @Test
    void acceptsHoldingWhenValidatorSupportsIt() {
        assertDoesNotThrow(
                () -> validator().validate(
                        inventory(
                                EconomicOwner.holding(
                                        new HoldingId(30L)
                                ),
                                10L
                        )
                )
        );
    }

    @Test
    void unsupportedHoldingFailsClosed() {
        EconomyReferenceValidator runtimeValidator =
                new EconomyReferenceValidator(
                        id -> true,
                        id -> true,
                        id -> true,
                        id -> false
                );

        assertThrows(
                IllegalStateException.class,
                () -> runtimeValidator.validate(
                        inventory(
                                EconomicOwner.holding(
                                        new HoldingId(30L)
                                ),
                                10L
                        )
                )
        );
    }

    @Test
    void validatesWholeManager() {
        InventoryManager manager = new InventoryManager(
                ResourceCatalog.benchmarkDefaults()
        );

        manager.create(
                EconomicOwner.npc(new NpcId(1L)),
                new SettlementId(10L)
        );

        manager.create(
                EconomicOwner.organization(
                        new OrganizationId(20L)
                ),
                new SettlementId(10L)
        );

        assertDoesNotThrow(
                () -> validator().validate(manager)
        );
    }

    @Test
    void rejectsInvalidMemberOfManager() {
        InventoryManager manager = new InventoryManager(
                ResourceCatalog.benchmarkDefaults()
        );

        manager.create(
                EconomicOwner.npc(new NpcId(1L)),
                new SettlementId(10L)
        );

        manager.create(
                EconomicOwner.npc(new NpcId(99L)),
                new SettlementId(10L)
        );

        assertThrows(
                IllegalStateException.class,
                () -> validator().validate(manager)
        );
    }
}
