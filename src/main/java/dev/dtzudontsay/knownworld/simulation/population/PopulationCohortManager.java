package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHoldingManager;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * One authoritative store of anonymous inhabitants.
 *
 * A cohort belongs either to an instantiated settlement
 * OR to an unplaced holding, never both.
 *
 * Persistent named NPCs belong solely to NpcRegistry.
 *
 * Holding population is NOT inferred from
 * LandedHolding.populationWeight.
 *
 * Counts must be authored or explicitly supplied by
 * a future demographic model.
 *
 * Batch 22.19:
 * Supports annual demographic progression through
 * a persistent lastDemographicYear marker.
 */
public final class PopulationCohortManager {

    public enum AgeBand {
        CHILD,
        ADOLESCENT,
        ADULT,
        ELDER
    }

    public enum Basis {
        MOD_ESTIMATE,
        RESEARCHED_ESTIMATE,
        USER_AUTHORED
    }

    public record Key(
            SettlementId settlement,
            AgeBand age,
            NpcSex sex,
            String cultureId,
            String religionId,
            String socialClassId,
            String occupationId
    ) implements Comparable<Key> {

        public Key {
            Objects.requireNonNull(settlement, "settlement");
            Objects.requireNonNull(age, "age");
            Objects.requireNonNull(sex, "sex");

            cultureId = requireId(cultureId);
            religionId = requireId(religionId);
            socialClassId = requireId(socialClassId);
            occupationId = requireId(occupationId);
        }

        @Override
        public int compareTo(Key other) {

            int c = settlement.compareTo(other.settlement);
            if (c != 0) return c;

            c = age.compareTo(other.age);
            if (c != 0) return c;

            c = sex.compareTo(other.sex);
            if (c != 0) return c;

            c = cultureId.compareTo(other.cultureId);
            if (c != 0) return c;

            c = religionId.compareTo(other.religionId);
            if (c != 0) return c;

            c = socialClassId.compareTo(other.socialClassId);

            return c != 0
                    ? c
                    : occupationId.compareTo(other.occupationId);
        }

        public Key at(SettlementId destination) {

            return new Key(
                    destination,
                    age,
                    sex,
                    cultureId,
                    religionId,
                    socialClassId,
                    occupationId
            );
        }
    }

    public record HoldingKey(
            HoldingId holding,
            AgeBand age,
            NpcSex sex,
            String cultureId,
            String religionId,
            String socialClassId,
            String occupationId
    ) implements Comparable<HoldingKey> {

        public HoldingKey {
            Objects.requireNonNull(holding, "holding");
            Objects.requireNonNull(age, "age");
            Objects.requireNonNull(sex, "sex");

            cultureId = requireId(cultureId);
            religionId = requireId(religionId);
            socialClassId = requireId(socialClassId);
            occupationId = requireId(occupationId);
        }

        @Override
        public int compareTo(HoldingKey other) {

            int c = holding.compareTo(other.holding);
            if (c != 0) return c;

            c = age.compareTo(other.age);
            if (c != 0) return c;

            c = sex.compareTo(other.sex);
            if (c != 0) return c;

            c = cultureId.compareTo(other.cultureId);
            if (c != 0) return c;

            c = religionId.compareTo(other.religionId);
            if (c != 0) return c;

            c = socialClassId.compareTo(other.socialClassId);

            return c != 0
                    ? c
                    : occupationId.compareTo(other.occupationId);
        }

        public HoldingKey at(HoldingId target) {

            return new HoldingKey(
                    target,
                    age,
                    sex,
                    cultureId,
                    religionId,
                    socialClassId,
                    occupationId
            );
        }

        public Key at(SettlementId target) {

            return new Key(
                    target,
                    age,
                    sex,
                    cultureId,
                    religionId,
                    socialClassId,
                    occupationId
            );
        }
    }

    public record Cohort(
            Key key,
            long people
    ) {

        public Cohort {
            Objects.requireNonNull(key, "key");

            if (people <= 0) {
                throw new IllegalArgumentException(
                        "Empty cohort"
                );
            }
        }
    }

    public record HoldingCohort(
            HoldingKey key,
            long people
    ) {

        public HoldingCohort {
            Objects.requireNonNull(key, "key");

            if (people <= 0) {
                throw new IllegalArgumentException(
                        "Empty holding cohort"
                );
            }
        }
    }

    /**
     * Original Batch 22.17 snapshot contract.
     *
     * Preserved for backward compatibility.
     */
    public record Snapshot(
            Map<SettlementId, Basis> surveyed,
            List<Cohort> cohorts
    ) {

        public Snapshot {
            surveyed = Map.copyOf(surveyed);
            cohorts = List.copyOf(cohorts);
        }
    }

