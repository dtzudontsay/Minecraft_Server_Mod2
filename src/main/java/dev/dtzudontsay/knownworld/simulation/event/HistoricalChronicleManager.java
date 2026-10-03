package dev.dtzudontsay.knownworld.simulation.event;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compact long-term historical chronicle.
 *
 * ------------------------------------------------------------
 * PURPOSE
 * ------------------------------------------------------------
 *
 * WorldEventManager retains a bounded amount of detailed event history.
 *
 * HistoricalChronicleManager instead retains compact aggregate information
 * about ALL events which pass through the simulation.
 *
 * This gives us two different historical layers:
 *
 * 1. Detailed journal
 *      Recent and/or especially important concrete WorldEvent records.
 *
 * 2. Chronicle
 *      Long-term compressed historical statistics which can survive
 *      arbitrarily long simulations without growing one object per event.
 *
 * ------------------------------------------------------------
 * SCALABILITY
 * ------------------------------------------------------------
 *
 * The primary chronicle structure is keyed by WorldEventType.
 *
 * WorldEventType is a finite enum, so memory use is effectively constant
 * whether the simulation runs for:
 *
 * - 30 days
 * - 100 years
 * - 10,000 years
 *
 * We deliberately DO NOT place one Chronicle object into memory for every
 * event.
 *
 * Later chronicle layers can add similarly bounded/aggregated structures for
 * wars, dynasties, rulers, settlements, economies, religions and political
 * eras without returning to an unlimited event log.
 */
public final class HistoricalChronicleManager {

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
     * This operation does not retain the WorldEvent itself.
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

        MutableTypeStats stats =
                byType.get(
                        event.type()
                );

        stats.record(
                event
        );
    }

    /**
     * Used when reconstructing a chronicle from retained detailed events in an
     * older world.
     *
     * For SIM 09B.1 this is intentionally identical to record().
     *
     * Once the dedicated chronicle persistence format arrives in SIM 09B.2,
     * persisted chronicle totals will take precedence and old retained events
     * will only be used as a migration fallback.
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