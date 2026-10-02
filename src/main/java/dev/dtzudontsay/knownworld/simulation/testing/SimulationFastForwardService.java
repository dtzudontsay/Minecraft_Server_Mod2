package dev.dtzudontsay.knownworld.simulation.testing;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.LifeCycleService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialHistoryMode;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.simulation.time.SimulationClock;

import java.util.List;
import java.util.Objects;

/**
 * Accelerated campaign-scale simulation.
 *
 * This does not wait for Minecraft wall-clock ticks and does not require
 * physical NPC entities.
 */
public final class SimulationFastForwardService {

    /*
     * 100 years per command for now.
     *
     * We will raise/remove this once demographic stepping, history
     * compression and larger political/economic systems are ready.
     */
    private static final int MAX_DAYS_PER_COMMAND =
            36_500;

    private final SimulationClock clock;

    private final CampaignCalendar calendar;

    private final LifeCycleService lifeCycle;

    private final AbstractSocialSimulationService social;

    public SimulationFastForwardService(
            SimulationClock clock,
            CampaignCalendar calendar,
            LifeCycleService lifeCycle,
            AbstractSocialSimulationService social
    ) {

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "clock"
                );

        this.calendar =
                Objects.requireNonNull(
                        calendar,
                        "calendar"
                );

        this.lifeCycle =
                Objects.requireNonNull(
                        lifeCycle,
                        "lifeCycle"
                );

        this.social =
                Objects.requireNonNull(
                        social,
                        "social"
                );
    }

    public Result advanceDays(
            int days,
            List<NpcId> socialPopulation,
            SocialHistoryMode historyMode
    ) {

        if (days <= 0
                || days > MAX_DAYS_PER_COMMAND) {

            throw new IllegalArgumentException(
                    "Fast-forward days must be between 1 and "
                            + MAX_DAYS_PER_COMMAND
                            + " per command"
            );
        }

        Objects.requireNonNull(
                socialPopulation,
                "socialPopulation"
        );

        Objects.requireNonNull(
                historyMode,
                "historyMode"
        );

        long startDay =
                calendar.absoluteDay();

        long eligibleActorEvaluations =
                0L;

        long attemptedInteractions =
                0L;

        long performedInteractions =
                0L;

        long historyRecords =
                0L;

        for (
                int index = 0;
                index < days;
                index++
        ) {

            /*
             * Keep event/memory timestamps moving forward even though we are
             * not waiting for real server ticks.
             */
            clock.advanceBy(
                    calendar.ticksPerCampaignDay()
            );

            calendar.advanceOneDay();

            /*
             * The ordinary lifecycle is run once per simulated campaign day.
             *
             * Birth, pregnancy, aging and death therefore remain part of
             * accelerated simulation.
             */
            lifeCycle.onNewCampaignDay(
                    clock.tick()
            );

            AbstractSocialSimulationService.DayResult socialResult =
                    social.simulateDay(
                            socialPopulation,
                            calendar.absoluteDay(),
                            clock.tick(),
                            historyMode
                    );

            eligibleActorEvaluations +=
                    socialResult.eligibleActors();

            attemptedInteractions +=
                    socialResult.attemptedInteractions();

            performedInteractions +=
                    socialResult.performedInteractions();

            historyRecords +=
                    socialResult.historyRecords();
        }

        return new Result(
                startDay,
                calendar.absoluteDay(),
                days,
                eligibleActorEvaluations,
                attemptedInteractions,
                performedInteractions,
                historyRecords
        );
    }

    public record Result(
            long startAbsoluteDay,
            long endAbsoluteDay,
            int daysAdvanced,
            long eligibleActorEvaluations,
            long attemptedInteractions,
            long performedInteractions,
            long historyRecords
    ) {
    }
}