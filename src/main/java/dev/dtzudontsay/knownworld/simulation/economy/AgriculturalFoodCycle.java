
package dev.dtzudontsay.knownworld.simulation.economy;

import java.util.Objects;

/**
 * Batch 22.6.
 *
 * Deterministic farm-to-household grain flow:
 *
 * 1. Agricultural production
 * 2. Distribution to a household
 * 3. Household consumption
 * 4. Shortage calculation
 *
 * All quantities use the ResourceCatalog's grain
 * unit, currently kilograms.
 *
 * The caller determines harvest output, planned
 * distribution and household food requirement.
 *
 * This class has no calendar or automatic tick hooks.
 */
public final class AgriculturalFoodCycle {

    public static final ResourceId GRAIN =
            new ResourceId("grain");

    public record Plan(
            InventoryId farm,
            InventoryId household,
            long harvestKg,
            long distributionKg,
            long foodRequirementKg
    ) {
        public Plan {
            Objects.requireNonNull(farm, "farm");
            Objects.requireNonNull(household, "household");

            if (farm.equals(household)) {
                throw new IllegalArgumentException(
                        "Farm and household must use "
                                + "different inventories"
                );
            }

            if (harvestKg < 0L
                    || distributionKg < 0L
                    || foodRequirementKg < 0L) {
                throw new IllegalArgumentException(
                        "Agricultural quantities cannot "
                                + "be negative"
                );
            }
        }
    }

    public record Result(
            long harvestedKg,
            long distributedKg,
            long consumedKg,
            long shortageKg,
            long farmStockKg,
            long householdStockKg,
            int recordedTransactions
    ) {
        public Result {
            if (harvestedKg < 0L
                    || distributedKg < 0L
                    || consumedKg < 0L
                    || shortageKg < 0L
                    || farmStockKg < 0L
                    || householdStockKg < 0L
                    || recordedTransactions < 0) {
                throw new IllegalArgumentException(
                        "Invalid agricultural cycle result"
                );
            }
        }

        public boolean foodRequirementMet() {
            return shortageKg == 0L;
        }
    }

    private AgriculturalFoodCycle() {
    }

    /**
     * Execute one complete agricultural cycle.
     *
     * The manager lock is held throughout planning
     * and mutation so no competing economic operation
     * can invalidate the calculated balances.
     *
     * All expected arithmetic, stock and journal
     * capacity errors are checked before mutation.
     *
     * This is not a cross-domain world transaction.
     */
    public static Result execute(
            InventoryManager manager,
            Plan plan
    ) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(plan, "plan");

        synchronized (manager) {

            ResourceInventory farm =
                    manager.require(plan.farm());

            ResourceInventory household =
                    manager.require(plan.household());

            /*
             * The first vertical slice only moves
             * food inside one settlement.
             *
             * Inter-settlement trade will be a
             * separate economic operation later.
             */
            if (!farm.location().equals(
                    household.location()
            )) {
                throw new IllegalArgumentException(
                        "Farm and household must be "
                                + "located in the same settlement"
                );
            }

            long farmBefore = farm.quantity(GRAIN);
            long householdBefore =
                    household.quantity(GRAIN);

            /*
             * Arithmetic is verified before any
             * balances are modified.
             */
            long availableFarm = Math.addExact(
                    farmBefore,
                    plan.harvestKg()
            );

            long distributed = Math.min(
                    availableFarm,
                    plan.distributionKg()
            );

            long remainingFarm =
                    availableFarm - distributed;

            long householdAfterDistribution =
                    Math.addExact(
                            householdBefore,
                            distributed
                    );

            long consumed = Math.min(
                    householdAfterDistribution,
                    plan.foodRequirementKg()
            );

            long shortage =
                    plan.foodRequirementKg() - consumed;

            long remainingHousehold =
                    householdAfterDistribution - consumed;

            /*
             * Zero-quantity operations do not generate
             * synthetic accounting transactions.
             */
            int requiredTransactions = 0;

            if (plan.harvestKg() > 0L) {
                requiredTransactions++;
            }

            if (distributed > 0L) {
                requiredTransactions++;
            }

            if (consumed > 0L) {
                requiredTransactions++;
            }

            /*
             * Preflight the entire cycle to avoid
             * a journal-capacity failure halfway
             * through its three operations.
             */
            if (manager.remainingJournalCapacity()
                    < requiredTransactions) {
                throw new IllegalStateException(
                        "Insufficient economic journal "
                                + "capacity for complete food cycle"
                );
            }

            /*
             * Check the transaction sequence space
             * for the current journal generation.
             */
            if (requiredTransactions > 0
                    && manager.transactionCount()
                    > Long.MAX_VALUE
                    - requiredTransactions - 1L) {
                throw new IllegalStateException(
                        "Transaction sequence capacity exhausted"
                );
            }

            /*
             * Execute the authoritative operations.
             * InventoryManager writes the corresponding
             * economic transaction journal records.
             */
            if (plan.harvestKg() > 0L) {
                manager.produce(
                        plan.farm(),
                        GRAIN,
                        plan.harvestKg(),
                        "agriculture:grain_harvest"
                );
            }

            if (distributed > 0L) {
                manager.transfer(
                        plan.farm(),
                        plan.household(),
                        GRAIN,
                        distributed,
                        "agriculture:household_allocation"
                );
            }

            if (consumed > 0L) {
                manager.consume(
                        plan.household(),
                        GRAIN,
                        consumed,
                        "household:grain_consumption"
                );
            }

            /*
             * Verify that the final authoritative
             * inventory balances match the forecast.
             */
            if (farm.quantity(GRAIN)
                    != remainingFarm
                    || household.quantity(GRAIN)
                    != remainingHousehold) {
                throw new IllegalStateException(
                        "Agricultural cycle accounting "
                                + "did not reconcile"
                );
            }

            return new Result(
                    plan.harvestKg(),
                    distributed,
                    consumed,
                    shortage,
                    remainingFarm,
                    remainingHousehold,
                    requiredTransactions
            );
        }
    }
}
