
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.simulation.time.SimulationClock;
import dev.dtzudontsay.knownworld.simulation.time.SimulationPhase;
import dev.dtzudontsay.knownworld.simulation.time.SimulationSchedule;
import dev.dtzudontsay.knownworld.simulation.time.SimulationTimeContract;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public final class CampaignAgricultureDayCoordinatorTest {

    private SimulationTimeContract day(long absoluteDay) {
        CampaignCalendar calendar =
                new CampaignCalendar(0, 0, 365, 1);

        calendar.restore(
                absoluteDay,
                0,
                1
        );

        return SimulationTimeContract.capture(
                new SimulationClock(absoluteDay),
                calendar
        );
    }

    @Test
    void startingDayDoesNotTriggerFakeOperation() {
        AtomicInteger operations = new AtomicInteger();

        CampaignAgricultureDayCoordinator coordinator =
                new CampaignAgricultureDayCoordinator(
                        0L,
                        (start, end, tick) ->
                                operations.incrementAndGet()
                );

        assertEquals(
                CampaignAgricultureDayCoordinator.Observation
                        .SAME_DAY,
                coordinator.observe(day(0L))
        );

        assertEquals(0, operations.get());
        assertEquals(0L, coordinator.executedDays());
    }

    @Test
    void oneCampaignDayDispatchesExactlyOnce() {
        List<String> operations = new ArrayList<>();

        CampaignAgricultureDayCoordinator coordinator =
                new CampaignAgricultureDayCoordinator(
                        0L,
                        (start, end, tick) ->
                                operations.add(
                                        start + ":" + end
                                )
                );

        assertEquals(
                CampaignAgricultureDayCoordinator.Observation
                        .EXECUTED,
                coordinator.observe(day(1L))
        );

        assertEquals(List.of("0:1"), operations);
        assertEquals(1L, coordinator.executedDays());
        assertEquals(1L, coordinator.lastObservedDay());
    }

    @Test
    void duplicateBoundaryDoesNotExecuteTwice() {
        AtomicInteger operations = new AtomicInteger();

        CampaignAgricultureDayCoordinator coordinator =
                new CampaignAgricultureDayCoordinator(
                        0L,
                        (start, end, tick) ->
                                operations.incrementAndGet()
                );

        coordinator.observe(day(1L));

        assertEquals(
                CampaignAgricultureDayCoordinator.Observation
                        .SAME_DAY,
                coordinator.observe(day(1L))
        );

        assertEquals(1, operations.get());
        assertEquals(1L, coordinator.executedDays());
    }

    @Test
    void strictModeRejectsMissingCampaignDays() {
        CampaignAgricultureDayCoordinator coordinator =
                new CampaignAgricultureDayCoordinator(
                        0L,
                        (start, end, tick) -> {}
                );

        assertThrows(
                IllegalStateException.class,
                () -> coordinator.observe(day(2L))
        );

        assertTrue(coordinator.blocked());

        assertThrows(
                IllegalStateException.class,
                () -> coordinator.observe(day(1L))
        );

        assertEquals(0L, coordinator.executedDays());
    }

    @Test
    void failedOperationBlocksAutomaticRetry() {
        AtomicInteger operations = new AtomicInteger();

        CampaignAgricultureDayCoordinator coordinator =
                new CampaignAgricultureDayCoordinator(
                        0L,
                        (start, end, tick) -> {
                            operations.incrementAndGet();

                            throw new IllegalStateException(
                                    "Simulated transaction failure"
                            );
                        }
                );

        assertThrows(
                IllegalStateException.class,
                () -> coordinator.observe(day(1L))
        );

        assertTrue(coordinator.blocked());

        assertThrows(
                IllegalStateException.class,
                () -> coordinator.observe(day(1L))
        );

        assertEquals(1, operations.get());
        assertEquals(0L, coordinator.executedDays());
    }

    @Test
    void passiveMonitorRecordsGapsWithoutSimulation() {
        CampaignAgricultureDayCoordinator coordinator =
                CampaignAgricultureDayCoordinator
                        .monitorOnly(0L);

        assertEquals(
                CampaignAgricultureDayCoordinator.Observation
                        .RECORDED_WITH_GAP,
                coordinator.observe(day(5L))
        );

        assertEquals(4L, coordinator.missedDays());
        assertEquals(5L, coordinator.lastObservedDay());
        assertEquals(0L, coordinator.executedDays());

        assertFalse(coordinator.blocked());
    }

    @Test
    void passiveMonitorRejectsBackwardsTime() {
        CampaignAgricultureDayCoordinator coordinator =
                CampaignAgricultureDayCoordinator
                        .monitorOnly(10L);

        assertThrows(
                IllegalStateException.class,
                () -> coordinator.observe(day(9L))
        );

        assertEquals(10L, coordinator.lastObservedDay());
    }

    @Test
    void mixedTimeNotificationsShareOneBoundaryState() {
        List<Long> completedDays = new ArrayList<>();

        CampaignAgricultureDayCoordinator coordinator =
                new CampaignAgricultureDayCoordinator(
                        0L,
                        (start, end, tick) ->
                                completedDays.add(end)
                );

        /*
         * One ordinary live day.
         */
        coordinator.observe(day(1L));

        /*
         * Two accelerated days.
         */
        coordinator.observe(day(2L));
        coordinator.observe(day(3L));

        /*
         * A later live observation of the same
         * campaign day must not duplicate it.
         */
        coordinator.observe(day(3L));

        assertEquals(
                List.of(1L, 2L, 3L),
                completedDays
        );

        assertEquals(3L, coordinator.executedDays());
    }

    @Test
    void scheduleReusesExistingEconomyPhase() {
        CampaignAgricultureDayCoordinator coordinator =
                CampaignAgricultureDayCoordinator
                        .monitorOnly(0L);

        assertEquals(
                SimulationPhase.ECONOMY,
                coordinator.schedule().phase()
        );

        assertEquals(
                SimulationSchedule.Frequency.CAMPAIGN_DAILY,
                coordinator.schedule().frequency()
        );
    }

    @Test
    void realCalendarTickBoundaryDrivesCoordinator() {
        CampaignCalendar calendar =
                new CampaignCalendar(0, 0, 365, 3);

        SimulationClock clock =
                new SimulationClock();

        AtomicInteger operations = new AtomicInteger();

        CampaignAgricultureDayCoordinator coordinator =
                new CampaignAgricultureDayCoordinator(
                        0L,
                        (start, end, tick) -> {
                            assertEquals(0L, start);
                            assertEquals(1L, end);
                            assertEquals(3L, tick);

                            operations.incrementAndGet();
                        }
                );

        for (int i = 0; i < 2; i++) {
            clock.advance();

            assertFalse(calendar.advanceTick());

            assertEquals(
                    CampaignAgricultureDayCoordinator.Observation
                            .SAME_DAY,
                    coordinator.observe(
                            SimulationTimeContract.capture(
                                    clock, calendar
                            )
                    )
            );
        }

        clock.advance();

        assertTrue(calendar.advanceTick());

        coordinator.observe(
                SimulationTimeContract.capture(
                        clock, calendar
                )
        );

        assertEquals(1, operations.get());
    }

    @Test
    void dailyWindowsRespectExistingHarvestIntervals() {
        AtomicInteger harvests = new AtomicInteger();

        CampaignAgricultureDayCoordinator coordinator =
                new CampaignAgricultureDayCoordinator(
                        29L,
                        (start, end, tick) -> {
                            AgriculturalInterval interval =
                                    new AgriculturalInterval(
                                            start,
                                            end,
                                            30L
                                    );

                            if (interval.harvestDue()) {
                                harvests.incrementAndGet();
                            }
                        }
                );

        /*
         * Crossing day 30 produces a harvest.
         */
        coordinator.observe(day(30L));

        /*
         * The following campaign day does not
         * repeat the same harvest.
         */
        coordinator.observe(day(31L));

        assertEquals(1, harvests.get());
        assertEquals(2L, coordinator.executedDays());
    }
}
