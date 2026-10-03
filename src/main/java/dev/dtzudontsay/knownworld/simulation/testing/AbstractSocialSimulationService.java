package dev.dtzudontsay.knownworld.simulation.testing;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialActionDecisionService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialEncounterIndex;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialEncounterIndexService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialEncounterService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialHistoryMode;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialInteractionService;

import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;

/**
 * Campaign-scale/off-screen social simulation.
 *
 * SIM 06:
 *
 * Social candidate lookup now uses a per-day index rather than repeatedly
 * scanning the full supplied population for every actor.
 */
public final class AbstractSocialSimulationService {

    private static final double BASE_DAILY_INTERACTION_CHANCE =
            0.18;

    private final NpcRegistry registry;

    private final SocialEncounterIndexService encounterIndexes;

    private final SocialEncounterService encounters;

    private final SocialInteractionService interactions;

    private final SocialActionDecisionService decisions;

    public AbstractSocialSimulationService(
            NpcRegistry registry,
            SocialEncounterIndexService encounterIndexes,
            SocialEncounterService encounters,
            SocialInteractionService interactions,
            SocialActionDecisionService decisions
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.encounterIndexes =
                Objects.requireNonNull(
                        encounterIndexes,
                        "encounterIndexes"
                );

        this.encounters =
                Objects.requireNonNull(
                        encounters,
                        "encounters"
                );

        this.interactions =
                Objects.requireNonNull(
                        interactions,
                        "interactions"
                );

        this.decisions =
                Objects.requireNonNull(
                        decisions,
                        "decisions"
                );
    }

    public DayResult simulateDay(
            List<NpcId> population,
            long absoluteDay,
            long simulationTick,
            SocialHistoryMode historyMode
    ) {

        Objects.requireNonNull(
                population,
                "population"
        );

        Objects.requireNonNull(
                historyMode,
                "historyMode"
        );

        List<NpcState> alive =
                population.stream()
                        .map(
                                registry::find
                        )
                        .flatMap(
                                java.util.Optional::stream
                        )
                        .filter(
                                NpcState::isAlive
                        )
                        .toList();

        if (alive.size()
                < 2) {

            return new DayResult(
                    alive.size(),
                    0,
                    0,
                    0
            );
        }

        /*
         * Built once for this simulated day.
         *
         * Later, when settlement populations and active campaign groups become
         * very large, the same concept can be cached or incrementally updated.
         */
        SocialEncounterIndex encounterIndex =
                encounterIndexes.build(
                        alive
                );

        int eligibleActors =
                alive.size();

        int attempted =
                0;

        int performed =
                0;

        int historyRecorded =
                0;

        for (
                NpcState actor :
                alive
        ) {

            SplittableRandom random =
                    randomFor(
                            absoluteDay,
                            actor.id()
                    );

            double chance =
                    clampUnit(
                            BASE_DAILY_INTERACTION_CHANCE
                                    +
                                    actor.personality()
                                            .sociability()
                                            * 0.08
                    );

            if (random.nextDouble()
                    >= chance) {

                continue;
            }

            attempted++;

            NpcState target =
                    encounters.chooseTarget(
                            actor,
                            encounterIndex,
                            random
                    );

            if (target == null) {

                continue;
            }

            SocialActionDecisionService.Decision decision =
                    decisions.choose(
                            actor.id(),
                            target.id(),
                            simulationTick,
                            random
                    );

            SocialInteractionService.Result result =
                    interactions.perform(
                            actor.id(),
                            target.id(),
                            decision.type(),
                            decision.magnitude(),
                            simulationTick,
                            historyMode
                    );

            performed++;

            if (result.historyRecorded()) {

                historyRecorded++;
            }
        }

        return new DayResult(
                eligibleActors,
                attempted,
                performed,
                historyRecorded
        );
    }

    private static SplittableRandom randomFor(
            long absoluteDay,
            NpcId actor
    ) {

        long seed =
                absoluteDay;

        seed ^=
                actor.value()
                        * 0x9E3779B97F4A7C15L;

        seed ^=
                0x5A17C0C1A1L;

        seed =
                mix64(
                        seed
                );

        return new SplittableRandom(
                seed
        );
    }

    private static long mix64(
            long value
    ) {

        value ^=
                value >>> 30;

        value *=
                0xBF58476D1CE4E5B9L;

        value ^=
                value >>> 27;

        value *=
                0x94D049BB133111EBL;

        value ^=
                value >>> 31;

        return value;
    }

    private static double clampUnit(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    public record DayResult(
            int eligibleActors,
            int attemptedInteractions,
            int performedInteractions,
            int historyRecords
    ) {
    }
}