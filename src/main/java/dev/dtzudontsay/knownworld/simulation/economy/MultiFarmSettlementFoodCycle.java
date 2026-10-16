
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Batch 22.12.
 *
 * Multiple farms supply one settlement.
 *
 * Household food demand is calculated ONCE for
 * the complete settlement interval.
 *
 * Each farm retains its own inventory and
 * ownership identity.
 *
 * The model uses existing authoritative
 * InventoryManager operations.
 *
 * No autonomous server hooks are registered.
 */
public final class MultiFarmSettlementFoodCycle {

    private static final ResourceId GRAIN =
            AgriculturalFoodCycle.GRAIN;

    public record Farm(
            HoldingId holding,
            InventoryId inventory,
            long harvestPeriodDays,
            AgriculturalYieldModel.Inputs yieldInputs,
            long protectedReserveKg
    ) {
        public Farm {
            Objects.requireNonNull(holding, "holding");
            Objects.requireNonNull(inventory, "inventory");
            Objects.requireNonNull(yieldInputs, "yieldInputs");

            if (harvestPeriodDays <= 0L
                    || protectedReserveKg < 0L) {
                throw new IllegalArgumentException(
                        "Invalid farm interval or reserve"
                );
            }
        }

        public AgriculturalInterval interval(
                long start,
                long end
        ) {
            return new AgriculturalInterval(
                    start,
                    end,
                    harvestPeriodDays
            );
        }
    }

