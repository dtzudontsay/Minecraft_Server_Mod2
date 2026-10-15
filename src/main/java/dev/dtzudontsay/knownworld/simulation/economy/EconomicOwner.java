
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.Objects;

/**
 * Typed reference to the owner of economic property.
 *
 * This does not create a second NPC, settlement,
 * organization or holding identity system.
 *
 * Future owner types can include households,
 * workplaces and governments.
 */
public record EconomicOwner(
        Kind kind,
        long id
) {

    public enum Kind {
        NPC,
        SETTLEMENT,
        HOLDING,
        ORGANIZATION
    }

    public EconomicOwner {
        Objects.requireNonNull(kind, "kind");

        if (id <= 0L) {
            throw new IllegalArgumentException(
                    "Economic owner ID must be positive"
            );
        }
    }

    public static EconomicOwner npc(NpcId id) {
        return new EconomicOwner(
                Kind.NPC,
                Objects.requireNonNull(id, "id").value()
        );
    }

    public static EconomicOwner settlement(SettlementId id) {
        return new EconomicOwner(
                Kind.SETTLEMENT,
                Objects.requireNonNull(id, "id").value()
        );
    }

    public static EconomicOwner holding(HoldingId id) {
        return new EconomicOwner(
                Kind.HOLDING,
                Objects.requireNonNull(id, "id").value()
        );
    }

    public static EconomicOwner organization(OrganizationId id) {
        return new EconomicOwner(
                Kind.ORGANIZATION,
                Objects.requireNonNull(id, "id").value()
        );
    }
}
