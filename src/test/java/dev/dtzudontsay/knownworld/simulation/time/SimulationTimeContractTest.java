
package dev.dtzudontsay.knownworld.simulation.time;

/**
 * Batch 20.1
 *
 * Dependency-free regression tests for simulation time.
 *
 * Run main() with Java assertions not required.
 * Failed checks throw AssertionError.
 */
public final class SimulationTimeContractTest {

    private SimulationTimeContractTest() {
    }

    public static void main(
            String[] args
    ) {

        testDefaultTimeConfiguration();
        testCampaignDayAdvancement();
        testRoutineDayAdvancement();
        testTimeSnapshot();
        testTickIntervalSchedule();
        testCampaignDailySchedule();
        testCampaignDayIntervalSchedule();
        testCampaignYearlySchedule();
        testFastForwardClockSemantics();
        testClockOverflowProtection();
        testInvalidSchedule();

        System.out.println(
                "Batch 20.1 time contract tests passed."
        );
    }

    private static void testDefaultTimeConfiguration() {

        SimulationClock clock =
                new SimulationClock();

        CampaignCalendar calendar =
                new CampaignCalendar();

        check(
                clock.tick() == 0L,
                "Simulation clock must begin at tick zero"
        );

        check(
                calendar.year() == 298,
                "Default campaign must start in 298 AC"
        );

        check(
                calendar.dayOfYear() == 0,
                "Default campaign must start at day zero"
        );

        check(
                calendar.ticksPerCampaignDay() == 1200,
                "Current campaign-day duration changed"
        );
    }

    private static void testCampaignDayAdvancement() {

        SimulationClock clock =
                new SimulationClock();

        CampaignCalendar calendar =
                new CampaignCalendar();

        long startingDay =
                calendar.absoluteDay();

        for (int i = 0; i < 1199; i++) {

            clock.advance();

            boolean advanced =
                    calendar.advanceTick();

            check(
                    !advanced,
                    "Campaign day advanced too early"
            );
        }

        clock.advance();

        check(
                calendar.advanceTick(),
                "Campaign day should advance on tick 1200"
        );

        check(
                calendar.absoluteDay() == startingDay + 1,
                "Campaign day index incorrect"
        );

        check(
                calendar.tickRemainder() == 0,
                "Campaign tick remainder not reset"
        );
    }

    private static void testRoutineDayAdvancement() {

        SimulationClock clock =
                new SimulationClock();

        clock.advanceBy(24000L);

        check(
                clock.day() == 1L,
                "Routine day must contain 24000 ticks"
        );

        check(
                clock.tickOfDay() == 0,
                "Routine tick remainder incorrect"
        );
    }

    private static void testTimeSnapshot() {

        SimulationClock clock =
                new SimulationClock();

        CampaignCalendar calendar =
                new CampaignCalendar();

        SimulationTimeContract snapshot =
                SimulationTimeContract.capture(
                        clock,
                        calendar
                );

        check(
                snapshot.simulationTick() == 0L,
                "Initial snapshot tick incorrect"
        );

        check(
                snapshot.campaignYear() == 298,
                "Initial snapshot year incorrect"
        );

        check(
                snapshot.campaignDaysPerRoutineDay()
                        == 20.0,
                "Existing time-scale ratio changed"
        );

        check(
                snapshot.campaignDateLabel().equals(
                        "298 AC, day 1"
                ),
                "Campaign date label incorrect"
        );
    }

    private static void testTickIntervalSchedule() {

        SimulationSchedule schedule =
                SimulationSchedule.everyTicks(
                        SimulationPhase.NEEDS,
                        20L
                );

        SimulationClock clock =
                new SimulationClock();

        CampaignCalendar calendar =
                new CampaignCalendar();

        check(
                !schedule.isDue(
                        SimulationTimeContract.capture(
                                clock,
                                calendar
                        ),
                        false
                ),
                "Tick interval must not run at tick zero"
        );

        clock.advanceBy(19L);

        check(
                !schedule.isDue(
                        SimulationTimeContract.capture(
                                clock,
                                calendar
                        ),
                        false
                ),
                "20-tick schedule ran at tick 19"
        );

        clock.advance();

        check(
                schedule.isDue(
                        SimulationTimeContract.capture(
                                clock,
                                calendar
                        ),
                        false
                ),
                "20-tick schedule did not run at tick 20"
        );
    }

