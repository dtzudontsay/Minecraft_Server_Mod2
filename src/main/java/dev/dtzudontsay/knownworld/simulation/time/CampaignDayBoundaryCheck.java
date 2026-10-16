
package dev.dtzudontsay.knownworld.simulation.time;

import java.util.Objects;

/**
 * Batch 22.14.
 *
 * Read-only verification of a requested historical
 * campaign-day operation against the authoritative
 * SimulationTimeContract.
 *
 * This class does not advance a clock, replay days,
 * or modify historical calendar state.
 *
 * Requested intervals are half-open:
 *
 * [startDayInclusive, endDayExclusive)
 *
 * A daily operation is eligible only at the exact
 * completed-day boundary.
 */
public final class CampaignDayBoundaryCheck {

    public enum Status {
        ALIGNED,
        INVALID_WINDOW,
        WRONG_CAMPAIGN_DAY,
        NOT_AT_DAY_BOUNDARY
    }

    public record Result(
            Status status,
            long requestedStartDay,
            long requestedEndDay,
            long observedCampaignDay
    ) {
        public Result {
            Objects.requireNonNull(status, "status");
        }

        public boolean aligned() {
            return status == Status.ALIGNED;
        }
    }

    private CampaignDayBoundaryCheck() {
    }

    /**
     * Verify that exactly one campaign day has
     * completed and that the observed calendar
     * equals the requested interval end.
     *
     * SimulationClock's routine day is intentionally
     * not compared with the campaign day because
     * they use different time scales.
     */
    public static Result check(
            SimulationTimeContract time,
            long startDayInclusive,
            long endDayExclusive
    ) {
        Objects.requireNonNull(time, "time");

        Status status;

        if (startDayInclusive < 0L
                || endDayExclusive <= startDayInclusive
                || endDayExclusive
                - startDayInclusive != 1L) {

            status = Status.INVALID_WINDOW;

        } else if (time.campaignAbsoluteDay()
                != endDayExclusive) {

            status = Status.WRONG_CAMPAIGN_DAY;

        } else if (!time.atCampaignDayBoundary()) {

            status = Status.NOT_AT_DAY_BOUNDARY;

        } else {

            status = Status.ALIGNED;
        }

        return new Result(
                status,
                startDayInclusive,
                endDayExclusive,
                time.campaignAbsoluteDay()
        );
    }
}