    /**
     * Combined population checkpoint.
     *
     * Includes settlement cohorts, holding cohorts,
     * and annual demographic progression.
     */
    public record ExtendedSnapshot(
            Snapshot settlements,
            Map<HoldingId, Basis> holdingSurveys,
            List<HoldingCohort> holdingCohorts,
            Integer lastDemographicYear
    ) {

        public ExtendedSnapshot {

            Objects.requireNonNull(
                    settlements,
                    "settlements"
            );

            holdingSurveys = Map.copyOf(holdingSurveys);
            holdingCohorts = List.copyOf(holdingCohorts);

            if (lastDemographicYear != null
                    && lastDemographicYear < 0) {

                throw new IllegalArgumentException(
                        "Negative demographic year"
                );
            }
        }

        /**
         * Compatibility with Batch 22.18.
         */
        public ExtendedSnapshot(
                Snapshot settlements,
                Map<HoldingId, Basis> surveys,
                List<HoldingCohort> cohorts
        ) {

            this(
                    settlements,
                    surveys,
                    cohorts,
                    null
            );
        }
    }

    private final SettlementManager settlements;

    private final Function<
            HoldingId,
            Optional<LandedHolding>
            > holdingLookup;

    private final TreeMap<Key, Long> settlementCounts =
            new TreeMap<>();

    private final Map<SettlementId, Basis> settlementSurveys =
            new HashMap<>();

    private final TreeMap<HoldingKey, Long> holdingCounts =
            new TreeMap<>();

    private final Map<HoldingId, Basis> holdingSurveys =
            new HashMap<>();

    /**
     * Null until annual demographic simulation
     * first completes.
     */
    private Integer lastDemographicYear;

    /**
     * Settlement-only compatibility constructor.
     *
     * Holding operations fail closed.
     */
    public PopulationCohortManager(
            SettlementManager settlements
    ) {

        this(
                settlements,
                id -> Optional.empty()
        );
    }

    public PopulationCohortManager(
            SettlementManager settlements,
            LandedHoldingManager holdings
    ) {

        this(
                settlements,
                Objects.requireNonNull(
                        holdings,
                        "holdings"
                )::find
        );
    }

    /**
     * Lookup-based construction for runtime,
     * scenario tools, and deterministic tests.
     */
    public PopulationCohortManager(
            SettlementManager settlements,
            Function<HoldingId, Optional<LandedHolding>>
                    holdingLookup
    ) {

        this.settlements =
                Objects.requireNonNull(
                        settlements,
                        "settlements"
                );

        this.holdingLookup =
                Objects.requireNonNull(
                        holdingLookup,
                        "holdingLookup"
                );
    }

    // =====================================================
    // Settlement population
    // =====================================================

    public synchronized void markSurveyed(
            SettlementId settlement,
            Basis basis
    ) {

        checkSettlement(settlement);
        Objects.requireNonNull(basis, "basis");

        Basis old =
                settlementSurveys.putIfAbsent(
                        settlement,
                        basis
                );

        if (old != null && old != basis) {
            throw new IllegalStateException(
                    "Settlement population basis already fixed"
            );
        }
    }

    public synchronized boolean surveyed(
            SettlementId id
    ) {

        return settlementSurveys.containsKey(id);
    }

    public synchronized long count(
            Key key
    ) {

        Objects.requireNonNull(key, "key");

        return settlementCounts.getOrDefault(
                key,
                0L
        );
    }

    public synchronized void change(
            Key key,
            long delta
    ) {

        Objects.requireNonNull(key, "key");

        requireSettlementSurvey(
                key.settlement()
        );

        putCount(
                settlementCounts,
                key,
                delta
        );
    }

    /**
     * Move anonymous inhabitants between
     * instantiated settlements.
     */
    public synchronized void migrate(
            Key from,
            SettlementId to,
            long people
    ) {

        Objects.requireNonNull(from, "from");

        requireSettlementSurvey(
                from.settlement()
        );

        requireSettlementSurvey(to);

        if (people < 0L) {
            throw new IllegalArgumentException(
                    "Negative migration"
            );
        }

        if (people == 0L
                || from.settlement().equals(to)) {
            return;
        }

        Key destination = from.at(to);

        transfer(
                settlementCounts,
                from,
                settlementCounts,
                destination,
                people
        );
    }

    // =====================================================
    // Holding population
    // =====================================================

    public synchronized void markHoldingSurveyed(
            HoldingId holding,
            Basis basis
    ) {

        checkHolding(holding);

        Objects.requireNonNull(basis, "basis");

        Basis old =
                holdingSurveys.putIfAbsent(
                        holding,
                        basis
                );

        if (old != null && old != basis) {
            throw new IllegalStateException(
                    "Holding population basis already fixed"
            );
        }
    }

