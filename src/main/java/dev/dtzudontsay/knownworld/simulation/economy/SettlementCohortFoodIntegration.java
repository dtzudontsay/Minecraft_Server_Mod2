
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.population.PopulationCohortManager;
import dev.dtzudontsay.knownworld.simulation.population.SettlementPopulationCensus;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Batch 22.20.
 *
 * Adds anonymous settlement population to the EXISTING
 * multi-farm food cycle, alongside real NPC households.
 *
 * PopulationCohortManager owns anonymous population.
 * NpcRegistry and NpcAffiliationManager own individuals.
 * InventoryManager owns actual grain.
 *
 * No parallel resource ledger or food allocation
 * algorithm is introduced.
 *
 * The aggregate pantry is explicitly represented by
 * an existing settlement-owned inventory.
 *
 * A single food cycle calculates the demand and
 * distributes the same real grain across all consumers.
 */
public final class SettlementCohortFoodIntegration {

    public record HoldingDemand(
            HoldingId holding,
            long anonymousResidents,
            long days,
            long requiredKg
    ) {
    }

    private final PopulationCohortManager cohorts;
    private final SettlementPopulationCensus census;

    private final InventoryId cohortPantry;
    private final long gramsPerPersonPerDay;

    /**
     * A pre-existing, settlement-owned inventory
     * must be supplied for anonymous consumption.
     *
     * The pantry may be null only when the requested
     * settlement currently has zero anonymous people.
     *
     * This class never creates an inventory.
     */
    public SettlementCohortFoodIntegration(
            PopulationCohortManager cohorts,
            SettlementPopulationCensus census,
            InventoryId cohortPantry,
            long gramsPerPersonPerDay
    ) {
        this.cohorts = Objects.requireNonNull(
                cohorts, "cohorts"
        );

        this.census = Objects.requireNonNull(
                census, "census"
        );

        if (gramsPerPersonPerDay <= 0L) {
            throw new IllegalArgumentException(
                    "Anonymous food requirement must be positive"
            );
        }

        this.cohortPantry = cohortPantry;
        this.gramsPerPersonPerDay =
                gramsPerPersonPerDay;
    }

    public InventoryId cohortPantry() {
        return cohortPantry;
    }

    public long gramsPerPersonPerDay() {
        return gramsPerPersonPerDay;
    }

