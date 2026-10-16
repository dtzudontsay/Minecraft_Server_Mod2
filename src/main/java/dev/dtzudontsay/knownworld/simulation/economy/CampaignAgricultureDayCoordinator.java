
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.time.SimulationPhase;
import dev.dtzudontsay.knownworld.simulation.time.SimulationSchedule;
import dev.dtzudontsay.knownworld.simulation.time.SimulationTimeContract;

import java.util.Objects;

/**
 * Batch 22.10.
 *
 * Shared campaign-day coordinator for agriculture.
 *
 * Consumes immutable snapshots of the existing
 * authoritative simulation time.
 *
 * Does NOT own another clock.
 * Does NOT own population or inventories.
 * Does NOT create agricultural resources.
 *
 * Strict mode is for explicit future activation
 * after scenario and persistence prerequisites
 * have been validated.
 */
public final class CampaignAgricultureDayCoordinator {

    public enum Mode {
        MONITOR_ONLY,
        STRICT_EXECUTION
    }

    public enum Observation {
        SAME_DAY,
        RECORDED,
        RECORDED_WITH_GAP,
        EXECUTED
    }

    /**
     * Operation for one campaign-day window:
     *
     * [startDayInclusive, endDayExclusive)
     *
     * For normal progression, end = start + 1.
     *
     * The operation receives the authoritative
     * simulation tick at the day boundary.
     *
     * Implementations may later construct holding-
     * specific AgriculturalInterval objects.
     */
    @FunctionalInterface
    public interface DayOperation {

        void execute(
                long startDayInclusive,
                long endDayExclusive,
                long simulationTick
        );
    }

    private final Mode mode;

    private final SimulationSchedule schedule =
            SimulationSchedule.campaignDaily(
                    SimulationPhase.ECONOMY
            );

    private final DayOperation operation;

    private long lastObservedDay;

    private long observedTransitions;
    private long missedDays;
    private long executedDays;

    private boolean blocked;

    /**
     * Strict execution coordinator.
     *
     * No recovery or automatic retry is attempted
     * after a failed operation.
     */
    public CampaignAgricultureDayCoordinator(
            long initialCampaignDay,
            DayOperation operation
    ) {
        this(
                initialCampaignDay,
                Mode.STRICT_EXECUTION,
                Objects.requireNonNull(
                        operation, "operation"
                )
        );
    }

    /**
     * Passive monitoring for existing worlds.
     *
     * No production callback is installed.
     */
    public static CampaignAgricultureDayCoordinator
    monitorOnly(long initialCampaignDay) {

        return new CampaignAgricultureDayCoordinator(
                initialCampaignDay,
                Mode.MONITOR_ONLY,
                null
        );
    }

    private CampaignAgricultureDayCoordinator(
            long initialCampaignDay,
            Mode mode,
            DayOperation operation
    ) {
        if (initialCampaignDay < 0L) {
            throw new IllegalArgumentException(
                    "Initial campaign day cannot be negative"
            );
        }

        this.mode = Objects.requireNonNull(mode);
        this.operation = operation;
        this.lastObservedDay = initialCampaignDay;
    }

    public synchronized Mode mode() {
        return mode;
    }

    public SimulationSchedule schedule() {
        return schedule;
    }

    public synchronized long lastObservedDay() {
        return lastObservedDay;
    }

    public synchronized long observedTransitions() {
        return observedTransitions;
    }

    public synchronized long missedDays() {
        return missedDays;
    }

    public synchronized long executedDays() {
        return executedDays;
    }

    public synchronized boolean blocked() {
        return blocked;
    }

    /**
     * Observe authoritative campaign time.
     *
     * Both ordinary gameplay and accelerated
     * simulation should call this same method.
     *
     * In monitor mode, gaps are recorded but
     * no missing economic days are fabricated.
     *
     * In strict mode, only an exact one-day
     * transition may execute.
     */
    public synchronized Observation observe(
            SimulationTimeContract time
    ) {
        Objects.requireNonNull(time, "time");

        if (blocked) {
            throw new IllegalStateException(
                    "Agricultural day coordinator is blocked "
                            + "after an unsafe execution attempt"
            );
        }

        long currentDay =
                time.campaignAbsoluteDay();

        if (currentDay < lastObservedDay) {
            throw new IllegalStateException(
                    "Campaign time moved backwards: "
                            + currentDay
                            + " < "
                            + lastObservedDay
            );
        }

        if (currentDay == lastObservedDay) {
            return Observation.SAME_DAY;
        }

        long advancedDays =
                currentDay - lastObservedDay;

        /*
         * Passive monitoring can identify skipped
         * observations without trying to replay
         * economic activity.
         */
        if (mode == Mode.MONITOR_ONLY) {

            missedDays = Math.addExact(
                    missedDays,
                    advancedDays - 1L
            );

            observedTransitions = Math.addExact(
                    observedTransitions,
                    1L
            );

            lastObservedDay = currentDay;

            return advancedDays == 1L
                    ? Observation.RECORDED
                    : Observation.RECORDED_WITH_GAP;
        }

        /*
         * Autonomous agriculture must never
         * silently skip multiple campaign days.
         *
         * The current interval runner has durable
         * replay protection, not a general-purpose
         * catch-up/rollback engine.
         */
        if (advancedDays != 1L) {
            blocked = true;

            throw new IllegalStateException(
                    "Strict agriculture missed "
                            + advancedDays
                            + " campaign days; manual reconciliation "
                            + "is required"
            );
        }

        /*
         * Reuse the existing scheduling contract
         * and its designated ECONOMY phase.
         *
         * The day-advanced flag is true because
         * we observed an actual increment.
         */
        if (!schedule.isDue(time, true)) {
            blocked = true;

            throw new IllegalStateException(
                    "Economy schedule rejected "
                            + "a campaign-day boundary"
            );
        }

        long startDay = lastObservedDay;

        /*
         * If an operation throws, it may have
         * performed durable effects before failing.
         *
         * Do not retry it on the next tick.
         */
        try {
            operation.execute(
                    startDay,
                    currentDay,
                    time.simulationTick()
            );

        } catch (RuntimeException | Error exception) {
            blocked = true;
            throw exception;
        }

        lastObservedDay = currentDay;

        observedTransitions = Math.addExact(
                observedTransitions,
                1L
        );

        executedDays = Math.addExact(
                executedDays,
                1L
        );

        return Observation.EXECUTED;
    }
}
