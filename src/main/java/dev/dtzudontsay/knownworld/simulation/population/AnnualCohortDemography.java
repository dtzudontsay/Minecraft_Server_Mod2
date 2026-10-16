package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.time.SimulationTimeContract;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Single authoritative annual birth/age/death operation for anonymous cohorts.
 * Batch 22.22 optionally uses completed real food-shortage history to modify
 * these SAME rates, only for anonymous settlement residents.
 *
 * No changes are applied to persistent NPCs, who retain LifeCycleService.
 * No automatic server callback or cross-domain atomic checkpoint is implied.
 */
public final class AnnualCohortDemography {
    public record Rates(int birthsPerAdultWomanPermille,
                        int childDeathsPermille, int adolescentDeathsPermille,
                        int adultDeathsPermille, int elderDeathsPermille,
                        int childAgingPermille, int adolescentAgingPermille,
                        int adultAgingPermille) {
        public Rates {
            for (int value : new int[]{birthsPerAdultWomanPermille,
                    childDeathsPermille, adolescentDeathsPermille, adultDeathsPermille,
                    elderDeathsPermille, childAgingPermille,
                    adolescentAgingPermille, adultAgingPermille}) {
                if (value < 0 || value > 1000) {
                    throw new IllegalArgumentException("Demographic rates must be 0..1000");
                }
            }
        }

        /** Explicit mod estimates, not ASOIAF-canonical demographic measurements. */
        public static Rates provisional() {
            return new Rates(160, 35, 8, 14, 100, 63, 250, 23);
        }

        int deathRate(PopulationCohortManager.AgeBand age) {
            return switch (age) {
                case CHILD -> childDeathsPermille;
                case ADOLESCENT -> adolescentDeathsPermille;
                case ADULT -> adultDeathsPermille;
                case ELDER -> elderDeathsPermille;
            };
        }

        int agingRate(PopulationCohortManager.AgeBand age) {
            return switch (age) {
                case CHILD -> childAgingPermille;
                case ADOLESCENT -> adolescentAgingPermille;
                case ADULT -> adultAgingPermille;
                case ELDER -> 0;
            };
        }

        /**
         * A whole-year mean daily grain deficit modifies fertility and
         * mortality. These effects are provisional balancing parameters.
         * 1000 means 100% of anonymous grain demand was unmet all year.
         * Aging remains independent of food availability.
         */
        public Rates withFoodDeficit(int deficitPermille) {
            if (deficitPermille < 0 || deficitPermille > 1000) {
                throw new IllegalArgumentException("Invalid annual food deficit");
            }
            int birthMultiplier = 1000 - deficitPermille * 600 / 1000;
            int births = birthsPerAdultWomanPermille * birthMultiplier / 1000;
            return new Rates(births,
                    capped(childDeathsPermille + deficitPermille * 350 / 1000),
                    capped(adolescentDeathsPermille + deficitPermille * 180 / 1000),
                    capped(adultDeathsPermille + deficitPermille * 150 / 1000),
                    capped(elderDeathsPermille + deficitPermille * 450 / 1000),
                    childAgingPermille, adolescentAgingPermille, adultAgingPermille);
        }

        private static int capped(int value) { return Math.min(1000, value); }
    }

    public enum Status { APPLIED, ALREADY_APPLIED }
    public record Result(Status status, int completedYear, long births,
                         long deaths, long agedToNextBand,
                         long totalBefore, long totalAfter) {}
    private record Projection(PopulationCohortManager.ExtendedSnapshot snapshot,
                              Result result) {}

    private final PopulationCohortManager population;
    private final Rates rates;
    private final long scenarioSeed;
    private final CohortFoodStressLedger foodStress;

    /** Original Batch 22.19 constructor; still uses the unmodified baseline. */
    public AnnualCohortDemography(PopulationCohortManager population,
                                  Rates rates, long scenarioSeed) {
        this(population, rates, scenarioSeed, null);
    }

    /** Food-stress integration is explicit and requires a COMPLETE recorded year. */
    public AnnualCohortDemography(PopulationCohortManager population,
                                  Rates rates, long scenarioSeed,
                                  CohortFoodStressLedger foodStress) {
        this.population = Objects.requireNonNull(population, "population");
        this.rates = Objects.requireNonNull(rates, "rates");
        this.scenarioSeed = scenarioSeed;
        this.foodStress = foodStress;
    }

    public Result advanceAtNewYear(SimulationTimeContract time) {
        synchronized (population) {
            Projection projection = prepare(time, population.extendedSnapshot());
            if (projection.result().status() == Status.APPLIED) {
                population.replaceExtended(projection.snapshot());
            }
            return projection.result();
        }
    }

