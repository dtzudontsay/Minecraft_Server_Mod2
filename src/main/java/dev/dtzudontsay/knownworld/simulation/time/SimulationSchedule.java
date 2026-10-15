
package dev.dtzudontsay.knownworld.simulation.time;

import java.util.Objects;

/**
 * Batch 20.1
 *
 * Immutable scheduling policy.
 *
 * Determines whether a simulation system should execute
 * at a given scheduling boundary.
 *
 * Does not contain Minecraft-specific dependencies.
 *
 * Does not execute simulation actions.
 */
public final class SimulationSchedule {

    public enum Frequency {

        /**
         * Every simulation tick.
         */
        EVERY_TICK,

        /**
         * Every N simulation ticks.
         */
        TICK_INTERVAL,

        /**
         * Once when the campaign day advances.
         */
        CAMPAIGN_DAILY,

        /**
         * Once at every Nth campaign-day boundary,
         * measured from absolute day zero.
         */
        CAMPAIGN_DAY_INTERVAL,

        /**
         * Once when a new campaign year starts.
         */
        CAMPAIGN_YEARLY
    }

    private final SimulationPhase phase;

    private final Frequency frequency;

    private final long interval;

    private SimulationSchedule(
            SimulationPhase phase,
            Frequency frequency,
            long interval
    ) {

        this.phase = Objects.requireNonNull(
                phase,
                "phase"
        );

        this.frequency = Objects.requireNonNull(
                frequency,
                "frequency"
        );

        if (interval <= 0L) {
            throw new IllegalArgumentException(
                    "interval must be positive"
            );
        }

        this.interval = interval;
    }

    public static SimulationSchedule everyTick(
            SimulationPhase phase
    ) {

        return new SimulationSchedule(
                phase,
                Frequency.EVERY_TICK,
                1L
        );
    }

    public static SimulationSchedule everyTicks(
            SimulationPhase phase,
            long interval
    ) {

        return new SimulationSchedule(
                phase,
                Frequency.TICK_INTERVAL,
                interval
        );
    }

    public static SimulationSchedule campaignDaily(
            SimulationPhase phase
    ) {

        return new SimulationSchedule(
                phase,
                Frequency.CAMPAIGN_DAILY,
                1L
        );
    }

    public static SimulationSchedule everyCampaignDays(
            SimulationPhase phase,
            long interval
    ) {

        return new SimulationSchedule(
                phase,
                Frequency.CAMPAIGN_DAY_INTERVAL,
                interval
        );
    }

    public static SimulationSchedule campaignYearly(
            SimulationPhase phase
    ) {

        return new SimulationSchedule(
                phase,
                Frequency.CAMPAIGN_YEARLY,
                1L
        );
    }

    /**
     * Evaluate whether this schedule is due.
     *
     * campaignDayAdvanced must be provided by the
     * authoritative calendar advancement operation.
     *
     * It cannot always be inferred from a snapshot:
     * at tick zero, the initial calendar may already
     * be positioned at a day boundary.
     */
    public boolean isDue(
            SimulationTimeContract time,
            boolean campaignDayAdvanced
    ) {

        Objects.requireNonNull(
                time,
                "time"
        );

        return switch (frequency) {

            case EVERY_TICK ->
                    true;

            case TICK_INTERVAL ->
                    time.simulationTick() > 0L
                            && time.simulationTick()
                            % interval == 0L;

            case CAMPAIGN_DAILY ->
                    campaignDayAdvanced;

            case CAMPAIGN_DAY_INTERVAL ->
                    campaignDayAdvanced
                            && time.campaignAbsoluteDay()
                            % interval == 0L;

            case CAMPAIGN_YEARLY ->
                    campaignDayAdvanced
                            && time.campaignDayOfYear()
                            == 0;
        };
    }

    public SimulationPhase phase() {
        return phase;
    }

    public Frequency frequency() {
        return frequency;
    }

    public long interval() {
        return interval;
    }

    @Override
    public String toString() {

        return "SimulationSchedule{"
                + "phase=" + phase
                + ", frequency=" + frequency
                + ", interval=" + interval
                + '}';
    }
}
