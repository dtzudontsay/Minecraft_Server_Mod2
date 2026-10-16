
package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.time.SimulationTimeContract;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Batch 22.19: explicit, deterministic ANNUAL progression of anonymous cohorts.
 *
 * It never creates an NpcState, invokes LifeCycleService, or reads an NPC's
 * individual age. Named NPCs retain their existing separate birth/death path.
 *
 * Uses the existing campaign-year boundary, not Minecraft routine days.
 * Progress is stored inside PopulationCohortManager.ExtendedSnapshot and hence
 * in the same checksum-protected population checkpoint as the revised counts.
 *
 * Not installed as an automatic server callback. The caller must persist the
 * population snapshot immediately after a successful annual transition.
 */
public final class AnnualCohortDemography {

    public record Rates(
            int birthsPerAdultWomanPermille,
            int childDeathsPermille,
            int adolescentDeathsPermille,
            int adultDeathsPermille,
            int elderDeathsPermille,
            int childAgingPermille,
            int adolescentAgingPermille,
            int adultAgingPermille
    ) {
        public Rates {
            for (int value : new int[] {
                    birthsPerAdultWomanPermille,
                    childDeathsPermille,
                    adolescentDeathsPermille,
                    adultDeathsPermille,
                    elderDeathsPermille,
                    childAgingPermille,
                    adolescentAgingPermille,
                    adultAgingPermille
            }) {
                if (value < 0 || value > 1000) {
                    throw new IllegalArgumentException(
                            "Demographic rates must be 0..1000 per year");
                }
            }
        }

        /** Explicit provisional balancing assumptions, NOT ASOIAF canon. */
        public static Rates provisional() {
            return new Rates(
                    160, 35, 8, 14, 100,
                    63, 250, 23
            );
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
    }

    public enum Status {
        APPLIED,
        ALREADY_APPLIED
    }

    public record Result(
            Status status,
            int completedYear,
            long births,
            long deaths,
            long agedToNextBand,
            long totalBefore,
            long totalAfter
    ) {}

    private record Projection(
            PopulationCohortManager.ExtendedSnapshot snapshot,
            Result result
    ) {}

    private final PopulationCohortManager population;
    private final Rates rates;
    private final long scenarioSeed;

    public AnnualCohortDemography(
            PopulationCohortManager population,
            Rates rates,
            long scenarioSeed
    ) {
        this.population = Objects.requireNonNull(
                population, "population");
        this.rates = Objects.requireNonNull(rates, "rates");
        this.scenarioSeed = scenarioSeed;
    }

    /**
     * Applies the year that has JUST finished, only at day zero of the next
     * campaign year. Duplicate calls do nothing; skipped processed years fail.
     */
    public Result advanceAtNewYear(SimulationTimeContract time) {
        synchronized (population) {
            Projection projection = prepare(
                    time, population.extendedSnapshot());

            if (projection.result.status() == Status.APPLIED) {
                population.replaceExtended(
                        projection.snapshot());
            }

            return projection.result();
        }
    }

    /**
     * Controlled execution entry point: execute and save the population state
     * with its year marker. This is safe against duplicate population-year
     * application after restart, but it is NOT atomic with the NPC/calendar save.
     *
     * A persistence exception propagates. The in-memory manager retains the
     * new state; a caller may retry the same boundary to checkpoint it again,
     * but must not advance to a later year before recovery.
     */
    public Result advanceAndCheckpoint(
            SimulationTimeContract time,
            PopulationCohortPersistence persistence
    ) throws IOException {

        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(persistence, "persistence");

        synchronized (population) {
            Result result = advanceAtNewYear(time);

            persistence.save(
                    time.campaignAbsoluteDay(),
                    population
            );

            return result;
        }
    }

    /** Read-only forecast of the exact same annual transition. */
    public Result previewAtNewYear(SimulationTimeContract time) {
        synchronized (population) {
            return prepare(
                    time,
                    population.extendedSnapshot()
            ).result();
        }
    }

    private Projection prepare(
            SimulationTimeContract time,
            PopulationCohortManager.ExtendedSnapshot previous
    ) {
        Objects.requireNonNull(time, "time");

        if (time.campaignYear() <= 0
                || time.campaignDayOfYear() != 0
                || !time.atCampaignDayBoundary()) {
            throw new IllegalArgumentException(
                    "Annual population progression requires "
                            + "a completed campaign-year boundary");
        }

        int finishedYear = time.campaignYear() - 1;
        Integer last = previous.lastDemographicYear();

        if (last != null) {
            if (last == finishedYear) {
                long total = total(previous);

                return new Projection(
                        previous,
                        new Result(
                                Status.ALREADY_APPLIED,
                                finishedYear,
                                0, 0, 0, total, total
                        )
                );
            }

            if (last == Integer.MAX_VALUE
                    || last + 1 != finishedYear) {
                throw new IllegalStateException(
                        "Demographic campaign years are not consecutive: "
                                + last + " -> " + finishedYear);
            }
        }

        if (previous.settlements().surveyed().isEmpty()
                && previous.holdingSurveys().isEmpty()) {
            throw new IllegalStateException(
                    "Cannot advance uninitialized anonymous population");
        }

        TreeMap<PopulationCohortManager.Key, Long>
                settlementCounts = new TreeMap<>();

        TreeMap<PopulationCohortManager.HoldingKey, Long>
                holdingCounts = new TreeMap<>();

        long[] totals = {0L, 0L, 0L}; // births, deaths, aged

        for (var cohort : previous.settlements().cohorts()) {
            var key = cohort.key();
            long count = cohort.people();

            long dead = countOf(
                    count,
                    rates.deathRate(key.age()),
                    finishedYear,
                    "S:" + key,
                    "death"
            );

            long survive = count - dead;

            long aging = countOf(
                    survive,
                    rates.agingRate(key.age()),
                    finishedYear,
                    "S:" + key,
                    "age"
            );

            merge(settlementCounts, key, survive - aging);

            if (aging > 0) {
                merge(
                        settlementCounts,
                        settlementKey(
                                key,
                                nextAge(key.age()),
                                key.sex(),
                                key.occupationId()
                        ),
                        aging
                );
            }

            totals[1] = Math.addExact(totals[1], dead);
            totals[2] = Math.addExact(totals[2], aging);

            if (key.age() == PopulationCohortManager.AgeBand.ADULT
                    && key.sex() == NpcSex.FEMALE) {

                long babies = countOf(
                        survive,
                        rates.birthsPerAdultWomanPermille(),
                        finishedYear,
                        "S:" + key,
                        "birth"
                );

                long boys = countOf(
                        babies, 510, finishedYear,
                        "S:" + key, "sex"
                );

                merge(
                        settlementCounts,
                        settlementKey(
                                key,
                                PopulationCohortManager.AgeBand.CHILD,
                                NpcSex.MALE,
                                "unemployed"
                        ),
                        boys
                );

                merge(
                        settlementCounts,
                        settlementKey(
                                key,
                                PopulationCohortManager.AgeBand.CHILD,
                                NpcSex.FEMALE,
                                "unemployed"
                        ),
                        babies - boys
                );

                totals[0] = Math.addExact(
                        totals[0], babies);
            }
        }

        for (var cohort : previous.holdingCohorts()) {
            var key = cohort.key();
            long count = cohort.people();

            long dead = countOf(
                    count,
                    rates.deathRate(key.age()),
                    finishedYear,
                    "H:" + key,
                    "death"
            );

            long survive = count - dead;

            long aging = countOf(
                    survive,
                    rates.agingRate(key.age()),
                    finishedYear,
                    "H:" + key,
                    "age"
            );

            merge(holdingCounts, key, survive - aging);

            if (aging > 0) {
                merge(
                        holdingCounts,
                        holdingKey(
                                key,
                                nextAge(key.age()),
                                key.sex(),
                                key.occupationId()
                        ),
                        aging
                );
            }

            totals[1] = Math.addExact(totals[1], dead);
            totals[2] = Math.addExact(totals[2], aging);

            if (key.age() == PopulationCohortManager.AgeBand.ADULT
                    && key.sex() == NpcSex.FEMALE) {

                long babies = countOf(
                        survive,
                        rates.birthsPerAdultWomanPermille(),
                        finishedYear,
                        "H:" + key,
                        "birth"
                );

                long boys = countOf(
                        babies, 510, finishedYear,
                        "H:" + key, "sex"
                );

                merge(
                        holdingCounts,
                        holdingKey(
                                key,
                                PopulationCohortManager.AgeBand.CHILD,
                                NpcSex.MALE,
                                "unemployed"
                        ),
                        boys
                );

                merge(
                        holdingCounts,
                        holdingKey(
                                key,
                                PopulationCohortManager.AgeBand.CHILD,
                                NpcSex.FEMALE,
                                "unemployed"
                        ),
                        babies - boys
                );

                totals[0] = Math.addExact(
                        totals[0], babies);
            }
        }

        List<PopulationCohortManager.Cohort> settlements =
                new ArrayList<>();

        settlementCounts.forEach((key, count) ->
                settlements.add(
                        new PopulationCohortManager.Cohort(
                                key, count
                        )
                )
        );

        List<PopulationCohortManager.HoldingCohort> holdings =
                new ArrayList<>();

        holdingCounts.forEach((key, count) ->
                holdings.add(
                        new PopulationCohortManager.HoldingCohort(
                                key, count
                        )
                )
        );

        var updated = new PopulationCohortManager.ExtendedSnapshot(
                new PopulationCohortManager.Snapshot(
                        previous.settlements().surveyed(),
                        settlements
                ),
                previous.holdingSurveys(),
                holdings,
                finishedYear
        );

        long before = total(previous);
        long after = total(updated);

        if (after != Math.addExact(
                Math.subtractExact(before, totals[1]),
                totals[0])) {
            throw new IllegalStateException(
                    "Demographic population conservation failed");
        }

        return new Projection(
                updated,
                new Result(
                        Status.APPLIED,
                        finishedYear,
                        totals[0],
                        totals[1],
                        totals[2],
                        before,
                        after
                )
        );
    }

    private static PopulationCohortManager.AgeBand nextAge(
            PopulationCohortManager.AgeBand age
    ) {
        return switch (age) {
            case CHILD -> PopulationCohortManager.AgeBand.ADOLESCENT;
            case ADOLESCENT -> PopulationCohortManager.AgeBand.ADULT;
            case ADULT, ELDER -> PopulationCohortManager.AgeBand.ELDER;
        };
    }

    private static PopulationCohortManager.Key settlementKey(
            PopulationCohortManager.Key k,
            PopulationCohortManager.AgeBand age,
            NpcSex sex,
            String occupation
    ) {
        return new PopulationCohortManager.Key(
                k.settlement(), age, sex,
                k.cultureId(), k.religionId(),
                k.socialClassId(), occupation
        );
    }

    private static PopulationCohortManager.HoldingKey holdingKey(
            PopulationCohortManager.HoldingKey k,
            PopulationCohortManager.AgeBand age,
            NpcSex sex,
            String occupation
    ) {
        return new PopulationCohortManager.HoldingKey(
                k.holding(), age, sex,
                k.cultureId(), k.religionId(),
                k.socialClassId(), occupation
        );
    }

    /**
     * Deterministic fractional rounding.
     * No shared random stream is consumed.
     */
    private long countOf(
            long people,
            int permille,
            int year,
            String identity,
            String stream
    ) {
        if (people == 0L || permille == 0) {
            return 0L;
        }

        BigInteger[] qr = BigInteger.valueOf(people)
                .multiply(BigInteger.valueOf(permille))
                .divideAndRemainder(BigInteger.valueOf(1000));

        long whole = qr[0].longValueExact();

        if (qr[1].signum() == 0) {
            return whole;
        }

        long hash = 0xcbf29ce484222325L ^ scenarioSeed;

        byte[] bytes = (year + "|" + stream + "|" + identity)
                .getBytes(StandardCharsets.UTF_8);

        for (byte b : bytes) {
            hash ^= (b & 0xffL);
            hash *= 0x100000001b3L;
        }

        return whole + (
                Long.remainderUnsigned(hash, 1000L)
                        < qr[1].longValueExact()
                        ? 1L : 0L
        );
    }

    private static <K> void merge(
            Map<K, Long> target,
            K key,
            long amount
    ) {
        if (amount > 0L) {
            target.merge(
                    key, amount, Math::addExact);
        }
    }

    private static long total(
            PopulationCohortManager.ExtendedSnapshot state
    ) {
        long sum = 0L;

        for (var c : state.settlements().cohorts()) {
            sum = Math.addExact(sum, c.people());
        }

        for (var c : state.holdingCohorts()) {
            sum = Math.addExact(sum, c.people());
        }

        return sum;
    }
}