    public record Inputs(
            SettlementId settlement,
            long startDayInclusive,
            long endDayExclusive,
            List<Farm> farms,
            List<SettlementFoodCycle.Household> households
    ) {
        public Inputs {
            Objects.requireNonNull(settlement, "settlement");
            farms = List.copyOf(farms);
            households = List.copyOf(households);

            if (startDayInclusive < 0L
                    || endDayExclusive <= startDayInclusive
                    || startDayInclusive == Long.MAX_VALUE
                    || endDayExclusive
                    != startDayInclusive + 1L) {
                throw new IllegalArgumentException(
                        "Multi-farm operation requires "
                                + "exactly one campaign day"
                );
            }

            if (farms.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one farm is required"
                );
            }

            Set<HoldingId> holdingIds = new HashSet<>();
            Set<InventoryId> inventoryIds = new HashSet<>();

            for (Farm farm : farms) {
                if (!holdingIds.add(farm.holding())
                        || !inventoryIds.add(farm.inventory())) {
                    throw new IllegalArgumentException(
                            "Duplicate holding or farm inventory"
                    );
                }
            }

            for (SettlementFoodCycle.Household household :
                    households) {
                if (!inventoryIds.add(household.inventory())) {
                    throw new IllegalArgumentException(
                            "Duplicate household inventory or "
                                    + "farm/household inventory collision"
                    );
                }
            }
        }
    }

    public record FarmResult(
            HoldingId holding,
            InventoryId inventory,
            long harvestedKg,
            long distributedKg,
            long remainingKg
    ) {
    }

    public record HouseholdResult(
            InventoryId inventory,
            long demandKg,
            long allocatedKg,
            long consumedKg,
            long shortageKg,
            long remainingKg
    ) {
    }

    public record Transfer(
            InventoryId farm,
            InventoryId household,
            long quantityKg
    ) {
    }

    /**
     * An immutable forecast or executed result.
     *
     * The only differences between preview and
     * execution are inventory mutations and journal
     * records created by execute().
     */
    public record Result(
            List<FarmResult> farms,
            List<HouseholdResult> households,
            List<Transfer> transfers,
            long harvestedKg,
            long distributedKg,
            long consumedKg,
            long shortageKg,
            int journalEntries
    ) {
        public Result {
            farms = List.copyOf(farms);
            households = List.copyOf(households);
            transfers = List.copyOf(transfers);
        }
    }

    private record Share(
            int index,
            BigInteger remainder
    ) {
    }

    private MultiFarmSettlementFoodCycle() {
    }

    /**
     * Pure planning against the current authoritative
     * inventory balances.
     *
     * No grain or journal entry is changed.
     */
    public static Result plan(
            InventoryManager manager,
            Inputs inputs
    ) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(inputs, "inputs");

        synchronized (manager) {
            return planLocked(manager, inputs);
        }
    }

    /**
     * Preflight and execute the complete settlement
     * operation under the same InventoryManager lock.
     *
     * Expected overflow, stock and journal-capacity
     * failures are detected before mutation.
     *
     * This is not a cross-domain rollback transaction.
     */
    public static Result execute(
            InventoryManager manager,
            Inputs inputs
    ) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(inputs, "inputs");

        synchronized (manager) {
            Result result = planLocked(manager, inputs);

            for (FarmResult farm : result.farms()) {
                if (farm.harvestedKg() > 0L) {
                    manager.produce(
                            farm.inventory(),
                            GRAIN,
                            farm.harvestedKg(),
                            "settlement:multi_farm_grain_harvest"
                    );
                }
            }

            for (Transfer transfer : result.transfers()) {
                manager.transfer(
                        transfer.farm(),
                        transfer.household(),
                        GRAIN,
                        transfer.quantityKg(),
                        "settlement:multi_farm_food_allocation"
                );
            }

            for (HouseholdResult household :
                    result.households()) {
                if (household.consumedKg() > 0L) {
                    manager.consume(
                            household.inventory(),
                            GRAIN,
                            household.consumedKg(),
                            "household:multi_farm_food_consumption"
                    );
                }
            }

            for (FarmResult farm : result.farms()) {
                if (manager.require(farm.inventory())
                        .quantity(GRAIN) != farm.remainingKg()) {
                    throw new IllegalStateException(
                            "Farm accounting failed reconciliation"
                    );
                }
            }

            for (HouseholdResult household :
                    result.households()) {
                if (manager.require(household.inventory())
                        .quantity(GRAIN)
                        != household.remainingKg()) {
                    throw new IllegalStateException(
                            "Household accounting failed reconciliation"
                    );
                }
            }

            return result;
        }
    }

    private static Result planLocked(
            InventoryManager manager,
            Inputs inputs
    ) {
        List<Farm> farms = new ArrayList<>(inputs.farms());

        farms.sort(
                Comparator.comparingLong(
                        farm -> farm.holding().value()
                )
        );

        List<SettlementFoodCycle.Household> households =
                new ArrayList<>(inputs.households());

        households.sort(
                Comparator.comparing(
                        SettlementFoodCycle.Household::inventory
                )
        );

        long[] farmHarvest = new long[farms.size()];
        long[] farmStock = new long[farms.size()];
        long[] farmSupply = new long[farms.size()];

        long totalHarvest = 0L;
        BigInteger totalSupply = BigInteger.ZERO;

        /*
         * Calculate all farm production independently.
         *
         * Each farm owns only its own stock.
         */
        for (int i = 0; i < farms.size(); i++) {
            Farm farm = farms.get(i);

            ResourceInventory inventory =
                    manager.require(farm.inventory());

            if (!inventory.location()
                    .equals(inputs.settlement())) {
                throw new IllegalArgumentException(
                        "Farm belongs to another settlement"
                );
            }

            if (!inventory.owner().equals(
                    EconomicOwner.holding(farm.holding()))) {
                throw new IllegalArgumentException(
                        "Farm inventory owner does not "
                                + "match its holding"
                );
            }

            AgriculturalInterval interval =
                    farm.interval(
                            inputs.startDayInclusive(),
                            inputs.endDayExclusive()
                    );

            long harvest = interval.harvestDue()
                    ? AgriculturalYieldModel.calculate(
                    farm.yieldInputs()
            ).harvestKg()
                    : 0L;

            long stocked = Math.addExact(
                    inventory.quantity(GRAIN),
                    harvest
            );

            long supply = Math.max(
                    0L,
                    stocked - farm.protectedReserveKg()
            );

            farmHarvest[i] = harvest;
            farmStock[i] = stocked;
            farmSupply[i] = supply;

            totalHarvest = Math.addExact(
                    totalHarvest, harvest
            );

            totalSupply = totalSupply.add(
                    BigInteger.valueOf(supply)
            );
        }

        long[] demand = new long[households.size()];
        long[] existing = new long[households.size()];
        long[] unmet = new long[households.size()];

        BigInteger totalUnmet = BigInteger.ZERO;

        /*
         * Calculate household demand ONCE.
         */
        for (int i = 0; i < households.size(); i++) {
            SettlementFoodCycle.Household household =
                    households.get(i);

            ResourceInventory inventory =
                    manager.require(household.inventory());

            if (!inventory.location()
                    .equals(inputs.settlement())) {
                throw new IllegalArgumentException(
                        "Household belongs to another settlement"
                );
            }

            existing[i] = inventory.quantity(GRAIN);

            demand[i] = HouseholdFoodDemandModel
                    .calculate(
                            new HouseholdFoodDemandModel.Inputs(
                                    household.population(),
                                    1L,
                                    household.gramsPerPersonPerDay()
                            )
                    ).requiredKg();

            unmet[i] = Math.max(
                    0L,
                    demand[i] - existing[i]
            );

            totalUnmet = totalUnmet.add(
                    BigInteger.valueOf(unmet[i])
            );
        }

        /*
         * Only actual available farm resources can
         * enter distribution.
         *
         * Aggregate totals use BigInteger.
         * A settlement-wide single-day distribution
         * larger than long can represent is rejected
         * before any mutation.
         */
        long totalAllocation = totalSupply
                .min(totalUnmet)
                .longValueExact();

        long[] allocations = allocate(
                unmet,
                totalUnmet,
                totalAllocation
        );

        long[] farmDistributed = new long[farms.size()];
        long[] remainingHousehold =
                allocations.clone();

        List<Transfer> transfers = new ArrayList<>();

        int householdIndex = 0;
        long unallocated = totalAllocation;

        /*
         * Stable donor priority:
         * lower HoldingId contributes first.
         *
         * Stable recipient order:
         * lower InventoryId receives first.
         *
         * This constructs exact source/destination
         * journal operations without mixing ownership.
         */
        for (int i = 0; i < farms.size(); i++) {
            long available = Math.min(
                    farmSupply[i],
                    unallocated
            );

            while (available > 0L) {
                while (householdIndex
                        < remainingHousehold.length
                        && remainingHousehold[householdIndex]
                        == 0L) {
                    householdIndex++;
                }

                if (householdIndex
                        >= remainingHousehold.length) {
                    throw new IllegalStateException(
                            "Unallocated settlement supply"
                    );
                }

                long quantity = Math.min(
                        available,
                        remainingHousehold[householdIndex]
                );

                transfers.add(
                        new Transfer(
                                farms.get(i).inventory(),
                                households.get(householdIndex)
                                        .inventory(),
                                quantity
                        )
                );

                farmDistributed[i] = Math.addExact(
                        farmDistributed[i],
                        quantity
                );

                remainingHousehold[householdIndex]
                        -= quantity;

                available -= quantity;
                unallocated -= quantity;
            }
        }

        if (unallocated != 0L) {
            throw new IllegalStateException(
                    "Settlement allocation failed to balance"
            );
        }

        List<FarmResult> farmResults = new ArrayList<>();
        List<HouseholdResult> householdResults =
                new ArrayList<>();

        int transactions = 0;

        for (int i = 0; i < farms.size(); i++) {
            if (farmHarvest[i] > 0L) {
                transactions = Math.addExact(
                        transactions, 1
                );
            }

            farmResults.add(
                    new FarmResult(
                            farms.get(i).holding(),
                            farms.get(i).inventory(),
                            farmHarvest[i],
                            farmDistributed[i],
                            farmStock[i] - farmDistributed[i]
                    )
            );
        }

        transactions = Math.addExact(
                transactions,
                transfers.size()
        );

        long totalConsumed = 0L;
        long totalShortage = 0L;

        for (int i = 0; i < households.size(); i++) {
            long afterTransfer = Math.addExact(
                    existing[i],
                    allocations[i]
            );

            long consumed = Math.min(
                    demand[i],
                    afterTransfer
            );

            long shortage = demand[i] - consumed;
            long remaining = afterTransfer - consumed;

            totalConsumed = Math.addExact(
                    totalConsumed, consumed
            );

            totalShortage = Math.addExact(
                    totalShortage, shortage
            );

            if (consumed > 0L) {
                transactions = Math.addExact(
                        transactions, 1
                );
            }

            householdResults.add(
                    new HouseholdResult(
                            households.get(i).inventory(),
                            demand[i],
                            allocations[i],
                            consumed,
                            shortage,
                            remaining
                    )
            );
        }

        if (manager.remainingJournalCapacity()
                < transactions) {
            throw new IllegalStateException(
                    "Insufficient economic journal capacity "
                            + "for complete multi-farm settlement cycle"
            );
        }

        return new Result(
                farmResults,
                householdResults,
                transfers,
                totalHarvest,
                totalAllocation,
                totalConsumed,
                totalShortage,
                transactions
        );
    }

    /**
     * Largest-remainder proportional distribution.
     *
     * Equal remainders are resolved by the already
     * sorted household inventory order.
     */
    private static long[] allocate(
            long[] unmet,
            BigInteger totalUnmet,
            long available
    ) {
        long[] allocations = new long[unmet.length];

        if (available == 0L
                || totalUnmet.signum() == 0) {
            return allocations;
        }

        BigInteger pool = BigInteger.valueOf(available);
        List<Share> remainders = new ArrayList<>();

        long assigned = 0L;

        for (int i = 0; i < unmet.length; i++) {
            BigInteger[] division =
                    BigInteger.valueOf(unmet[i])
                            .multiply(pool)
                            .divideAndRemainder(totalUnmet);

            allocations[i] =
                    division[0].longValueExact();

            assigned = Math.addExact(
                    assigned, allocations[i]
            );

            remainders.add(
                    new Share(i, division[1])
            );
        }

        remainders.sort(
                Comparator.comparing(
                        Share::remainder,
                        Comparator.reverseOrder()
                ).thenComparingInt(Share::index)
        );

        long leftover = available - assigned;

        for (int i = 0; i < leftover; i++) {
            allocations[
                    remainders.get(i).index()
                    ]++;
        }

        return allocations;
    }
}