    /**
     * This saves the population + processed-year marker together. It does not
     * atomically save the separate economy, food-stress or calendar files.
     */
    public Result advanceAndCheckpoint(SimulationTimeContract time,
                                       PopulationCohortPersistence persistence)
            throws IOException {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(persistence, "persistence");
        synchronized (population) {
            Result result = advanceAtNewYear(time);
            persistence.save(time.campaignAbsoluteDay(), population);
            return result;
        }
    }

    public Result previewAtNewYear(SimulationTimeContract time) {
        synchronized (population) {
            return prepare(time, population.extendedSnapshot()).result();
        }
    }

    private Projection prepare(SimulationTimeContract time,
                               PopulationCohortManager.ExtendedSnapshot previous) {
        Objects.requireNonNull(time, "time");
        if (time.campaignYear() <= 0 || time.campaignDayOfYear() != 0
                || !time.atCampaignDayBoundary()) {
            throw new IllegalArgumentException("Demography requires a completed campaign year");
        }
        int finishedYear = time.campaignYear() - 1;
        Integer last = previous.lastDemographicYear();
        if (last != null) {
            if (last == finishedYear) {
                long total = total(previous);
                return new Projection(previous, new Result(Status.ALREADY_APPLIED,
                        finishedYear, 0, 0, 0, total, total));
            }
            if (last == Integer.MAX_VALUE || last + 1 != finishedYear) {
                throw new IllegalStateException("Demographic years not consecutive: "
                        + last + " -> " + finishedYear);
            }
        }
        if (previous.settlements().surveyed().isEmpty()
                && previous.holdingSurveys().isEmpty()) {
            throw new IllegalStateException("Anonymous population not surveyed");
        }

        // Preflight EVERY participating settlement before building any new state.
        Map<SettlementId, Rates> effective = new HashMap<>();
        if (foodStress != null) {
            for (var cohort : previous.settlements().cohorts()) {
                SettlementId id = cohort.key().settlement();
                if (!effective.containsKey(id)) {
                    int deficit = foodStress.completedYearDeficitPermille(
                            id, finishedYear, time.daysPerCampaignYear());
                    effective.put(id, rates.withFoodDeficit(deficit));
                }
            }
        }

        TreeMap<PopulationCohortManager.Key, Long> settlements = new TreeMap<>();
        TreeMap<PopulationCohortManager.HoldingKey, Long> holdings = new TreeMap<>();
        long[] totals = new long[3]; // births, deaths, aged

        for (var cohort : previous.settlements().cohorts()) {
            var key = cohort.key();
            Rates applied = effective.getOrDefault(key.settlement(), rates);
            long count = cohort.people();
            long dead = countOf(count, applied.deathRate(key.age()), finishedYear,
                    "S:" + key, "death");
            long surviving = count - dead;
            long aged = countOf(surviving, applied.agingRate(key.age()), finishedYear,
                    "S:" + key, "age");
            merge(settlements, key, surviving - aged);
            if (aged > 0L) {
                merge(settlements, settlementKey(key, nextAge(key.age()),
                        key.sex(), key.occupationId()), aged);
            }
            totals[1] = Math.addExact(totals[1], dead);
            totals[2] = Math.addExact(totals[2], aged);
            if (key.age() == PopulationCohortManager.AgeBand.ADULT
                    && key.sex() == NpcSex.FEMALE) {
                long babies = countOf(surviving, applied.birthsPerAdultWomanPermille(),
                        finishedYear, "S:" + key, "birth");
                long boys = countOf(babies, 510, finishedYear, "S:" + key, "sex");
                merge(settlements, settlementKey(key, PopulationCohortManager.AgeBand.CHILD,
                        NpcSex.MALE, "unemployed"), boys);
                merge(settlements, settlementKey(key, PopulationCohortManager.AgeBand.CHILD,
                        NpcSex.FEMALE, "unemployed"), babies - boys);
                totals[0] = Math.addExact(totals[0], babies);
            }
        }

        // Unplaced holding population has no proven food-stress observations.
        // It continues to use existing baseline rates rather than invented data.
        for (var cohort : previous.holdingCohorts()) {
            var key = cohort.key();
            long count = cohort.people();
            long dead = countOf(count, rates.deathRate(key.age()), finishedYear,
                    "H:" + key, "death");
            long surviving = count - dead;
            long aged = countOf(surviving, rates.agingRate(key.age()), finishedYear,
                    "H:" + key, "age");
            merge(holdings, key, surviving - aged);
            if (aged > 0L) {
                merge(holdings, holdingKey(key, nextAge(key.age()),
                        key.sex(), key.occupationId()), aged);
            }
            totals[1] = Math.addExact(totals[1], dead);
            totals[2] = Math.addExact(totals[2], aged);
            if (key.age() == PopulationCohortManager.AgeBand.ADULT
                    && key.sex() == NpcSex.FEMALE) {
                long babies = countOf(surviving, rates.birthsPerAdultWomanPermille(),
                        finishedYear, "H:" + key, "birth");
                long boys = countOf(babies, 510, finishedYear, "H:" + key, "sex");
                merge(holdings, holdingKey(key, PopulationCohortManager.AgeBand.CHILD,
                        NpcSex.MALE, "unemployed"), boys);
                merge(holdings, holdingKey(key, PopulationCohortManager.AgeBand.CHILD,
                        NpcSex.FEMALE, "unemployed"), babies - boys);
                totals[0] = Math.addExact(totals[0], babies);
            }
        }

        List<PopulationCohortManager.Cohort> settlementRows = new ArrayList<>();
        settlements.forEach((key, count) -> settlementRows.add(
                new PopulationCohortManager.Cohort(key, count)));
        List<PopulationCohortManager.HoldingCohort> holdingRows = new ArrayList<>();
        holdings.forEach((key, count) -> holdingRows.add(
                new PopulationCohortManager.HoldingCohort(key, count)));

        var updated = new PopulationCohortManager.ExtendedSnapshot(
                new PopulationCohortManager.Snapshot(
                        previous.settlements().surveyed(), settlementRows),
                previous.holdingSurveys(), holdingRows, finishedYear);
        long before = total(previous);
        long after = total(updated);
        if (after != Math.addExact(Math.subtractExact(before, totals[1]), totals[0])) {
            throw new IllegalStateException("Demographic conservation failed");
        }
        return new Projection(updated, new Result(Status.APPLIED, finishedYear,
                totals[0], totals[1], totals[2], before, after));
    }

