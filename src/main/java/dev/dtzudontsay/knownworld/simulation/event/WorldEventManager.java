package dev.dtzudontsay.knownworld.simulation.event;

import dev.dtzudontsay.knownworld.simulation.SimulationPosition;
import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Canonical manager for objective world events.
 *
 * World events are distinct from memories:
 *
 * WorldEvent:
 *     what objectively happened according to the simulation.
 *
 * NpcMemory:
 *     what one specific NPC remembers.
 *
 * ------------------------------------------------------------
 * SIM 09A - bounded detailed event journal
 * ------------------------------------------------------------
 *
 * An infinite detailed world-event list is not viable for a simulation
 * intended to support:
 *
 * - very large populations
 * - centuries or millennia of history
 * - accelerated simulation
 *
 * Therefore this manager maintains two bounded detailed tiers:
 *
 * ROUTINE
 *     Ordinary low/medium-importance social events.
 *
 * LANDMARK
 *     Structurally important events, non-social events, very important
 *     social events, and betrayals.
 *
 * ------------------------------------------------------------
 * SIM 09B.1 - compact historical chronicle
 * ------------------------------------------------------------
 *
 * Every newly created objective event is additionally recorded into the
 * HistoricalChronicleManager.
 *
 * The chronicle does not retain another copy of every WorldEvent.
 *
 * Instead it compresses history into constant-size aggregate statistics.
 *
 * This allows information about an arbitrarily long simulation to survive
 * conceptually even while detailed events are pruned.
 */
public final class WorldEventManager {

    /**
     * Maximum number of ordinary detailed world events retained.
     */
    public static final int MAX_ROUTINE_EVENTS =
            4_096;

    /**
     * Maximum number of historically important detailed events retained.
     */
    public static final int MAX_LANDMARK_EVENTS =
            4_096;

    /**
     * Absolute upper bound of detailed world events resident in this manager.
     */
    public static final int MAX_DETAILED_EVENTS =
            MAX_ROUTINE_EVENTS
                    +
                    MAX_LANDMARK_EVENTS;

    /**
     * A social event at or above this importance is promoted to the landmark
     * tier.
     */
    private static final double LANDMARK_SOCIAL_IMPORTANCE =
            0.80;

    private final NpcRegistry npcRegistry;

    /**
     * Compact long-term objective-history aggregate.
     */
    private final HistoricalChronicleManager chronicle =
            new HistoricalChronicleManager();

    /**
     * Retained detailed events.
     */
    private final Map<WorldEventId, WorldEvent> events =
            new LinkedHashMap<>();

    /**
     * IDs of retained routine events from oldest to newest.
     */
    private final Deque<WorldEventId> routineOrder =
            new ArrayDeque<>();

    /**
     * IDs of retained landmark events.
     */
    private final Deque<WorldEventId> landmarkOrder =
            new ArrayDeque<>();

    private long nextId =
            1L;

    private int routineCount =
            0;

    private int landmarkCount =
            0;

    /**
     * Number of detailed events discarded from the resident journal since
     * this manager instance was created.
     */
    private long discardedDetailedEvents =
            0L;

    public WorldEventManager(
            NpcRegistry npcRegistry
    ) {

        this.npcRegistry =
                Objects.requireNonNull(
                        npcRegistry,
                        "npcRegistry"
                );
    }

    public synchronized WorldEvent create(
            WorldEventType type,
            String summary,
            SimulationPosition position,
            long occurredTick,
            double importance,
            NpcId actorNpc,
            NpcId subjectNpc,
            String factKey,
            String factValue
    ) {

        validateNpcIfPresent(
                actorNpc
        );

        validateNpcIfPresent(
                subjectNpc
        );

        WorldEvent event =
                new WorldEvent(
                        allocateId(),
                        type,
                        summary,
                        position,
                        occurredTick,
                        importance,
                        actorNpc,
                        subjectNpc,
                        factKey,
                        factValue
                );

        /*
         * Long-term compressed history must see the event BEFORE detailed
         * retention is allowed to discard anything.
         */
        chronicle.record(
                event
        );

        registerRetainedEvent(
                event
        );

        enforceLimits();

        return event;
    }