    public synchronized boolean holdingSurveyed(
            HoldingId id
    ) {

        return holdingSurveys.containsKey(id);
    }

    public synchronized long holdingCount(
            HoldingKey key
    ) {

        Objects.requireNonNull(key, "key");

        return holdingCounts.getOrDefault(
                key,
                0L
        );
    }

    public synchronized void changeHolding(
            HoldingKey key,
            long delta
    ) {

        Objects.requireNonNull(key, "key");

        requireHoldingSurvey(
                key.holding()
        );

        putCount(
                holdingCounts,
                key,
                delta
        );
    }

    /**
     * Move anonymous inhabitants between
     * existing landed holdings.
     */
    public synchronized void migrateHolding(
            HoldingKey from,
            HoldingId to,
            long people
    ) {

        Objects.requireNonNull(from, "from");

        requireHoldingSurvey(
                from.holding()
        );

        requireHoldingSurvey(to);

        if (people < 0L) {
            throw new IllegalArgumentException(
                    "Negative migration"
            );
        }

        if (people == 0L
                || from.holding().equals(to)) {
            return;
        }

        transfer(
                holdingCounts,
                from,
                holdingCounts,
                from.at(to),
                people
        );
    }

    /**
     * Explicitly relocate anonymous people
     * from an unplaced holding to an
     * instantiated settlement.
     *
     * Does not materialize an NPC or
     * move people already represented
     * in NpcRegistry.
     */
    public synchronized void moveToSettlement(
            HoldingKey from,
            SettlementId to,
            long people
    ) {

        Objects.requireNonNull(from, "from");

        requireHoldingSurvey(
                from.holding()
        );

        requireSettlementSurvey(to);

        if (people < 0L) {
            throw new IllegalArgumentException(
                    "Negative migration"
            );
        }

        if (people == 0L) {
            return;
        }

        transfer(
                holdingCounts,
                from,
                settlementCounts,
                from.at(to),
                people
        );
    }

    // =====================================================
    // Snapshots
    // =====================================================

    public synchronized Snapshot snapshot() {

        List<Cohort> entries =
                new ArrayList<>();

        settlementCounts.forEach(
                (key, people) ->
                        entries.add(
                                new Cohort(
                                        key,
                                        people
                                )
                        )
        );

        return new Snapshot(
                settlementSurveys,
                entries
        );
    }

    public synchronized ExtendedSnapshot extendedSnapshot() {

        List<HoldingCohort> entries =
                new ArrayList<>();

        holdingCounts.forEach(
                (key, people) ->
                        entries.add(
                                new HoldingCohort(
                                        key,
                                        people
                                )
                        )
        );

        return new ExtendedSnapshot(
                snapshot(),
                holdingSurveys,
                entries,
                lastDemographicYear
        );
    }

    /**
     * Compatibility method.
     *
     * Replaces settlement population only.
     */
    public synchronized void replaceAll(
            Snapshot state
    ) {

        Objects.requireNonNull(state, "state");

        ValidatedSettlements validated =
                validateSettlements(state);

        settlementCounts.clear();
        settlementCounts.putAll(
                validated.counts()
        );

        settlementSurveys.clear();
        settlementSurveys.putAll(
                validated.surveys()
        );
    }

    /**
     * All-or-nothing validation of both
     * settlement and holding population.
     */
    public synchronized void replaceExtended(
            ExtendedSnapshot state
    ) {

        Objects.requireNonNull(state, "state");

        ValidatedSettlements settlementData =
                validateSettlements(
                        state.settlements()
                );

        Map<HoldingId, Basis> surveys =
                new HashMap<>();

        for (
                var entry :
                state.holdingSurveys().entrySet()
        ) {

            checkHolding(
                    entry.getKey()
            );

            surveys.put(
                    entry.getKey(),
                    Objects.requireNonNull(
                            entry.getValue()
                    )
            );
        }

        TreeMap<HoldingKey, Long> counts =
                new TreeMap<>();

        for (
                HoldingCohort entry :
                state.holdingCohorts()
        ) {

            checkHolding(
                    entry.key().holding()
            );

            if (
                    !surveys.containsKey(
                            entry.key().holding()
                    )
                            || counts.putIfAbsent(
                            entry.key(),
                            entry.people()
                    ) != null
            ) {

                throw new IllegalArgumentException(
                        "Unsurveyed or duplicate holding cohort"
                );
            }
        }

        settlementCounts.clear();
        settlementCounts.putAll(
                settlementData.counts()
        );

        settlementSurveys.clear();
        settlementSurveys.putAll(
                settlementData.surveys()
        );

        holdingCounts.clear();
        holdingCounts.putAll(counts);

        holdingSurveys.clear();
        holdingSurveys.putAll(surveys);

        lastDemographicYear =
                state.lastDemographicYear();
    }

