package dev.dtzudontsay.knownworld.simulation.social;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.Objects;

/**
 * Persistent social placement for one NPC.
 *
 * Residence and organization memberships are separate concepts.
 */
public final class NpcAffiliation {

    private final NpcId owner;

    private SettlementId residenceSettlement;

    private OrganizationId household;

    private OrganizationId nobleHouse;

    private OrganizationId faction;

    public NpcAffiliation(
            NpcId owner
    ) {
        this(
                owner,
                null,
                null,
                null,
                null
        );
    }

    public NpcAffiliation(
            NpcId owner,
            SettlementId residenceSettlement,
            OrganizationId household,
            OrganizationId nobleHouse,
            OrganizationId faction
    ) {
        this.owner =
                Objects.requireNonNull(
                        owner,
                        "owner"
                );

        this.residenceSettlement =
                residenceSettlement;

        this.household =
                household;

        this.nobleHouse =
                nobleHouse;

        this.faction =
                faction;
    }

    public NpcId owner() {
        return owner;
    }

    public SettlementId residenceSettlement() {
        return residenceSettlement;
    }

    public void setResidenceSettlement(
            SettlementId settlement
    ) {
        this.residenceSettlement =
                settlement;
    }

    public OrganizationId household() {
        return household;
    }

    public void setHousehold(
            OrganizationId household
    ) {
        this.household =
                household;
    }

    public OrganizationId nobleHouse() {
        return nobleHouse;
    }

    public void setNobleHouse(
            OrganizationId nobleHouse
    ) {
        this.nobleHouse =
                nobleHouse;
    }

    public OrganizationId faction() {
        return faction;
    }

    public void setFaction(
            OrganizationId faction
    ) {
        this.faction =
                faction;
    }
}