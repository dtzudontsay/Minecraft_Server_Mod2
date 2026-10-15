
package dev.dtzudontsay.knownworld.simulation.economy;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Batch 22.8.
 *
 * One farm, multiple households, one simulation
 * interval, and one deterministic food allocation
 * policy.
 *
 * Existing inventories remain authoritative.
 *
 * All expected stock, arithmetic and journal
 * capacity errors are checked before mutations.
 *
 * No inventory or resource is created implicitly.
 *
 * This class does not register server tick hooks
 * or automatically execute agricultural intervals.
 */
public final class SettlementFoodCycle {

    private static final ResourceId GRAIN =
            AgriculturalFoodCycle.GRAIN;

    /**
     * An existing household inventory plus the
     * demographic inputs required for food demand.
     *
     * The household identity is provisional:
     * InventoryId is not a permanent household ID.
     */
    public record Household(
            InventoryId inventory,
            long population,
            long gramsPerPersonPerDay
    ) {
        public Household {
            Objects.requireNonNull(
                    inventory, "inventory"
            );

            if (population < 0L
                    || gramsPerPersonPerDay < 0L) {
                throw new IllegalArgumentException(
                        "Invalid household food inputs"
                );
            }
        }
    }

    /**
     * Complete inputs for one settlement food cycle.
     *
     * Harvest yield is calculated only when the
     * supplied interval crosses a harvest boundary.
     *
     * Protected farm grain cannot be distributed.
     */
    public record Inputs(
            InventoryId farm,
            AgriculturalInterval interval,
            AgriculturalYieldModel.Inputs harvestInputs,
            long protectedFarmReserveKg,
            List<Household> households
    ) {
        public Inputs {
            Objects.requireNonNull(farm, "farm");
            Objects.requireNonNull(
                    interval, "interval"
            );
            Objects.requireNonNull(
                    harvestInputs, "harvestInputs"
            );
            Objects.requireNonNull(
                    households, "households"
            );

            if (protectedFarmReserveKg < 0L) {
                throw new IllegalArgumentException(
                        "Farm reserve cannot be negative"
                );
            }

            households = List.copyOf(households);

            Set<InventoryId> seen = new HashSet<>();

            for (Household household : households) {
                if (household.inventory().equals(farm)
                        || !seen.add(
                        household.inventory()
                )) {
                    throw new IllegalArgumentException(
                            "Duplicate household or "
                                    + "farm inventory"
                    );
                }
            }
        }
    }

    public record HouseholdResult(
            InventoryId inventory,
            long demandKg,
            long allocationKg,
            long consumedKg,
            long shortageKg,
            long remainingKg
    ) {
    }

    public record Result(
            long harvestedKg,
            long distributedKg,
            long consumedKg,
            long shortageKg,
            long farmRemainingKg,
            List<HouseholdResult> households,
            int transactions
    ) {
        public Result {
            households = List.copyOf(households);
        }
    }

    /**
     * A household's remainder in the proportional
     * distribution algorithm.
     *
     * BigInteger is required because aggregate
     * unmet demand may exceed Long.MAX_VALUE
     * even when individual demands are valid.
     */
    private record Share(
            int index,
            BigInteger remainder
    ) {
    }

    private SettlementFoodCycle() {
    }

    /**
     * Execute exactly one agricultural interval.
     *
     * IMPORTANT:
     * The caller is responsible for preventing
     * duplicate processing of the same interval.
     *
     * Runtime calendar integration and persistent
     * processed-interval tracking are future work.
     */
    public static Result execute(
            InventoryManager manager,
            Inputs inputs
    ) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(inputs, "inputs");

