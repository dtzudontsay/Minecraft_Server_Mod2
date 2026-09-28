package dev.dtzudontsay.knownworld.simulation.social;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class NpcAffiliationManager {

    private final NpcRegistry npcs;

    private final SettlementManager settlements;

    private final OrganizationManager organizations;

    private final Map<NpcId, NpcAffiliation> affiliations =
            new LinkedHashMap<>();

    public NpcAffiliationManager(
            NpcRegistry npcs,
            SettlementManager settlements,
            OrganizationManager organizations
    ) {
        this.npcs =
                Objects.requireNonNull(
                        npcs,
                        "npcs"
                );

        this.settlements =
                Objects.requireNonNull(
                        settlements,
                        "settlements"
                );

        this.organizations =
                Objects.requireNonNull(
                        organizations,
                        "organizations"
                );
    }

    public synchronized NpcAffiliation getOrCreate(
            NpcId npc
    ) {
        validateNpc(
                npc
        );

        return affiliations.computeIfAbsent(
                npc,
                NpcAffiliation::new
        );
    }

    public synchronized void registerLoaded(
            NpcAffiliation affiliation
    ) {
        Objects.requireNonNull(
                affiliation,
                "affiliation"
        );

        validateNpc(
                affiliation.owner()
        );

        validateSettlementIfPresent(
                affiliation.residenceSettlement()
        );

        validateOrganizationType(
                affiliation.household(),
                OrganizationType.HOUSEHOLD
        );

        validateOrganizationType(
                affiliation.nobleHouse(),
                OrganizationType.NOBLE_HOUSE
        );

        validateOrganizationType(
                affiliation.faction(),
                OrganizationType.FACTION
        );

        affiliations.put(
                affiliation.owner(),
                affiliation
        );
    }

    public synchronized void setResidence(
            NpcId npc,
            SettlementId settlement
    ) {
        validateSettlementIfPresent(
                settlement
        );

        getOrCreate(
                npc
        )
                .setResidenceSettlement(
                        settlement
                );
    }

    public synchronized void setHousehold(
            NpcId npc,
            OrganizationId organization
    ) {
        validateOrganizationType(
                organization,
                OrganizationType.HOUSEHOLD
        );

        getOrCreate(
                npc
        )
                .setHousehold(
                        organization
                );
    }

    public synchronized void setNobleHouse(
            NpcId npc,
            OrganizationId organization
    ) {
        validateOrganizationType(
                organization,
                OrganizationType.NOBLE_HOUSE
        );

        getOrCreate(
                npc
        )
                .setNobleHouse(
                        organization
                );
    }

    public synchronized void setFaction(
            NpcId npc,
            OrganizationId organization
    ) {
        validateOrganizationType(
                organization,
                OrganizationType.FACTION
        );

        getOrCreate(
                npc
        )
                .setFaction(
                        organization
                );
    }

    public synchronized List<NpcAffiliation> all() {
        return List.copyOf(
                affiliations.values()
        );
    }

    public synchronized int size() {
        return affiliations.size();
    }

    private void validateNpc(
            NpcId id
    ) {
        if (!npcs.contains(
                id
        )) {
            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + id
            );
        }
    }

    private void validateSettlementIfPresent(
            SettlementId id
    ) {
        if (id != null
                && settlements.find(
                id
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown settlement ID: "
                            + id
            );
        }
    }

    private void validateOrganizationType(
            OrganizationId id,
            OrganizationType requiredType
    ) {
        if (id == null) {
            return;
        }

        Organization organization =
                organizations.find(
                                id
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown organization ID: "
                                                        + id
                                        )
                        );

        if (organization.type()
                != requiredType) {

            throw new IllegalArgumentException(
                    "Organization #"
                            + id
                            + " is "
                            + organization.type()
                            + ", expected "
                            + requiredType
            );
        }
    }
}