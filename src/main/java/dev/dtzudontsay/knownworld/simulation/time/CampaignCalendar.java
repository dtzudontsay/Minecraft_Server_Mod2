package dev.dtzudontsay.knownworld.simulation.time;

/**
 * Historical / campaign-scale calendar.
 *
 * This is intentionally separate from Minecraft's ordinary
 * 24,000-tick routine clock.
 *
 * NPC daily routines continue to operate on the normal simulation
 * tick/day cycle while aging, pregnancy, fertility and generations
 * operate on this accelerated campaign calendar.
 */
public final class CampaignCalendar {

    public static final int DEFAULT_DAYS_PER_YEAR =
            365;

    public static final int DEFAULT_START_YEAR =
            298;

    public static final int DEFAULT_TICKS_PER_CAMPAIGN_DAY =
            1200;

    private final int daysPerYear;

    private long absoluteDay;

    private int tickRemainder;

    private int ticksPerCampaignDay;

    public CampaignCalendar() {
        this(
                DEFAULT_START_YEAR,
                0,
                DEFAULT_DAYS_PER_YEAR,
                DEFAULT_TICKS_PER_CAMPAIGN_DAY
        );
    }

    public CampaignCalendar(
            int startYear,
            int startDayOfYear,
            int daysPerYear,
            int ticksPerCampaignDay
    ) {
        if (daysPerYear <= 0) {
            throw new IllegalArgumentException(
                    "daysPerYear must be positive"
            );
        }

        if (startDayOfYear < 0
                || startDayOfYear >= daysPerYear) {

            throw new IllegalArgumentException(
                    "startDayOfYear out of range"
            );
        }

        if (ticksPerCampaignDay <= 0) {
            throw new IllegalArgumentException(
                    "ticksPerCampaignDay must be positive"
            );
        }

        this.daysPerYear =
                daysPerYear;

        this.absoluteDay =
                ((long) startYear)
                        * daysPerYear
                        + startDayOfYear;

        this.ticksPerCampaignDay =
                ticksPerCampaignDay;

        this.tickRemainder =
                0;
    }

    /**
     * Advances one Minecraft/server simulation tick.
     *
     * @return true when a new campaign day begins.
     */
    public boolean advanceTick() {

        tickRemainder++;

        if (tickRemainder
                < ticksPerCampaignDay) {

            return false;
        }

        tickRemainder =
                0;

        advanceOneDay();

        return true;
    }

    public void advanceOneDay() {

        if (absoluteDay == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Campaign calendar exhausted"
            );
        }

        absoluteDay++;
    }

    public long absoluteDay() {
        return absoluteDay;
    }

    public int year() {
        return (int) Math.floorDiv(
                absoluteDay,
                daysPerYear
        );
    }

    public int dayOfYear() {
        return (int) Math.floorMod(
                absoluteDay,
                daysPerYear
        );
    }

    public int daysPerYear() {
        return daysPerYear;
    }

    public int ticksPerCampaignDay() {
        return ticksPerCampaignDay;
    }

    public int tickRemainder() {
        return tickRemainder;
    }

    public void setTicksPerCampaignDay(
            int ticksPerCampaignDay
    ) {
        if (ticksPerCampaignDay <= 0) {

            throw new IllegalArgumentException(
                    "ticksPerCampaignDay must be positive"
            );
        }

        this.ticksPerCampaignDay =
                ticksPerCampaignDay;

        if (tickRemainder
                >= ticksPerCampaignDay) {

            tickRemainder =
                    0;
        }
    }

    public void restore(
            long absoluteDay,
            int tickRemainder,
            int ticksPerCampaignDay
    ) {
        if (absoluteDay < 0) {

            throw new IllegalArgumentException(
                    "absoluteDay cannot be negative"
            );
        }

        if (ticksPerCampaignDay <= 0) {

            throw new IllegalArgumentException(
                    "ticksPerCampaignDay must be positive"
            );
        }

        if (tickRemainder < 0
                || tickRemainder >= ticksPerCampaignDay) {

            throw new IllegalArgumentException(
                    "tickRemainder out of range"
            );
        }

        this.absoluteDay =
                absoluteDay;

        this.tickRemainder =
                tickRemainder;

        this.ticksPerCampaignDay =
                ticksPerCampaignDay;
    }
}