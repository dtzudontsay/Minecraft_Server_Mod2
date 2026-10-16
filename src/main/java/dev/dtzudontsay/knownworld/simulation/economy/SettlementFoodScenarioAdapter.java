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
 * Read-only bridge between authoritative NPC/social state
 * and the existing agricultural food cycle.
 *
 * Batch 22.21:
 * An explicitly enabled cohort integration may operate
 * without named NPC households.
 *
 * This does not create, infer, or count anonymous cohorts.
 * SettlementCohortFoodIntegration remains responsible for
 * validating and including those inhabitants.
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
        this.affiliations = Objects.requireNonNull(affiliations);
        this.organizations = Objects.requireNonNull(organizations);
        this.settlements = Objects.requireNonNull(settlements);
        this.holdingExists = Objects.requireNonNull(holdingExists);
    }

    /**
     * Original API.
     *
     * Preserves the requirement that at least one
     * represented NPC household exists.
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
        return resolve(
                inventories,
                farmHolding,
                farmInventory,
                settlement,
                interval,
                harvestInputs,
                protectedReserveKg,
                gramsPerPersonPerDay,
                false
        );
    }

    /**
     * Batch 22.21:
     *
     * If allowEmptyNamedHouseholds is true, an empty
     * represented household list is permitted.
     *
     * This is intended only for callers that separately
     * verify the anonymous population census and its
     * settlement-owned pantry.
     *
     * All existing farm identity, residence, household
     * ownership, and inventory checks still apply.
     */
    public Resolved resolve(
            InventoryManager inventories,
            HoldingId farmHolding,
            InventoryId farmInventory,
            SettlementId settlement,
            AgriculturalInterval interval,
            AgriculturalYieldModel.Inputs harvestInputs,
            long protectedReserveKg,
            long gramsPerPersonPerDay,
            boolean allowEmptyNamedHouseholds
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
                    EconomicOwner.holding(farmHolding))) {
                throw new IllegalStateException(
                        "Farm inventory owner does not match "
                                + "the selected holding"
                );
            }

            if (!farm.location().equals(settlement)) {
                throw new IllegalStateException(
                        "Farm inventory belongs to another settlement"
                );
            }

            /*
             * Named inhabitants remain authoritative
             * in NpcRegistry and NpcAffiliationManager.
             *
             * Nothing about population cohorts is
             * inferred from political or geography data.
             */
            Map<Long, Long> populations = new TreeMap<>();

            long unassigned = 0L;

            for (NpcAffiliation affiliation :
                    affiliations.all()) {

                if (!settlement.equals(
                        affiliation.residenceSettlement())) {
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
                            "Household seat and NPC residence disagree"
                    );
                }

                populations.merge(
                        householdId.value(),
                        1L,
                        Math::addExact
                );
            }

            /*
             * Existing behavior remains the default.
             *
             * Only the cohort-enabled runner may ask
             * for a zero-named-resident operation.
             */
            if (populations.isEmpty()
                    && !allowEmptyNamedHouseholds) {
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
             * Named households each require exactly
             * one pre-existing organization-owned
             * inventory in the selected settlement.
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
                                "Multiple inventories for household "
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