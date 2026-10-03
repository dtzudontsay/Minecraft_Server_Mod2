package dev.dtzudontsay.knownworld.simulation.event;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Compact long-term historical chronicle.
 *
 * WorldEventManager retains a bounded detailed event journal. This manager
 * retains compressed aggregate history for every objective event which passes
 * through the simulation.
 *
 * The primary aggregate is keyed by WorldEventType, a finite enum, so memory
 * use does not grow one object per historical event. Later systems can add
 * similarly bounded aggregates for wars, rulers, dynasties, economies and
 * other long-lived historical structures.
 */
public final class HistoricalChronicleManager {

    private static final double EPSILON =
            0.000_001;

    private final Map<WorldEventType, MutableTypeStats> byType =
            new EnumMap<>(
                    WorldEventType.class
            );

    private long totalEvents =
            0L;

    private long socialEvents =
            0L;

    private long structuralEvents =
            0L;

    private long eventsWithFacts =
            0L;

    private double totalImportance =
            0.0;

    private double maximumImportance =
            0.0;

    private long firstRecordedTick =
            -1L;

    private long lastRecordedTick =
            -1L;

    public HistoricalChronicleManager() {

        for (
                WorldEventType type :
                WorldEventType.values()
        ) {

            byType.put(
                    type,
                    new MutableTypeStats(
                            type
                    )
            );
        }
    }

    /**
     * Records one objective event into the compressed chronicle.
     *
     * The WorldEvent itself is not retained here.
     */
    public synchronized void record(
            WorldEvent event
    ) {

        Objects.requireNonNull(
                event,
                "event"
        );

        totalEvents++;

        totalImportance +=
                event.importance();

        maximumImportance =
                Math.max(
                        maximumImportance,
                        event.importance()
                );

        if (firstRecordedTick < 0L) {

            firstRecordedTick =
                    event.occurredTick();
        }

        lastRecordedTick =
                Math.max(
                        lastRecordedTick,
                        event.occurredTick()
                );

        if (event.type()
                == WorldEventType.SOCIAL) {

            socialEvents++;

        } else {

            structuralEvents++;
        }

        if (event.containsFact()) {

            eventsWithFacts++;
        }

        byType.get(
                        event.type()
                )
                .record(
                        event
                );
    }

    /**
     * Migration fallback used when loading an older world which does not yet
     * possess a dedicated persisted chronicle.
     *
     * In that case the bounded retained event journal reconstructs as much
     * historical information as is still available.
     */
    public synchronized void recordLoaded(
            WorldEvent event
    ) {

        record(
                event
        );
    }

    public synchronized long totalEvents() {

        return totalEvents;
    }

    public synchronized long socialEvents() {

        return socialEvents;
    }

    public synchronized long structuralEvents() {

        return structuralEvents;
    }

    public synchronized long eventsWithFacts() {

        return eventsWithFacts;
    }

    public synchronized double totalImportance() {

        return totalImportance;
    }

    public synchronized double averageImportance() {

        if (totalEvents == 0L) {

            return 0.0;
        }

        return totalImportance
                / totalEvents;
    }

    public synchronized double maximumImportance() {

        return maximumImportance;
    }

    public synchronized long firstRecordedTick() {

        return firstRecordedTick;
    }

    public synchronized long lastRecordedTick() {

        return lastRecordedTick;
    }

    public synchronized TypeStats statsFor(
            WorldEventType type
    ) {

        Objects.requireNonNull(
                type,
                "type"
        );

        return byType.get(
                        type
                )
                .snapshot();
    }

    public synchronized List<TypeStats> allTypeStats() {

        List<TypeStats> result =
                new ArrayList<>();

        for (
                WorldEventType type :
                WorldEventType.values()
        ) {

            result.add(
                    byType.get(
                                    type
                            )
                            .snapshot()
            );
        }

        return List.copyOf(
                result
        );
    }

    public synchronized ChronicleStats stats() {

        return new ChronicleStats(
                totalEvents,
                socialEvents,
                structuralEvents,
                eventsWithFacts,
                totalImportance,
                averageImportance(),
                maximumImportance,
                firstRecordedTick,
                lastRecordedTick
        );
    }

    /**
     * Complete immutable persistence snapshot.
     *
     * No individual WorldEvent objects are duplicated here.
     */
    public synchronized PersistedSnapshot snapshotForPersistence() {

        return new PersistedSnapshot(
                stats(),
                allTypeStats()
        );
    }

