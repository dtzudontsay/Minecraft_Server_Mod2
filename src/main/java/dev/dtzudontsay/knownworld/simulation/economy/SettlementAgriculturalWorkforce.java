
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.population.SettlementPopulationCensus;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.world.reference.WorldLocationDefinition;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Batch 22.17: one planning step assigns available settlement farmers across
 * enrolled holdings. Prevents the old error of giving every farm the *entire*
 * settlement workforce. Allocation is by provisional cultivated area, NOT
 * proof of actual farm employment or physical work completed.
 */
public final class SettlementAgriculturalWorkforce {
    public record Farm(LandedHolding holding, InventoryId inventory) {
        public Farm {
            Objects.requireNonNull(holding);
            Objects.requireNonNull(inventory);
        }
    }
    public record Result(SettlementId settlement, long availableWorkers,
                         long assignedWorkers, List<AgriculturalDayEnrollment> enrollments) {
        public Result { enrollments = List.copyOf(enrollments); }
    }
    private record Weighted(Farm farm, HoldingAgricultureCatalog.Capacity capacity,
                            long workers, BigInteger remainder) {}

    private SettlementAgriculturalWorkforce() {}

    public static Result plan(SettlementId settlement, List<Farm> farms,
                              SettlementPopulationCensus.Result census,
                              InventoryManager inventories,
                              HoldingAgricultureCatalog catalog,
                              WorldReferenceCatalog references,
                              int weatherPermille, int seasonPermille) {
        Objects.requireNonNull(references);
        return plan(settlement, farms, census, inventories, catalog,
                references::location, weatherPermille, seasonPermille);
    }

    /** Test/tooling version using existing reference identity lookups. */
    public static Result plan(SettlementId settlement, List<Farm> farms,
                              SettlementPopulationCensus.Result census,
                              InventoryManager inventories,
                              HoldingAgricultureCatalog catalog,
                              Function<String, Optional<WorldLocationDefinition>> references,
                              int weatherPermille, int seasonPermille) {
        Objects.requireNonNull(settlement);
        Objects.requireNonNull(farms);
        Objects.requireNonNull(census);
        Objects.requireNonNull(inventories);
        Objects.requireNonNull(catalog);
        Objects.requireNonNull(references);
        if (!settlement.equals(census.settlement()) || !census.hasPopulationCoverage()) {
            throw new IllegalStateException("No surveyed census for the agricultural settlement");
        }
        if (census.residentsWithoutHousehold() > 0L) {
            throw new IllegalStateException("Unassigned living household residents");
        }
        if (farms.isEmpty()) throw new IllegalArgumentException("No enrolled farm holdings");
        List<Farm> sorted = new ArrayList<>(farms);
        sorted.sort(Comparator.comparingLong(f -> f.holding().id().value()));
        Set<HoldingId> holdingIds = new HashSet<>();
        Set<InventoryId> inventoryIds = new HashSet<>();
        List<Weighted> weighted = new ArrayList<>();
        BigInteger totalHectares = BigInteger.ZERO;
        for (Farm farm : sorted) {
            if (!holdingIds.add(farm.holding().id()) || !inventoryIds.add(farm.inventory())) {
                throw new IllegalArgumentException("Duplicate farm holding or inventory");
            }
            ResourceInventory inventory = inventories.require(farm.inventory());
            if (!settlement.equals(inventory.location())
                    || !EconomicOwner.holding(farm.holding().id()).equals(inventory.owner())) {
                throw new IllegalArgumentException("Farm owner/location does not match enrollment");
            }
            var capacity = catalog.resolve(farm.holding(), references)
                    .orElseThrow(() -> new IllegalStateException(
                            "No agricultural capacity for " + farm.holding().id()));
            totalHectares = totalHectares.add(BigInteger.valueOf(capacity.cultivatedHectares()));
            weighted.add(new Weighted(farm, capacity, 0L, BigInteger.ZERO));
        }
        long workerPool = census.availableFarmWorkers();
        long assigned = 0L;
        List<Weighted> shares = new ArrayList<>();
        for (Weighted item : weighted) {
            BigInteger[] qr = totalHectares.signum() == 0
                    ? new BigInteger[]{BigInteger.ZERO, BigInteger.ZERO}
                    : BigInteger.valueOf(workerPool)
                    .multiply(BigInteger.valueOf(item.capacity().cultivatedHectares()))
                    .divideAndRemainder(totalHectares);
            long workers = qr[0].longValueExact();
            assigned = Math.addExact(assigned, workers);
            shares.add(new Weighted(item.farm(), item.capacity(), workers, qr[1]));
        }
        long spare = totalHectares.signum() == 0 ? 0L : workerPool - assigned;
        List<Integer> priority = new ArrayList<>();
        for (int i = 0; i < shares.size(); i++) priority.add(i);
        priority.sort(Comparator.<Integer, BigInteger>comparing(
                        i -> shares.get(i).remainder()).reversed()
                .thenComparingLong(i -> shares.get(i).farm().holding().id().value()));
        long[] workers = new long[shares.size()];
        for (int i = 0; i < shares.size(); i++) workers[i] = shares.get(i).workers();
        for (int i = 0; i < spare; i++) workers[priority.get(i)]++;
        List<AgriculturalDayEnrollment> enrollments = new ArrayList<>();
        for (int i = 0; i < shares.size(); i++) {
            Weighted item = shares.get(i);
            var capacity = item.capacity();
            enrollments.add(new AgriculturalDayEnrollment(
                    item.farm().holding().id(), item.farm().inventory(), settlement,
                    capacity.harvestPeriodDays(),
                    capacity.yieldInputs(workers[i], weatherPermille, seasonPermille),
                    capacity.protectedReserveKg(), capacity.gramsPerPersonPerDay()));
        }
        return new Result(settlement, workerPool,
                totalHectares.signum() == 0 ? 0L : workerPool, enrollments);
    }
}