    private static PopulationCohortManager.AgeBand nextAge(
            PopulationCohortManager.AgeBand age) {
        return switch (age) {
            case CHILD -> PopulationCohortManager.AgeBand.ADOLESCENT;
            case ADOLESCENT -> PopulationCohortManager.AgeBand.ADULT;
            case ADULT, ELDER -> PopulationCohortManager.AgeBand.ELDER;
        };
    }

    private static PopulationCohortManager.Key settlementKey(
            PopulationCohortManager.Key key, PopulationCohortManager.AgeBand age,
            NpcSex sex, String occupation) {
        return new PopulationCohortManager.Key(key.settlement(), age, sex,
                key.cultureId(), key.religionId(), key.socialClassId(), occupation);
    }

    private static PopulationCohortManager.HoldingKey holdingKey(
            PopulationCohortManager.HoldingKey key, PopulationCohortManager.AgeBand age,
            NpcSex sex, String occupation) {
        return new PopulationCohortManager.HoldingKey(key.holding(), age, sex,
                key.cultureId(), key.religionId(), key.socialClassId(), occupation);
    }

    /** Unchanged Batch 22.19 fixed-point deterministic cohort rounding. */
    private long countOf(long people, int permille, int year,
                         String identity, String stream) {
        if (people == 0L || permille == 0) return 0L;
        BigInteger[] qr = BigInteger.valueOf(people)
                .multiply(BigInteger.valueOf(permille))
                .divideAndRemainder(BigInteger.valueOf(1000));
        long whole = qr[0].longValueExact();
        if (qr[1].signum() == 0) return whole;
        long hash = 0xcbf29ce484222325L ^ scenarioSeed;
        byte[] bytes = (year + "|" + stream + "|" + identity)
                .getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            hash ^= (b & 0xffL);
            hash *= 0x100000001b3L;
        }
        return whole + (Long.remainderUnsigned(hash, 1000L)
                < qr[1].longValueExact() ? 1L : 0L);
    }

    private static <K> void merge(Map<K, Long> target, K key, long amount) {
        if (amount > 0L) target.merge(key, amount, Math::addExact);
    }

    private static long total(PopulationCohortManager.ExtendedSnapshot state) {
        long value = 0L;
        for (var cohort : state.settlements().cohorts()) {
            value = Math.addExact(value, cohort.people());
        }
        for (var cohort : state.holdingCohorts()) {
            value = Math.addExact(value, cohort.people());
        }
        return value;
    }
}