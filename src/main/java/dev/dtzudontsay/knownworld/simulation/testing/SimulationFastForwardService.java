
package dev.dtzudontsay.knownworld.simulation.testing;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.lifecycle.LifeCycleService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialHistoryMode;
import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;
import dev.dtzudontsay.knownworld.simulation.time.CampaignDayObserverHub;
import dev.dtzudontsay.knownworld.simulation.time.SimulationClock;
import dev.dtzudontsay.knownworld.simulation.time.SimulationTimeContract;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Accelerated campaign-scale simulation.
 *
 * Does not wait for Minecraft wall-clock ticks
 * or require physical NPC entities.
 *
 * Batch 22.10:
 * Publishes campaign-day notifications to the same
 * observer contract used by future live simulation
 * integration.
 *
 * Existing lifecycle and social simulation behavior
 * remains unchanged.
 */
public final class SimulationFastForwardService {

    /*
     * 100 years per command for now.
     *
     * This cap remains unchanged until demographic
     * stepping, history compression and larger
     * political/economic systems are ready.
     */
    private static final int MAX_DAYS_PER_COMMAND =
            36_500;

    private final SimulationClock clock;
    private final CampaignCalendar calendar;
    private final LifeCycleService lifeCycle;
    private final AbstractSocialSimulationService social;

    private final CampaignDayObserverHub dayObservers =
            new CampaignDayObserverHub();

    public SimulationFastForwardService(
            SimulationClock clock,
            CampaignCalendar calendar,
            LifeCycleService lifeCycle,
            AbstractSocialSimulationService social
    ) {
        this.clock = Objects.requireNonNull(
                clock, "clock"
        );

        this.calendar = Objects.requireNonNull(
                calendar, "calendar"
        );

        this.lifeCycle = Objects.requireNonNull(
                lifeCycle, "lifeCycle"
        );

        this.social = Objects.requireNonNull(
                social, "social"
        );
    }

    /**
     * Subscribe a domain to campaign-day advancement.
     *
     * The observer is called once per successfully
     * advanced campaign day.
     *
     * No economic observer is installed unless a
     * runtime explicitly registers one.
     */
    public void registerCampaignDayObserver(
            Consumer<SimulationTimeContract> observer
    ) {
        dayObservers.register(observer);
    }

    public void removeCampaignDayObserver(
            Consumer<SimulationTimeContract> observer
    ) {
        dayObservers.unregister(observer);
    }

    public int campaignDayObserverCount() {
        return dayObservers.observerCount();
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
                socialPopulation, "socialPopulation"
        );

        Objects.requireNonNull(
                historyMode, "historyMode"
        );

        long startDay = calendar.absoluteDay();

        long eligibleActorEvaluations = 0L;
        long attemptedInteractions = 0L;
        long performedInteractions = 0L;
        long historyRecords = 0L;

        for (int index = 0; index < days; index++) {

            /*
             * Keep timestamps moving forward even
             * though no physical server ticks pass.
             */
            clock.advanceBy(
                    calendar.ticksPerCampaignDay()
            );

            /*
             * Authoritative historical calendar.
             */
            calendar.advanceOneDay();

            /*
             * Preserve the established life-cycle
             * order: births, deaths, pregnancies and
             * related events are handled first.
             */
            lifeCycle.onNewCampaignDay(
                    clock.tick()
            );

            /*
             * NEW, Batch 22.10:
             *
             * Notify registered campaign-day systems
             * using the existing immutable time
             * contract.
             *
             * This notification does not itself
             * create any economic activity.
             *
             * Exceptions propagate. We deliberately
             * do not continue through further days
             * after an observer fails.
             */
            dayObservers.publish(
                    clock,
                    calendar
            );

            /*
             * Preserve the existing abstract
             * social simulation logic.
             */
            AbstractSocialSimulationService.DayResult
                    socialResult = social.simulateDay(
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