    public synchronized void registerLoaded(
            WorldEvent event
    ) {

        Objects.requireNonNull(
                event,
                "event"
        );

        validateNpcIfPresent(
                event.actorNpc()
        );

        validateNpcIfPresent(
                event.subjectNpc()
        );

        if (events.containsKey(
                event.id()
        )) {

            throw new IllegalStateException(
                    "Duplicate world event ID: "
                            + event.id()
            );
        }

        /*
         * SIM 09B.1 migration behavior:
         *
         * Until dedicated chronicle persistence is installed in SIM 09B.2,
         * retained detailed events from an existing world reconstruct the
         * chronicle on load.
         *
         * Events which were already pruned before SIM 09B.1 obviously cannot
         * be reconstructed retrospectively.
         */
        chronicle.recordLoaded(
                event
        );

        /*
         * Advance the allocator even if this event is later removed by the
         * retention policy.
         */
        if (event.id()
                .value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {

            nextId =
                    Math.max(
                            nextId,
                            event.id()
                                    .value()
                                    + 1
                    );
        }

        registerRetainedEvent(
                event
        );

        enforceLimits();
    }

    public synchronized Optional<WorldEvent> find(
            WorldEventId id
    ) {

        Objects.requireNonNull(
                id,
                "id"
        );

        return Optional.ofNullable(
                events.get(
                        id
                )
        );
    }

    public synchronized Collection<WorldEvent> all() {

        return List.copyOf(
                events.values()
        );
    }

    public synchronized int size() {

        return events.size();
    }

    public synchronized int routineSize() {

        return routineCount;
    }

    public synchronized int landmarkSize() {

        return landmarkCount;
    }

    public synchronized long discardedDetailedEvents() {

        return discardedDetailedEvents;
    }

    public synchronized int maximumDetailedEvents() {

        return MAX_DETAILED_EVENTS;
    }

    public synchronized int maximumRoutineEvents() {

        return MAX_ROUTINE_EVENTS;
    }

    public synchronized int maximumLandmarkEvents() {

        return MAX_LANDMARK_EVENTS;
    }

    /**
     * Compact long-term historical aggregate.
     */
    public HistoricalChronicleManager chronicle() {

        return chronicle;
    }

    /**
     * Explicit maintenance hook for future migration/debug systems.
     */
    public synchronized void maintain() {

        enforceLimits();
    }

    /**
     * Returns true when an event belongs to the durable-detail tier.
     *
     * All non-social events are treated as landmarks because they generally
     * represent structural world history.
     *
     * Social events normally remain routine unless sufficiently important or
     * explicitly a betrayal.
     */
    public static boolean isLandmark(
            WorldEvent event
    ) {

        Objects.requireNonNull(
                event,
                "event"
        );

        if (event.type()
                != WorldEventType.SOCIAL) {

            return true;
        }

        if (event.importance()
                >= LANDMARK_SOCIAL_IMPORTANCE) {

            return true;
        }

        String factKey =
                event.factKey();

        return factKey != null
                &&
                (
                        factKey.equals(
                                "social.event.betray"
                        )
                                ||
                                factKey.equals(
                                        "social.action.betray"
                                )
                );
    }

    private void registerRetainedEvent(
            WorldEvent event
    ) {

        events.put(
                event.id(),
                event
        );

        if (isLandmark(
                event
        )) {

            landmarkOrder.addLast(
                    event.id()
            );

            landmarkCount++;

        } else {

            routineOrder.addLast(
                    event.id()
            );

            routineCount++;
        }
    }

    private void enforceLimits() {

        trimRoutineEvents();

        trimLandmarkEvents();

        while (events.size()
                > MAX_DETAILED_EVENTS) {

            if (!routineOrder.isEmpty()) {

                WorldEventId oldestRoutine =
                        routineOrder.pollFirst();

                removeRetainedEvent(
                        oldestRoutine,
                        false
                );

            } else {

                WorldEventId weakestLandmark =
                        weakestLandmarkId();

                if (weakestLandmark == null) {

                    break;
                }

                removeRetainedEvent(
                        weakestLandmark,
                        true
                );
            }
        }
    }

    private void trimRoutineEvents() {

        while (routineCount
                > MAX_ROUTINE_EVENTS) {

            WorldEventId oldest =
                    routineOrder.pollFirst();

            if (oldest == null) {

                routineCount =
                        countRoutineEvents();

                return;
            }

            removeRetainedEvent(
                    oldest,
                    false
            );
        }
    }

    private void trimLandmarkEvents() {

        while (landmarkCount
                > MAX_LANDMARK_EVENTS) {

            WorldEventId weakest =
                    weakestLandmarkId();

            if (weakest == null) {

                landmarkCount =
                        countLandmarkEvents();

                return;
            }

            landmarkOrder.remove(
                    weakest
            );

            removeRetainedEvent(
                    weakest,
                    true
            );
        }
    }

    private WorldEventId weakestLandmarkId() {

        WorldEvent weakest =
                null;

        for (
                WorldEventId id :
                landmarkOrder
        ) {

            WorldEvent candidate =
                    events.get(
                            id
                    );

            if (candidate == null) {

                continue;
            }

            if (weakest == null
                    ||
                    isWeakerLandmark(
                            candidate,
                            weakest
                    )) {

                weakest =
                        candidate;
            }
        }

        return weakest == null
                ? null
                : weakest.id();
    }

    private static boolean isWeakerLandmark(
            WorldEvent candidate,
            WorldEvent currentWeakest
    ) {

        int importanceComparison =
                Double.compare(
                        candidate.importance(),
                        currentWeakest.importance()
                );

        if (importanceComparison != 0) {

            return importanceComparison
                    < 0;
        }

        int tickComparison =
                Long.compare(
                        candidate.occurredTick(),
                        currentWeakest.occurredTick()
                );

        if (tickComparison != 0) {

            return tickComparison
                    < 0;
        }

        return candidate.id()
                .compareTo(
                        currentWeakest.id()
                )
                < 0;
    }

    private void removeRetainedEvent(
            WorldEventId id,
            boolean landmark
    ) {

        WorldEvent removed =
                events.remove(
                        id
                );

        if (removed == null) {

            return;
        }

        if (landmark) {

            landmarkCount =
                    Math.max(
                            0,
                            landmarkCount
                                    - 1
                    );

        } else {

            routineCount =
                    Math.max(
                            0,
                            routineCount
                                    - 1
                    );
        }

        discardedDetailedEvents++;
    }

    private int countRoutineEvents() {

        int count =
                0;

        for (
                WorldEvent event :
                events.values()
        ) {

            if (!isLandmark(
                    event
            )) {

                count++;
            }
        }

        return count;
    }

    private int countLandmarkEvents() {

        int count =
                0;

        for (
                WorldEvent event :
                events.values()
        ) {

            if (isLandmark(
                    event
            )) {

                count++;
            }
        }

        return count;
    }

    private WorldEventId allocateId() {

        if (nextId <= 0
                ||
                nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "World event ID space exhausted"
            );
        }

        WorldEventId id =
                new WorldEventId(
                        nextId
                );

        nextId++;

        return id;
    }

    private void validateNpcIfPresent(
            NpcId id
    ) {

        if (id != null
                &&
                !npcRegistry.contains(
                        id
                )) {

            throw new IllegalArgumentException(
                    "Unknown NPC ID: "
                            + id
            );
        }
    }

    public record Stats(
            int retained,
            int routine,
            int landmarks,
            int routineLimit,
            int landmarkLimit,
            int totalLimit,
            long discardedSinceStartup
    ) {
    }

    public synchronized Stats stats() {

        return new Stats(
                events.size(),
                routineCount,
                landmarkCount,
                MAX_ROUTINE_EVENTS,
                MAX_LANDMARK_EVENTS,
                MAX_DETAILED_EVENTS,
                discardedDetailedEvents
        );
    }
}