    /**
     * Append ONE aggregate anonymous consumer to the
     * already-resolved NPC household list.
     *
     * Preconditions:
     *
     * - Cohorts have an explicitly surveyed settlement.
     * - All named residents are represented by the
     *   existing household list exactly once.
     * - No named resident lacks a household.
     * - The cohort pantry is owned by the settlement.
     * - No farm or named household shares that pantry.
     *
     * The returned list is suitable for
     * MultiFarmSettlementFoodCycle.Inputs.
     *
     * Read-only: no inventory or population mutation.
     */
    public List<SettlementFoodCycle.Household>
    appendHouseholds(
            InventoryManager inventories,
            SettlementId settlement,
            List<SettlementFoodCycle.Household>
                    representedHouseholds
    ) {
        Objects.requireNonNull(
                inventories, "inventories"
        );
        Objects.requireNonNull(
                settlement, "settlement"
        );
        Objects.requireNonNull(
                representedHouseholds,
                "representedHouseholds"
        );

        synchronized (cohorts) {

            SettlementPopulationCensus.Result population =
                    census.forSettlement(settlement);

            if (!population.hasPopulationCoverage()) {
                throw new IllegalStateException(
                        "Settlement has no surveyed "
                                + "anonymous population: "
                                + settlement
                );
            }

            if (population.residentsWithoutHousehold()
                    != 0L) {
                throw new IllegalStateException(
                        "Named settlement residents have "
                                + "no household: "
                                + settlement
                );
            }

            /*
             * Validate the actual population manager,
             * not only a cached census result.
             */
            long anonymous = 0L;

            for (PopulationCohortManager.Cohort cohort :
                    cohorts.snapshot().cohorts()) {

                if (cohort.key().settlement()
                        .equals(settlement)) {

                    anonymous = Math.addExact(
                            anonymous,
                            cohort.people()
                    );
                }
            }

            if (anonymous != population.anonymous()) {
                throw new IllegalStateException(
                        "Anonymous cohort and census "
                                + "population disagree"
                );
            }

            long represented = 0L;
            Set<InventoryId> used = new HashSet<>();

            for (SettlementFoodCycle.Household household :
                    representedHouseholds) {

                if (!used.add(household.inventory())) {
                    throw new IllegalArgumentException(
                            "Duplicate represented household "
                                    + "inventory"
                    );
                }

                ResourceInventory inventory =
                        inventories.require(
                                household.inventory()
                        );

                if (!inventory.location()
                        .equals(settlement)) {

                    throw new IllegalStateException(
                            "Household inventory belongs to "
                                    + "another settlement"
                    );
                }

                if (inventory.owner().kind()
                        != EconomicOwner.Kind.ORGANIZATION) {

                    throw new IllegalStateException(
                            "Represented household inventory "
                                    + "must belong to an organization"
                    );
                }

                represented = Math.addExact(
                        represented,
                        household.population()
                );
            }

            /*
             * Ensures that the named-NPC demand resolved
             * by SettlementFoodScenarioAdapter is exactly
             * the population counted by the census.
             *
             * Cohort inhabitants are never added to this
             * represented-person total.
             */
            if (represented != population.represented()) {
                throw new IllegalStateException(
                        "Represented household population "
                                + "does not match NPC census: "
                                + represented
                                + " vs "
                                + population.represented()
                );
            }

            if (represented != 0L
                    && representedHouseholds.isEmpty()) {

                throw new IllegalStateException(
                        "Named residents require existing "
                                + "household inventories"
                );
            }

            List<SettlementFoodCycle.Household> combined =
                    new ArrayList<>(
                            representedHouseholds
                    );

            if (anonymous == 0L) {
                return List.copyOf(combined);
            }

            if (cohortPantry == null) {
                throw new IllegalStateException(
                        "Anonymous residents require an "
                                + "existing aggregate pantry"
                );
            }

            if (!used.add(cohortPantry)) {
                throw new IllegalArgumentException(
                        "Aggregate pantry collides with "
                                + "a named household inventory"
                );
            }

            ResourceInventory pantry =
                    inventories.require(cohortPantry);

            if (!pantry.location().equals(settlement)) {
                throw new IllegalStateException(
                        "Aggregate pantry belongs to "
                                + "a different settlement"
                );
            }

            if (!pantry.owner().equals(
                    EconomicOwner.settlement(settlement)
            )) {
                throw new IllegalStateException(
                        "Aggregate pantry must have "
                                + "the existing settlement "
                                + "as economic owner"
                );
            }

            /*
             * Anonymous residents are one aggregate
             * consumer for this settlement.
             *
             * The existing cycle handles demand,
             * harvest, proportional distribution,
             * shortages and real consumption.
             */
            combined.add(
                    new SettlementFoodCycle.Household(
                            cohortPantry,
                            anonymous,
                            gramsPerPersonPerDay
                    )
            );

            return List.copyOf(combined);
        }
    }

    /**
     * Read-only food requirement for anonymous people
     * assigned to an unplaced landed holding.
     *
     * InventoryManager currently requires a
     * SettlementId location. Therefore this method
     * does not invent a settlement, pantry or grain
     * balance for a holding without one.
     *
     * It calculates required grain only.
     */
    public static HoldingDemand projectHoldingDemand(
            PopulationCohortManager cohorts,
            HoldingId holding,
            long days,
            long gramsPerPersonPerDay
    ) {
        Objects.requireNonNull(cohorts, "cohorts");
        Objects.requireNonNull(holding, "holding");

        if (days < 0L
                || gramsPerPersonPerDay < 0L) {
            throw new IllegalArgumentException(
                    "Negative holding food-demand input"
            );
        }

        synchronized (cohorts) {

            if (!cohorts.holdingSurveyed(holding)) {
                throw new IllegalStateException(
                        "Unsurveyed holding population: "
                                + holding
                );
            }

            long population = 0L;

            for (PopulationCohortManager.HoldingCohort cohort :
                    cohorts.extendedSnapshot()
                            .holdingCohorts()) {

                if (cohort.key().holding()
                        .equals(holding)) {

                    population = Math.addExact(
                            population,
                            cohort.people()
                    );
                }
            }

            long required =
                    HouseholdFoodDemandModel.calculate(
                            new HouseholdFoodDemandModel.Inputs(
                                    population,
                                    days,
                                    gramsPerPersonPerDay
                            )
                    ).requiredKg();

            return new HoldingDemand(
                    holding,
                    population,
                    days,
                    required
            );
        }
    }
}
