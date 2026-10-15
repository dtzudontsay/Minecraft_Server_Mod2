
package dev.dtzudontsay.knownworld.simulation.economy;

/**
 * Half-open simulation-day interval:
 *
 * [startDayInclusive, endDayExclusive)
 *
 * Harvest boundaries occur at positive multiples
 * of harvestPeriodDays.
 *
 * This is independent of real-world clock time.
 */
public record AgriculturalInterval(
        long startDayInclusive,
        long endDayExclusive,
        long harvestPeriodDays
) {
    public AgriculturalInterval {
        if (startDayInclusive < 0
                || endDayExclusive <= startDayInclusive
                || harvestPeriodDays <= 0) {
            throw new IllegalArgumentException(
                    "Invalid agricultural interval"
            );
        }

        if (endDayExclusive - startDayInclusive
                > harvestPeriodDays) {
            throw new IllegalArgumentException(
                    "Interval exceeds one harvest period"
            );
        }

        if (harvestCount(
                startDayInclusive,
                endDayExclusive,
                harvestPeriodDays
        ) > 1L) {
            throw new IllegalArgumentException(
                    "Multiple harvest boundaries in one interval"
            );
        }
    }

    public long days() {
        return endDayExclusive - startDayInclusive;
    }

    /**
     * The harvest happens when the interval crosses
     * a positive multiple of the harvest period.
     *
     * For a 30-day period:
     * [0, 29)  -> no harvest
     * [29, 30) -> harvest
     * [30, 31) -> no harvest
     * [59, 60) -> harvest
     */
    public boolean harvestDue() {
        return harvestCount(
                startDayInclusive,
                endDayExclusive,
                harvestPeriodDays
        ) == 1L;
    }

    private static long harvestCount(
            long start,
            long end,
            long period
    ) {
        return end / period - start / period;
    }
}