    /**
     * Replaces the current reconstructed/in-memory aggregate with a canonical
     * persisted chronicle.
     *
     * This is intentionally a full replacement rather than an additive merge:
     * retained detailed events are only a migration fallback, while a persisted
     * chronicle represents the complete compressed history known to the save.
     */
    public synchronized void restore(
            PersistedSnapshot snapshot
    ) {

        Objects.requireNonNull(
                snapshot,
                "snapshot"
        );

        validateSnapshot(
                snapshot
        );

        ChronicleStats global =
                snapshot.global();

        totalEvents =
                global.totalEvents();

        socialEvents =
                global.socialEvents();

        structuralEvents =
                global.structuralEvents();

        eventsWithFacts =
                global.eventsWithFacts();

        totalImportance =
                global.totalImportance();

        maximumImportance =
                global.maximumImportance();

        firstRecordedTick =
                global.firstRecordedTick();

        lastRecordedTick =
                global.lastRecordedTick();

        for (
                MutableTypeStats stats :
                byType.values()
        ) {

            stats.clear();
        }

        for (
                TypeStats typeStats :
                snapshot.types()
        ) {

            MutableTypeStats target =
                    byType.get(
                            typeStats.type()
                    );

            if (target != null) {

                target.restore(
                        typeStats
                );
            }
        }
    }

    /**
     * Validates both local field ranges and the cross-record invariants which
     * make the persisted aggregate trustworthy.
     */
    public static void validateSnapshot(
            PersistedSnapshot snapshot
    ) {

        Objects.requireNonNull(
                snapshot,
                "snapshot"
        );

        ChronicleStats global =
                Objects.requireNonNull(
                        snapshot.global(),
                        "snapshot.global"
                );

        validateGlobal(
                global
        );

        Set<WorldEventType> seen =
                new HashSet<>();

        long countSum =
                0L;

        long factSum =
                0L;

        long socialCount =
                0L;

        double importanceSum =
                0.0;

        double maximum =
                0.0;

        long earliest =
                -1L;

        long latest =
                -1L;

        for (
                TypeStats typeStats :
                snapshot.types()
        ) {

            validateType(
                    typeStats
            );

            if (!seen.add(
                    typeStats.type()
            )) {

                throw new IllegalArgumentException(
                        "Duplicate chronicle type stats: "
                                + typeStats.type()
                );
            }

            countSum +=
                    typeStats.count();

            factSum +=
                    typeStats.factCount();

            importanceSum +=
                    typeStats.totalImportance();

            maximum =
                    Math.max(
                            maximum,
                            typeStats.maximumImportance()
                    );

            if (typeStats.type()
                    == WorldEventType.SOCIAL) {

                socialCount =
                        typeStats.count();
            }

            if (typeStats.count()
                    > 0L) {

                if (earliest < 0L
                        || typeStats.firstTick()
                        < earliest) {

                    earliest =
                            typeStats.firstTick();
                }

                latest =
                        Math.max(
                                latest,
                                typeStats.lastTick()
                        );
            }
        }

        if (countSum
                != global.totalEvents()) {

            throw new IllegalArgumentException(
                    "Chronicle total event count does not match type totals"
            );
        }

        if (factSum
                != global.eventsWithFacts()) {

            throw new IllegalArgumentException(
                    "Chronicle fact count does not match type totals"
            );
        }

        if (socialCount
                != global.socialEvents()) {

            throw new IllegalArgumentException(
                    "Chronicle social event count does not match SOCIAL type count"
            );
        }

        if (global.structuralEvents()
                != global.totalEvents()
                - global.socialEvents()) {

            throw new IllegalArgumentException(
                    "Chronicle structural event count is inconsistent"
            );
        }

        if (!approximatelyEqual(
                importanceSum,
                global.totalImportance()
        )) {

            throw new IllegalArgumentException(
                    "Chronicle importance total does not match type totals"
            );
        }

        if (!approximatelyEqual(
                maximum,
                global.maximumImportance()
        )) {

            throw new IllegalArgumentException(
                    "Chronicle maximum importance does not match type totals"
            );
        }

        if (earliest
                != global.firstRecordedTick()) {

            throw new IllegalArgumentException(
                    "Chronicle first tick does not match type totals"
            );
        }

        if (latest
                != global.lastRecordedTick()) {

            throw new IllegalArgumentException(
                    "Chronicle last tick does not match type totals"
            );
        }
    }

    private static void validateGlobal(
            ChronicleStats stats
    ) {

        if (stats.totalEvents()
                < 0L
                || stats.socialEvents()
                < 0L
                || stats.structuralEvents()
                < 0L
                || stats.eventsWithFacts()
                < 0L) {

            throw new IllegalArgumentException(
                    "Chronicle counts cannot be negative"
            );
        }

        if (stats.socialEvents()
                > stats.totalEvents()
                || stats.structuralEvents()
                > stats.totalEvents()
                || stats.eventsWithFacts()
                > stats.totalEvents()) {

            throw new IllegalArgumentException(
                    "Chronicle counts exceed totalEvents"
            );
        }

        validateImportanceAggregate(
                stats.totalImportance(),
                stats.maximumImportance()
        );

        validateTicks(
                stats.totalEvents(),
                stats.firstRecordedTick(),
                stats.lastRecordedTick()
        );
    }

