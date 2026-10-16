
package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.npc.NpcSex;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Batch 22.17: anonymous residents only. Existing named/materialized people
 * remain exclusively in NpcRegistry and must never be inserted as cohorts.
 *
 * Counts are actual people, not the normalized populationWeight on holdings.
 * Cohorts have no new identities, relationships or NPC state. Future
 * materialization must atomically decrement a cohort while creating an NPC.
 */
public final class PopulationCohortManager {
    public enum AgeBand { CHILD, ADOLESCENT, ADULT, ELDER }
    public enum Basis { MOD_ESTIMATE, RESEARCHED_ESTIMATE, USER_AUTHORED }

    public record Key(SettlementId settlement, AgeBand age,
                      NpcSex sex, String cultureId, String religionId,
                      String socialClassId, String occupationId)
            implements Comparable<Key> {
        public Key {
            Objects.requireNonNull(settlement, "settlement");
            Objects.requireNonNull(age, "age");
            Objects.requireNonNull(sex, "sex");
            cultureId = requireId(cultureId);
            religionId = requireId(religionId);
            socialClassId = requireId(socialClassId);
            occupationId = requireId(occupationId);
        }

        @Override public int compareTo(Key other) {
            int c = Long.compare(settlement.value(), other.settlement.value());
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
            return c != 0 ? c : occupationId.compareTo(other.occupationId);
        }

        public Key at(SettlementId destination) {
            return new Key(destination, age, sex, cultureId, religionId,
                    socialClassId, occupationId);
        }
    }

    public record Cohort(Key key, long people) {
        public Cohort {
            Objects.requireNonNull(key, "key");
            if (people <= 0) throw new IllegalArgumentException("Cohort must contain people");
        }
    }

    public record Snapshot(Map<SettlementId, Basis> surveyed,
                           List<Cohort> cohorts) {
        public Snapshot {
            surveyed = Map.copyOf(surveyed);
            cohorts = List.copyOf(cohorts);
        }
    }

    private final SettlementManager settlements;
    private final TreeMap<Key, Long> counts = new TreeMap<>();
    private final Map<SettlementId, Basis> surveyed = new HashMap<>();

    public PopulationCohortManager(SettlementManager settlements) {
        this.settlements = Objects.requireNonNull(settlements, "settlements");
    }

    /** Explicitly records coverage, including settlements with zero anonymous residents. */
    public synchronized void markSurveyed(SettlementId settlement, Basis basis) {
        checkSettlement(settlement);
        Objects.requireNonNull(basis, "basis");
        Basis previous = surveyed.putIfAbsent(settlement, basis);
        if (previous != null && previous != basis) {
            throw new IllegalStateException("Population source already established for " + settlement);
        }
    }

    public synchronized boolean surveyed(SettlementId settlement) {
        return surveyed.containsKey(settlement);
    }

    public synchronized long count(Key key) {
        Objects.requireNonNull(key, "key");
        return counts.getOrDefault(key, 0L);
    }

    /** Explicit birth, death, employment or other cohort change; never time-triggered here. */
    public synchronized void change(Key key, long delta) {
        requireSurveyed(key.settlement());
        long before = count(key);
        long after = Math.addExact(before, delta);
        if (after < 0L) throw new IllegalArgumentException("Insufficient anonymous population");
        if (after == 0L) counts.remove(key);
        else counts.put(key, after);
    }

    /** Transfers anonymous inhabitants without adding or deleting people. */
    public synchronized void migrate(Key origin, SettlementId destination, long people) {
        Objects.requireNonNull(origin, "origin");
        requireSurveyed(origin.settlement());
        requireSurveyed(destination);
        if (people < 0L) throw new IllegalArgumentException("Negative migration");
        if (people == 0L || origin.settlement().equals(destination)) return;
        Key target = origin.at(destination);
        if (count(origin) < people) throw new IllegalArgumentException("Insufficient migrants");
        long destinationCount = Math.addExact(count(target), people);
        long sourceCount = count(origin) - people;
        if (sourceCount == 0L) counts.remove(origin);
        else counts.put(origin, sourceCount);
        counts.put(target, destinationCount);
    }

    public synchronized Snapshot snapshot() {
        List<Cohort> rows = new ArrayList<>();
        counts.forEach((key, count) -> rows.add(new Cohort(key, count)));
        return new Snapshot(surveyed, rows);
    }

    /** Validates a complete loaded checkpoint before changing the live manager. */
    public synchronized void replaceAll(Snapshot state) {
        Objects.requireNonNull(state, "state");
        Map<SettlementId, Basis> newSurveyed = new HashMap<>();
        for (var entry : state.surveyed().entrySet()) {
            checkSettlement(entry.getKey());
            newSurveyed.put(entry.getKey(), Objects.requireNonNull(entry.getValue()));
        }
        TreeMap<Key, Long> newCounts = new TreeMap<>();
        for (Cohort row : state.cohorts()) {
            checkSettlement(row.key().settlement());
            if (!newSurveyed.containsKey(row.key().settlement())
                    || newCounts.putIfAbsent(row.key(), row.people()) != null) {
                throw new IllegalArgumentException("Unsurveyed or duplicate cohort");
            }
        }
        counts.clear();
        counts.putAll(newCounts);
        surveyed.clear();
        surveyed.putAll(newSurveyed);
    }

    private void requireSurveyed(SettlementId id) {
        checkSettlement(id);
        if (!surveyed(id)) throw new IllegalStateException("Unsurveyed settlement: " + id);
    }

    private void checkSettlement(SettlementId settlement) {
        Objects.requireNonNull(settlement, "settlement");
        if (settlements.find(settlement).isEmpty()) {
            throw new IllegalArgumentException("Unknown settlement: " + settlement);
        }
    }

    private static String requireId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Use a nonempty reference ID: " + id);
        }
        return id;
    }
}