    /**
     * Annual demographic progress marker.
     *
     * Stored with the population snapshot
     * to prevent duplicate annual changes.
     */
    public synchronized Optional<Integer>
    lastDemographicYear() {

        return Optional.ofNullable(
                lastDemographicYear
        );
    }

    // =====================================================
    // Internal validation
    // =====================================================

    private record ValidatedSettlements(
            Map<SettlementId, Basis> surveys,
            TreeMap<Key, Long> counts
    ) {
    }

    private ValidatedSettlements validateSettlements(
            Snapshot state
    ) {

        Map<SettlementId, Basis> surveys =
                new HashMap<>();

        for (
                var entry :
                state.surveyed().entrySet()
        ) {

            checkSettlement(
                    entry.getKey()
            );

            surveys.put(
                    entry.getKey(),
                    Objects.requireNonNull(
                            entry.getValue()
                    )
            );
        }

        TreeMap<Key, Long> counts =
                new TreeMap<>();

        for (
                Cohort entry :
                state.cohorts()
        ) {

            checkSettlement(
                    entry.key().settlement()
            );

            if (
                    !surveys.containsKey(
                            entry.key().settlement()
                    )
                            || counts.putIfAbsent(
                            entry.key(),
                            entry.people()
                    ) != null
            ) {

                throw new IllegalArgumentException(
                        "Unsurveyed or duplicate settlement cohort"
                );
            }
        }

        return new ValidatedSettlements(
                surveys,
                counts
        );
    }

    private void requireSettlementSurvey(
            SettlementId id
    ) {

        checkSettlement(id);

        if (!surveyed(id)) {
            throw new IllegalStateException(
                    "Unsurveyed settlement: " + id
            );
        }
    }

    private void requireHoldingSurvey(
            HoldingId id
    ) {

        checkHolding(id);

        if (!holdingSurveyed(id)) {
            throw new IllegalStateException(
                    "Unsurveyed holding: " + id
            );
        }
    }

    private void checkSettlement(
            SettlementId id
    ) {

        Objects.requireNonNull(
                id,
                "settlement"
        );

        if (settlements.find(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown settlement: " + id
            );
        }
    }

    private void checkHolding(
            HoldingId id
    ) {

        Objects.requireNonNull(
                id,
                "holding"
        );

        LandedHolding holding =
                Objects.requireNonNull(
                        holdingLookup.apply(id),
                        "holding lookup result"
                ).orElseThrow(
                        () -> new IllegalArgumentException(
                                "Unknown holding: " + id
                        )
                );

        if (
                !id.equals(holding.id())
                        || holding.type()
                        == HoldingType.REGION
                        || holding.type()
                        == HoldingType.TERRITORY
        ) {

            throw new IllegalArgumentException(
                    "Aggregate or mismatched holding: "
                            + id
            );
        }
    }

    /**
     * Adjust an anonymous population count.
     */
    private static <K> void putCount(
            Map<K, Long> counts,
            K key,
            long delta
    ) {

        long updated =
                Math.addExact(
                        counts.getOrDefault(
                                key,
                                0L
                        ),
                        delta
                );

        if (updated < 0L) {
            throw new IllegalArgumentException(
                    "Insufficient anonymous inhabitants"
            );
        }

        if (updated == 0L) {
            counts.remove(key);
        } else {
            counts.put(
                    key,
                    updated
            );
        }
    }

    /**
     * Transfer people between demographic cohorts.
     *
     * The source and destination maps can belong
     * to either the same location category or
     * different categories.
     *
     * IMPORTANT:
     * 'to' is the destination KEY.
     * 'target' is the destination COUNT.
     */
    private static <A, B> void transfer(
            Map<A, Long> fromMap,
            A from,
            Map<B, Long> toMap,
            B to,
            long people
    ) {

        long have =
                fromMap.getOrDefault(
                        from,
                        0L
                );

        if (have < people) {
            throw new IllegalArgumentException(
                    "Insufficient anonymous migrants"
            );
        }

        long target =
                Math.addExact(
                        toMap.getOrDefault(
                                to,
                                0L
                        ),
                        people
                );

        long remaining =
                have - people;

        if (remaining == 0L) {
            fromMap.remove(from);
        } else {
            fromMap.put(
                    from,
                    remaining
            );
        }

        // Correct destination key and count.
        toMap.put(to, target);
    }

    private static String requireId(
            String id
    ) {

        if (
                id == null
                        || !id.matches(
                        "[a-z0-9_.-]+"
                )
        ) {

            throw new IllegalArgumentException(
                    "Invalid population reference ID: "
                            + id
            );
        }

        return id;
    }
}