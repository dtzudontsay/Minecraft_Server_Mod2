
package dev.dtzudontsay.knownworld.simulation.time;

import java.util.Objects;

/**
 * Batch 20.1
 *
 * Immutable observation of the existing simulation time.
 *
 * This class does not advance either clock.
 *
 * The current architecture maintains two time scales:
 *
 * 1. SimulationClock:
 *    Short-term routines and simulation ticks.
 *
 * 2. CampaignCalendar:
 *    Historical ageing and generational progression.
 *
 * No assumption is made that one campaign day equals
 * one routine day.
 */
public record SimulationTimeContract(
        long simulationTick,
        long routineDay,
        int routineTickOfDay,
        long campaignAbsoluteDay,
        int campaignYear,
        int campaignDayOfYear,
        int daysPerCampaignYear,
        int ticksPerCampaignDay,
        int campaignTickRemainder
) {

    public SimulationTimeContract {

        if (simulationTick < 0L) {
            throw new IllegalArgumentException(
                    "simulationTick cannot be negative"
            );
        }

        if (routineDay < 0L) {
            throw new IllegalArgumentException(
                    "routineDay cannot be negative"
            );
        }

        if (routineTickOfDay < 0
                || routineTickOfDay
                >= SimulationClock.TICKS_PER_DAY) {

            throw new IllegalArgumentException(
                    "routineTickOfDay out of range"
            );
        }

        if (campaignAbsoluteDay < 0L) {
            throw new IllegalArgumentException(
                    "campaignAbsoluteDay cannot be negative"
            );
        }

        if (daysPerCampaignYear <= 0) {
            throw new IllegalArgumentException(
                    "daysPerCampaignYear must be positive"
            );
        }

        if (campaignDayOfYear < 0
                || campaignDayOfYear >= daysPerCampaignYear) {

            throw new IllegalArgumentException(
                    "campaignDayOfYear out of range"
            );
        }

        if (ticksPerCampaignDay <= 0) {
            throw new IllegalArgumentException(
                    "ticksPerCampaignDay must be positive"
            );
        }

        if (campaignTickRemainder < 0
                || campaignTickRemainder >= ticksPerCampaignDay) {

            throw new IllegalArgumentException(
                    "campaignTickRemainder out of range"
            );
        }

        if (routineDay
                != simulationTick
                / SimulationClock.TICKS_PER_DAY) {

            throw new IllegalArgumentException(
                    "Routine day inconsistent with simulation tick"
            );
        }

        if (routineTickOfDay
                != simulationTick
                % SimulationClock.TICKS_PER_DAY) {

            throw new IllegalArgumentException(
                    "Routine tick-of-day inconsistent"
            );
        }

        if (campaignAbsoluteDay
                / daysPerCampaignYear
                != campaignYear) {

            throw new IllegalArgumentException(
                    "Campaign year inconsistent"
            );
        }

        if (campaignAbsoluteDay
                % daysPerCampaignYear
                != campaignDayOfYear) {

            throw new IllegalArgumentException(
                    "Campaign day-of-year inconsistent"
            );
        }
    }

    /**
     * Capture current state without modifying either
     * clock.
     */
    public static SimulationTimeContract capture(
            SimulationClock clock,
            CampaignCalendar calendar
    ) {

        Objects.requireNonNull(
                clock,
                "clock"
        );

        Objects.requireNonNull(
                calendar,
                "calendar"
        );

        return new SimulationTimeContract(
                clock.tick(),
                clock.day(),
                clock.tickOfDay(),
                calendar.absoluteDay(),
                calendar.year(),
                calendar.dayOfYear(),
                calendar.daysPerYear(),
                calendar.ticksPerCampaignDay(),
                calendar.tickRemainder()
        );
    }

    /**
     * Returns the number of campaign days represented
     * by one routine day at the current configuration.
     *
     * This is a ratio, not a synchronization guarantee.
     */
    public double campaignDaysPerRoutineDay() {

        return (double) SimulationClock.TICKS_PER_DAY
                / ticksPerCampaignDay;
    }

    /**
     * Campaign-day duration expressed in simulated ticks.
     */
    public long campaignDayDurationTicks() {

        return ticksPerCampaignDay;
    }

    /**
     * Returns true when the current campaign time
     * is exactly at a campaign-day boundary.
     */
    public boolean atCampaignDayBoundary() {

        return campaignTickRemainder == 0;
    }

    /**
     * Returns the campaign-year position in the
     * existing numeric calendar representation.
     *
     * This is not a historical date formatter.
     */
    public String campaignDateLabel() {

        return campaignYear
                + " AC, day "
                + (campaignDayOfYear + 1);
    }
}