    private static void testCampaignDailySchedule() {

        SimulationSchedule schedule =
                SimulationSchedule.campaignDaily(
                        SimulationPhase.LIFE_CYCLE
                );

        SimulationClock clock =
                new SimulationClock();

        CampaignCalendar calendar =
                new CampaignCalendar();

        check(
                !schedule.isDue(
                        SimulationTimeContract.capture(
                                clock,
                                calendar
                        ),
                        false
                ),
                "Initial day boundary must not trigger work"
        );

        clock.advanceBy(1200L);

        calendar.advanceOneDay();

        check(
                schedule.isDue(
                        SimulationTimeContract.capture(
                                clock,
                                calendar
                        ),
                        true
                ),
                "Daily schedule should run after advancement"
        );

        check(
                !schedule.isDue(
                        SimulationTimeContract.capture(
                                clock,
                                calendar
                        ),
                        false
                ),
                "Daily schedule must not run twice"
        );
    }

    private static void testCampaignDayIntervalSchedule() {

        SimulationSchedule schedule =
                SimulationSchedule.everyCampaignDays(
                        SimulationPhase.ECONOMY,
                        7L
                );

        SimulationClock clock =
                new SimulationClock();

        CampaignCalendar calendar =
                new CampaignCalendar();

        boolean foundDueDay = false;

        for (int i = 0; i < 14; i++) {

            clock.advanceBy(
                    calendar.ticksPerCampaignDay()
            );

            calendar.advanceOneDay();

            SimulationTimeContract time =
                    SimulationTimeContract.capture(
                            clock,
                            calendar
                    );

            boolean expected =
                    time.campaignAbsoluteDay() % 7L == 0L;

            check(
                    schedule.isDue(time, true) == expected,
                    "Campaign-day interval mismatch"
            );

            foundDueDay |= expected;
        }

        check(
                foundDueDay,
                "No scheduled campaign interval was found"
        );
    }

    private static void testCampaignYearlySchedule() {

        SimulationSchedule schedule =
                SimulationSchedule.campaignYearly(
                        SimulationPhase.DEMOGRAPHY
                );

        SimulationClock clock =
                new SimulationClock();

        CampaignCalendar calendar =
                new CampaignCalendar();

        int daysRemaining =
                calendar.daysPerYear()
                        - calendar.dayOfYear();

        for (int i = 0; i < daysRemaining; i++) {

            clock.advanceBy(
                    calendar.ticksPerCampaignDay()
            );

            calendar.advanceOneDay();
        }

        SimulationTimeContract time =
                SimulationTimeContract.capture(
                        clock,
                        calendar
                );

        check(
                time.campaignYear() == 299,
                "Year rollover incorrect"
        );

        check(
                schedule.isDue(time, true),
                "Yearly schedule did not run at rollover"
        );
    }

    private static void testFastForwardClockSemantics() {

        SimulationClock clock =
                new SimulationClock();

        CampaignCalendar calendar =
                new CampaignCalendar();

        for (int i = 0; i < 100; i++) {

            clock.advanceBy(
                    calendar.ticksPerCampaignDay()
            );

            calendar.advanceOneDay();
        }

        SimulationTimeContract time =
                SimulationTimeContract.capture(
                        clock,
                        calendar
                );

        check(
                time.simulationTick() == 120000L,
                "Fast-forward tick progression incorrect"
        );

        check(
                time.routineDay() == 5L,
                "Routine day calculation incorrect"
        );

        check(
                time.campaignDayOfYear() == 100,
                "Campaign day calculation incorrect"
        );
    }

    private static void testClockOverflowProtection() {

        SimulationClock clock =
                new SimulationClock(
                        Long.MAX_VALUE
                );

        boolean rejected = false;

        try {
            clock.advance();
        } catch (IllegalStateException expected) {
            rejected = true;
        }

        check(
                rejected,
                "Simulation clock must reject overflow"
        );
    }

    private static void testInvalidSchedule() {

        boolean rejected = false;

        try {
            SimulationSchedule.everyTicks(
                    SimulationPhase.ACTIONS,
                    0L
            );
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }

        check(
                rejected,
                "Zero-length schedule must be rejected"
        );
    }

    private static void check(
            boolean condition,
            String message
    ) {

        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