        synchronized (manager) {

            ResourceInventory farm =
                    manager.require(inputs.farm());

            List<Household> households =
                    new ArrayList<>(inputs.households());

            /*
             * Stable ordering ensures that input
             * collection order cannot affect
             * proportional allocation or tie-breaks.
             */
            households.sort(
                    Comparator.comparing(
                            Household::inventory
                    )
            );

            ResourceInventory[] stocks =
                    new ResourceInventory[
                            households.size()
                            ];

            long[] demand =
                    new long[households.size()];

            long[] existing =
                    new long[households.size()];

            long[] unmet =
                    new long[households.size()];

            BigInteger totalUnmet = BigInteger.ZERO;

            /*
             * Phase 1:
             * Calculate demand and pre-existing
             * household food stocks.
             *
             * No mutation happens in this phase.
             */
            for (int i = 0;
                 i < households.size();
                 i++) {

                Household household =
                        households.get(i);

                ResourceInventory stock =
                        manager.require(
                                household.inventory()
                        );

                if (!farm.location().equals(
                        stock.location()
                )) {
                    throw new IllegalArgumentException(
                            "Household is outside "
                                    + "the farm settlement"
                    );
                }

                stocks[i] = stock;

                existing[i] =
                        stock.quantity(GRAIN);

                HouseholdFoodDemandModel.Result
                        calculatedDemand =
                        HouseholdFoodDemandModel.calculate(
                                new HouseholdFoodDemandModel.Inputs(
                                        household.population(),
                                        inputs.interval().days(),
                                        household.gramsPerPersonPerDay()
                                )
                        );

                demand[i] =
                        calculatedDemand.requiredKg();

                unmet[i] = Math.max(
                        0L,
                        demand[i] - existing[i]
                );

                totalUnmet = totalUnmet.add(
                        BigInteger.valueOf(unmet[i])
                );
            }

            /*
             * Phase 2:
             * Calculate the harvest.
             *
             * The yield coefficients must represent
             * the harvest period supplied by the
             * caller, not an arbitrary daily yield.
             */
            long harvest =
                    inputs.interval().harvestDue()
                            ? AgriculturalYieldModel
                            .calculate(
                                    inputs.harvestInputs()
                            ).harvestKg()
                            : 0L;

            long availableFarm = Math.addExact(
                    farm.quantity(GRAIN),
                    harvest
            );

            /*
             * Protected reserves are not available
             * for household distribution.
             */
            long distributable = Math.max(
                    0L,
                    availableFarm
                            - inputs.protectedFarmReserveKg()
            );

            /*
             * Aggregate household demand may exceed
             * a signed long.
             *
             * Compute the allocation pool using
             * BigInteger, then convert back because
             * available inventory remains long-based.
             */
            BigInteger toDistribute =
                    BigInteger.valueOf(distributable)
                            .min(totalUnmet);

            long totalAllocation =
                    toDistribute.longValueExact();

            long[] allocations = allocate(
                    households,
                    unmet,
                    totalUnmet,
                    totalAllocation
            );

            /*
             * Phase 3:
             * Precompute all household outcomes.
             */
            List<HouseholdResult> results =
                    new ArrayList<>();

            long consumedTotal = 0L;
            BigInteger shortageTotal = BigInteger.ZERO;

            int operations =
                    harvest > 0L ? 1 : 0;

            for (int i = 0;
                 i < households.size();
                 i++) {

                long afterTransfer = Math.addExact(
                        existing[i],
                        allocations[i]
                );

                long consumed = Math.min(
                        demand[i],
                        afterTransfer
                );

                long shortage =
                        demand[i] - consumed;

                long remaining =
                        afterTransfer - consumed;

                if (allocations[i] > 0L) {
                    operations =
                            Math.addExact(operations, 1);
                }

                if (consumed > 0L) {
                    operations =
                            Math.addExact(operations, 1);
                }

                consumedTotal = Math.addExact(
                        consumedTotal,
                        consumed
                );

                shortageTotal = shortageTotal.add(
                        BigInteger.valueOf(shortage)
                );

                results.add(
                        new HouseholdResult(
                                households.get(i)
                                        .inventory(),
                                demand[i],
                                allocations[i],
                                consumed,
                                shortage,
                                remaining
                        )
                );
            }

            /*
             * Phase 4:
             * Verify that the complete plan fits
             * the available transaction journal.
             *
             * This must happen before producing,
             * transferring or consuming anything.
             */
            if (manager.remainingJournalCapacity()
                    < operations) {
                throw new IllegalStateException(
                        "Insufficient journal capacity "
                                + "for settlement food cycle"
                );
            }

            long farmRemaining =
                    availableFarm - totalAllocation;

            long shortageKg =
                    shortageTotal.longValueExact();

            /*
             * Phase 5:
             * Execute authoritative mutations.
             *
             * All expected balance and capacity
             * errors were checked above.
             */
            if (harvest > 0L) {
                manager.produce(
                        inputs.farm(),
                        GRAIN,
                        harvest,
                        "settlement:grain_harvest"
                );
            }

            for (HouseholdResult household : results) {

                if (household.allocationKg() > 0L) {
                    manager.transfer(
                            inputs.farm(),
                            household.inventory(),
                            GRAIN,
                            household.allocationKg(),
                            "settlement:food_allocation"
                    );
                }

                if (household.consumedKg() > 0L) {
                    manager.consume(
                            household.inventory(),
                            GRAIN,
                            household.consumedKg(),
                            "household:food_consumption"
                    );
                }
            }

            /*
             * Phase 6:
             * Verify the final inventory balances.
             */
            if (farm.quantity(GRAIN)
                    != farmRemaining) {
                throw new IllegalStateException(
                        "Farm balance failed reconciliation"
                );
            }

            for (int i = 0;
                 i < results.size();
                 i++) {

                if (stocks[i].quantity(GRAIN)
                        != results.get(i).remainingKg()) {
                    throw new IllegalStateException(
                            "Household balance failed "
                                    + "reconciliation"
                    );
                }
            }

            return new Result(
                    harvest,
                    totalAllocation,
                    consumedTotal,
                    shortageKg,
                    farmRemaining,
                    results,
                    operations
            );
        }
    }

    /**
     * Proportional allocation using the
     * largest-remainder method.
     *
     * Equal remainders are resolved in favor
     * of lower inventory IDs.
     *
     * All intermediate proportional arithmetic
     * uses BigInteger.
     */
    private static long[] allocate(
            List<Household> households,
            long[] unmet,
            BigInteger totalUnmet,
            long available
    ) {
        long[] result =
                new long[households.size()];

        if (available == 0L
                || totalUnmet.signum() == 0) {
            return result;
        }

        BigInteger pool =
                BigInteger.valueOf(available);

        List<Share> remainders =
                new ArrayList<>();

        long assigned = 0L;

        for (int i = 0;
             i < unmet.length;
             i++) {

            BigInteger numerator =
                    BigInteger.valueOf(unmet[i])
                            .multiply(pool);

            BigInteger[] division =
                    numerator.divideAndRemainder(
                            totalUnmet
                    );

            result[i] =
                    division[0].longValueExact();

            assigned = Math.addExact(
                    assigned,
                    result[i]
            );

            remainders.add(
                    new Share(
                            i,
                            division[1]
                    )
            );
        }

        /*
         * Largest remainder first.
         *
         * Household order is already sorted
         * by inventory ID, so index order
         * provides deterministic tie-breaking.
         */
        remainders.sort(
                Comparator.comparing(
                        Share::remainder,
                        Comparator.reverseOrder()
                ).thenComparingInt(
                        Share::index
                )
        );

        long leftovers =
                available - assigned;

        /*
         * Largest-remainder allocation has fewer
         * leftover whole units than household count.
         */
        for (int i = 0;
             i < leftovers;
             i++) {

            result[
                    remainders.get(i).index()
                    ]++;
        }

        return result;
    }
}
