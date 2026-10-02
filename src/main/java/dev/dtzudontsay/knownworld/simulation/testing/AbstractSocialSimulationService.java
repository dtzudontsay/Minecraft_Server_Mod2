package dev.dtzudontsay.knownworld.simulation.testing;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialActionDecisionService;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialHistoryMode;
import dev.dtzudontsay.knownworld.simulation.npc.social.SocialInteractionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;

public final class AbstractSocialSimulationService {

    private static final double BASE_DAILY_INTERACTION_CHANCE =
            0.18;

    private static final double SAME_COHORT_TARGET_WEIGHT =
            4.0;

    private final NpcRegistry registry;

    private final NpcRelationshipManager relationships;

    private final SocialInteractionService interactions;

    private final SocialActionDecisionService decisions;

    public AbstractSocialSimulationService(
            NpcRegistry registry,
            NpcRelationshipManager relationships,
            SocialInteractionService interactions,
            SocialActionDecisionService decisions
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.relationships =
                Objects.requireNonNull(
                        relationships,
                        "relationships"
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
                    chooseTarget(
                            actor,
                            alive,
                            random
                    );

            if (target == null) {

                continue;
            }

            /*
             * SIM 03:
             *
             * The abstract simulation no longer decides actions itself.
             *
             * The same reusable decision service can later be called by
             * physical AI, court simulation, political encounters, journeys,
             * armies and other contexts.
             */
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

    private NpcState chooseTarget(
            NpcState actor,
            List<NpcState> alive,
            SplittableRandom random
    ) {

        List<NpcState> candidates =
                new ArrayList<>();

        List<Double> weights =
                new ArrayList<>();

        double totalWeight =
                0.0;

        for (
                NpcState candidate :
                alive
        ) {

            if (candidate.id()
                    .equals(
                            actor.id()
                    )) {

                continue;
            }

            double weight =
                    1.0;

            /*
             * This remains a temporary sandbox stand-in for real social
             * context.
             *
             * Later this is replaced with household/settlement/court/unit/
             * organization/travel/event candidate selection.
             */
            if (actor.identity()
                    .familyName()
                    .equals(
                            candidate.identity()
                                    .familyName()
                    )) {

                weight *=
                        SAME_COHORT_TARGET_WEIGHT;
            }

            NpcRelationship existing =
                    relationships.find(
                                    actor.id(),
                                    candidate.id()
                            )
                            .orElse(
                                    null
                            );

            if (existing != null) {

                weight +=
                        existing.familiarity()
                                * 2.0;
            }

            candidates.add(
                    candidate
            );

            weights.add(
                    weight
            );

            totalWeight +=
                    weight;
        }

        if (candidates.isEmpty()
                || totalWeight <= 0.0) {

            return null;
        }

        double roll =
                random.nextDouble(
                        totalWeight
                );

        for (
                int index = 0;
                index < candidates.size();
                index++
        ) {

            roll -=
                    weights.get(
                            index
                    );

            if (roll <= 0.0) {

                return candidates.get(
                        index
                );
            }
        }

        return candidates.get(
                candidates.size()
                        - 1
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