
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliation;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Batch 22.9.
 *
 * Read-only bridge between existing Known World
 * social identities and settlement agriculture.
 *
 * Household identity remains OrganizationId.
 * Residence remains NpcAffiliation state.
 * Farm ownership remains HoldingId.
 *
 * Only living, persistent NPCs are counted.
 *
 * Abstract population cohorts are NOT silently
 * inferred from population weights.
 */
public final class SettlementFoodScenarioAdapter {

    public record Resolved(
            SettlementFoodCycle.Inputs cycleInputs,
            long representedResidents,
            long unassignedResidents,
            List<OrganizationId> householdOrganizations
    ) {
        public Resolved {
            Objects.requireNonNull(cycleInputs);
            householdOrganizations =
                    List.copyOf(householdOrganizations);
        }
    }

    private final NpcRegistry npcs;
    private final NpcAffiliationManager affiliations;
    private final OrganizationManager organizations;
    private final SettlementManager settlements;
    private final Predicate<HoldingId> holdingExists;

    public SettlementFoodScenarioAdapter(
            NpcRegistry npcs,
            NpcAffiliationManager affiliations,
            OrganizationManager organizations,
            SettlementManager settlements,
            Predicate<HoldingId> holdingExists
    ) {
        this.npcs = Objects.requireNonNull(npcs);
        this.affiliations =
                Objects.requireNonNull(affiliations);
        this.organizations =
                Objects.requireNonNull(organizations);
        this.settlements =
                Objects.requireNonNull(settlements);
        this.holdingExists =
                Objects.requireNonNull(holdingExists);
    }

    /**
     * Resolve existing social state into food-cycle
     * inputs without modifying any manager.
     *
     * A holding-to-settlement association must be
     * supplied explicitly and represented by the
     * farm inventory's owner and location.
     *
     * This does NOT infer geography from
     * LandedHolding.populationWeight or taxBase.
     */
    public Resolved resolve(
            InventoryManager inventories,
            HoldingId farmHolding,
            InventoryId farmInventory,
            SettlementId settlement,
            AgriculturalInterval interval,
            AgriculturalYieldModel.Inputs harvestInputs,
            long protectedReserveKg,
            long gramsPerPersonPerDay
    ) {
        Objects.requireNonNull(inventories);
        Objects.requireNonNull(farmHolding);
        Objects.requireNonNull(farmInventory);
        Objects.requireNonNull(settlement);
        Objects.requireNonNull(interval);
        Objects.requireNonNull(harvestInputs);

        if (protectedReserveKg < 0L
                || gramsPerPersonPerDay < 0L) {
            throw new IllegalArgumentException(
                    "Negative agricultural input"
            );
        }

        synchronized (inventories) {

            if (settlements.find(settlement).isEmpty()) {
                throw new IllegalStateException(
                        "Unknown agricultural settlement: "
                                + settlement
                );
            }

            if (!holdingExists.test(farmHolding)) {
                throw new IllegalStateException(
                        "Unknown agricultural holding: "
                                + farmHolding
                );
            }

            ResourceInventory farm =
                    inventories.require(farmInventory);

            if (!farm.owner().equals(
                    EconomicOwner.holding(farmHolding)
            )) {
                throw new IllegalStateException(
                        "Farm inventory is not owned by "
                                + "the selected holding"
                );
            }

            if (!farm.location().equals(settlement)) {
                throw new IllegalStateException(
                        "Farm inventory belongs to "
                                + "a different settlement"
                );
            }

            /*
             * Count only existing living individuals
             * whose authoritative residence matches
             * the requested settlement.
             *
             * OrganizationId, not a new HouseholdId,
             * identifies the household.
             */
            Map<Long, Long> populations =
                    new TreeMap<>();

            long unassigned = 0L;

            for (NpcAffiliation affiliation :
                    affiliations.all()) {

                if (!settlement.equals(
                        affiliation.residenceSettlement()
                )) {
                    continue;
                }

                NpcState npc = npcs.find(
                        affiliation.owner()
                ).orElseThrow(
                        () -> new IllegalStateException(
                                "Missing affiliated NPC: "
                                        + affiliation.owner()
                        )
                );

                if (!npc.isAlive()) {
                    continue;
                }

                OrganizationId householdId =
                        affiliation.household();

                if (householdId == null) {
                    unassigned = Math.addExact(
                            unassigned, 1L
                    );
                    continue;
                }

                Organization organization =
                        organizations.find(householdId)
                                .orElseThrow(
                                        () -> new IllegalStateException(
                                                "Unknown household "
                                                        + householdId
                                        )
                                );

                if (organization.type()
                        != OrganizationType.HOUSEHOLD) {
                    throw new IllegalStateException(
                            "Household reference points to "
                                    + "a non-household organization"
                    );
                }

                if (organization.seatSettlement() != null
                        && !organization.seatSettlement()
                        .equals(settlement)) {
                    throw new IllegalStateException(
                            "Household seat and NPC "
                                    + "residence disagree"
                    );
                }

                populations.merge(
                        householdId.value(),
                        1L,
                        Math::addExact
                );
            }

            if (populations.isEmpty()) {
                throw new IllegalStateException(
                        "No living household population "
                                + "resolved for settlement"
                );
            }

            List<SettlementFoodCycle.Household> households =
                    new ArrayList<>();

            List<OrganizationId> organizationIds =
                    new ArrayList<>();

            long represented = 0L;

            /*
             * Each represented household requires
             * exactly one pre-existing inventory at
             * the selected settlement.
             *
             * No automatic provisioning occurs.
             */
            for (Map.Entry<Long, Long> entry :
                    populations.entrySet()) {

                OrganizationId organizationId =
                        new OrganizationId(entry.getKey());

                EconomicOwner owner =
                        EconomicOwner.organization(
                                organizationId
                        );

                InventoryId found = null;

                for (ResourceInventory inventory :
                        inventories.all()) {

                    if (!inventory.owner().equals(owner)
                            || !inventory.location()
                            .equals(settlement)) {
                        continue;
                    }

                    if (found != null) {
                        throw new IllegalStateException(
                                "Multiple inventories for "
                                        + "household "
                                        + organizationId
                        );
                    }

                    found = inventory.id();
                }

                if (found == null) {
                    throw new IllegalStateException(
                            "Household lacks an inventory: "
                                    + organizationId
                    );
                }

                represented = Math.addExact(
                        represented,
                        entry.getValue()
                );

                organizationIds.add(organizationId);

                households.add(
                        new SettlementFoodCycle.Household(
                                found,
                                entry.getValue(),
                                gramsPerPersonPerDay
                        )
                );
            }

            SettlementFoodCycle.Inputs cycleInputs =
                    new SettlementFoodCycle.Inputs(
                            farmInventory,
                            interval,
                            harvestInputs,
                            protectedReserveKg,
                            households
                    );

            return new Resolved(
                    cycleInputs,
                    represented,
                    unassigned,
                    organizationIds
            );
        }
    }
}