    private static void validateType(
            TypeStats stats
    ) {

        Objects.requireNonNull(
                stats,
                "typeStats"
        );

        Objects.requireNonNull(
                stats.type(),
                "typeStats.type"
        );

        if (stats.count()
                < 0L
                || stats.factCount()
                < 0L
                || stats.factCount()
                > stats.count()) {

            throw new IllegalArgumentException(
                    "Invalid chronicle type counts for "
                            + stats.type()
            );
        }

        validateImportanceAggregate(
                stats.totalImportance(),
                stats.maximumImportance()
        );

        validateTicks(
                stats.count(),
                stats.firstTick(),
                stats.lastTick()
        );
    }

    private static void validateImportanceAggregate(
            double total,
            double maximum
    ) {

        if (!Double.isFinite(
                total
        )
                || total < 0.0) {

            throw new IllegalArgumentException(
                    "Chronicle total importance must be finite and non-negative"
            );
        }

        if (!Double.isFinite(
                maximum
        )
                || maximum < 0.0
                || maximum > 1.0) {

            throw new IllegalArgumentException(
                    "Chronicle maximum importance must be between 0.0 and 1.0"
            );
        }
    }

    private static void validateTicks(
            long count,
            long first,
            long last
    ) {

        if (count == 0L) {

            if (first != -1L
                    || last != -1L) {

                throw new IllegalArgumentException(
                        "Empty chronicle stats must use -1 tick sentinels"
                );
            }

            return;
        }

        if (first < 0L
                || last < 0L
                || last < first) {

            throw new IllegalArgumentException(
                    "Invalid chronicle tick range"
            );
        }
    }

    private static boolean approximatelyEqual(
            double a,
            double b
    ) {

        double scale =
                Math.max(
                        1.0,
                        Math.max(
                                Math.abs(
                                        a
                                ),
                                Math.abs(
                                        b
                                )
                        )
                );

        return Math.abs(
                a - b
        )
                <= EPSILON
                * scale;
    }

    /**
     * Immutable public view of one event-type aggregate.
     */
    public record TypeStats(
            WorldEventType type,
            long count,
            long factCount,
            double totalImportance,
            double averageImportance,
            double maximumImportance,
            long firstTick,
            long lastTick
    ) {
    }

    /**
     * Immutable public view of the complete compact chronicle.
     */
    public record ChronicleStats(
            long totalEvents,
            long socialEvents,
            long structuralEvents,
            long eventsWithFacts,
            double totalImportance,
            double averageImportance,
            double maximumImportance,
            long firstRecordedTick,
            long lastRecordedTick
    ) {
    }

    /**
     * Exact persistence payload for the aggregate chronicle.
     */
    public record PersistedSnapshot(
            ChronicleStats global,
            List<TypeStats> types
    ) {

        public PersistedSnapshot {

            Objects.requireNonNull(
                    global,
                    "global"
            );

            types =
                    List.copyOf(
                            Objects.requireNonNull(
                                    types,
                                    "types"
                            )
                    );
        }
    }

    private static final class MutableTypeStats {

        private final WorldEventType type;

        private long count =
                0L;

        private long factCount =
                0L;

        private double totalImportance =
                0.0;

        private double maximumImportance =
                0.0;

        private long firstTick =
                -1L;

        private long lastTick =
                -1L;

        private MutableTypeStats(
                WorldEventType type
        ) {

            this.type =
                    Objects.requireNonNull(
                            type,
                            "type"
                    );
        }

        private void record(
                WorldEvent event
        ) {

            count++;

            if (event.containsFact()) {

                factCount++;
            }

            totalImportance +=
                    event.importance();

            maximumImportance =
                    Math.max(
                            maximumImportance,
                            event.importance()
                    );

            if (firstTick < 0L) {

                firstTick =
                        event.occurredTick();
            }

            lastTick =
                    Math.max(
                            lastTick,
                            event.occurredTick()
                    );
        }

        private void clear() {

            count =
                    0L;

            factCount =
                    0L;

            totalImportance =
                    0.0;

            maximumImportance =
                    0.0;

            firstTick =
                    -1L;

            lastTick =
                    -1L;
        }

        private void restore(
                TypeStats stats
        ) {

            clear();

            count =
                    stats.count();

            factCount =
                    stats.factCount();

            totalImportance =
                    stats.totalImportance();

            maximumImportance =
                    stats.maximumImportance();

            firstTick =
                    stats.firstTick();

            lastTick =
                    stats.lastTick();
        }

        private TypeStats snapshot() {

            double average =
                    count == 0L
                            ? 0.0
                            : totalImportance
                            / count;

            return new TypeStats(
                    type,
                    count,
                    factCount,
                    totalImportance,
                    average,
                    maximumImportance,
                    firstTick,
                    lastTick
            );
        }
    }
}
