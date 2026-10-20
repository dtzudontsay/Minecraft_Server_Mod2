
package dev.dtzudontsay.knownworld.simulation.time;

/**
 * Persistent historical calendar.
 *
 * Two supported modes:
 *
 * LEGACY / STANDALONE:
 *   Advances every configured number of simulation ticks.
 *   Preserves compatibility with existing standalone tests
 *   and non-Known-World simulation environments.
 *
 * KNOWN WORLD / OVERWORLD CLOCK:
 *   A server-side bridge observes the real Minecraft
 *   overworld clock and advances historical dates.
 *   The ordinary advanceTick() call becomes a no-op
 *   to prevent duplicate daily processing.
 *
 * Existing campaign_calendar.tsv saves remain compatible.
 */
public final class CampaignCalendar {

    public static final int DEFAULT_DAYS_PER_YEAR = 365;
    public static final int DEFAULT_START_YEAR = 298;

    /**
     * Compatibility default for legacy tests and
     * non-Known-World simulation.
     */
    public static final int DEFAULT_TICKS_PER_CAMPAIGN_DAY = 1200;

    /**
     * New authoritative day duration inside Known World.
     */
    public static final int WORLD_TICKS_PER_CAMPAIGN_DAY = 24000;

    private final int daysPerYear;

    private long absoluteDay;
    private int tickRemainder;
    private int ticksPerCampaignDay;

    private boolean worldClockAuthority;

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

        this.daysPerYear = daysPerYear;

        this.absoluteDay =
                (long) startYear * daysPerYear
                        + startDayOfYear;

        this.ticksPerCampaignDay = ticksPerCampaignDay;
        this.tickRemainder = 0;
        this.worldClockAuthority = false;
    }

    /**
     * Tick-driven calendar advancement remains available
     * to standalone test harnesses and legacy simulation.
     *
     * A Known World uses its overworld clock instead.
     */
    public boolean advanceTick() {

        if (worldClockAuthority) {
            return false;
        }

        tickRemainder++;

        if (tickRemainder < ticksPerCampaignDay) {
            return false;
        }

        tickRemainder = 0;
        advanceOneDay();

        return true;
    }

    /**
     * Enable real Minecraft clock authority.
     *
     * Called once the existing saved campaign date
     * has been loaded.
     *
     * Never changes absoluteDay, year or dayOfYear.
     */
    public void enableWorldClockAuthority(long overworldTime) {

        worldClockAuthority = true;
        ticksPerCampaignDay = WORLD_TICKS_PER_CAMPAIGN_DAY;

        syncWorldClockPhase(overworldTime);
    }

    /**
     * Set the calendar's current phase according to
     * the Minecraft 26.3 overworld clock.
     */
    public void syncWorldClockPhase(long overworldTime) {

        if (!worldClockAuthority) {
            throw new IllegalStateException(
                    "World-clock authority is not enabled"
            );
        }

        tickRemainder = (int) Math.floorMod(
                overworldTime,
                WORLD_TICKS_PER_CAMPAIGN_DAY
        );
    }

    public boolean worldClockAuthority() {
        return worldClockAuthority;
    }

    /**
     * Authoritative advancement of one historical day.
     *
     * Caller must trigger relevant lifecycle operations.
     */
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

        if (worldClockAuthority
                && ticksPerCampaignDay
                != WORLD_TICKS_PER_CAMPAIGN_DAY) {

            throw new IllegalStateException(
                    "Cannot override Minecraft world-clock duration"
            );
        }

        this.ticksPerCampaignDay = ticksPerCampaignDay;

        if (tickRemainder >= ticksPerCampaignDay) {
            tickRemainder = 0;
        }
    }

    /**
     * Restore legacy version-one calendar saves unchanged.
     *
     * World-clock authority is enabled after restoration,
     * only if the save uses KnownWorldChunkGenerator.
     */
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

        this.absoluteDay = absoluteDay;
        this.tickRemainder = tickRemainder;
        this.ticksPerCampaignDay = ticksPerCampaignDay;
        this.worldClockAuthority = false;
    }
}
