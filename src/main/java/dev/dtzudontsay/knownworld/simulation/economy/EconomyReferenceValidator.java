
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Batch 21.2
 *
 * Validates that inventory owner and location
 * references exist in authoritative simulation state.
 *
 * Predicate-based integration allows validation
 * without duplicating authoritative registries.
 *
 * An unsupported owner type must fail closed.
 */
public final class EconomyReferenceValidator {

    private final Predicate<NpcId> npcExists;
    private final Predicate<SettlementId> settlementExists;
    private final Predicate<OrganizationId> organizationExists;
    private final Predicate<HoldingId> holdingExists;

    public EconomyReferenceValidator(
            Predicate<NpcId> npcExists,
            Predicate<SettlementId> settlementExists,
            Predicate<OrganizationId> organizationExists,
            Predicate<HoldingId> holdingExists
    ) {
        this.npcExists = Objects.requireNonNull(
                npcExists, "npcExists"
        );
        this.settlementExists = Objects.requireNonNull(
                settlementExists, "settlementExists"
        );
        this.organizationExists = Objects.requireNonNull(
                organizationExists, "organizationExists"
        );
        this.holdingExists = Objects.requireNonNull(
                holdingExists, "holdingExists"
        );
    }

    public void validate(InventoryManager manager) {
        Objects.requireNonNull(manager, "manager");

        for (ResourceInventory inventory : manager.all()) {
            validate(inventory);
        }
    }

    public void validate(ResourceInventory inventory) {
        Objects.requireNonNull(inventory, "inventory");

        SettlementId location = inventory.location();

        if (!settlementExists.test(location)) {
            throw new IllegalStateException(
                    "Inventory " + inventory.id()
                            + " references missing settlement "
                            + location
            );
        }

        EconomicOwner owner = inventory.owner();

        boolean exists = switch (owner.kind()) {
            case NPC -> npcExists.test(
                    new NpcId(owner.id())
            );

            case SETTLEMENT -> settlementExists.test(
                    new SettlementId(owner.id())
            );

            case ORGANIZATION -> organizationExists.test(
                    new OrganizationId(owner.id())
            );

            case HOLDING -> holdingExists.test(
                    new HoldingId(owner.id())
            );
        };

        if (!exists) {
            throw new IllegalStateException(
                    "Inventory " + inventory.id()
                            + " references missing or unsupported "
                            + owner.kind()
                            + " owner " + owner.id()
            );
        }
    }
}
