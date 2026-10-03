package dev.dtzudontsay.knownworld.simulation.npc.social;

import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationship;
import dev.dtzudontsay.knownworld.simulation.npc.relationship.NpcRelationshipManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;

/**
 * Chooses which socially eligible NPC an actor encounters.
 *
 * Encounter selection is intentionally separate from:
 *
 * - deciding what the actor does
 * - deciding how the target reacts
 * - actually applying the interaction
 *
 * This lets future systems provide candidate populations from settlements,
 * courts, armies, journeys, ships, households, organizations and events
 * while reusing the same encounter mechanics.
 */
public final class SocialEncounterService {

    private final SocialContextService contexts;

    private final NpcRelationshipManager relationships;

    public SocialEncounterService(
            SocialContextService contexts,
            NpcRelationshipManager relationships
    ) {

        this.contexts =
                Objects.requireNonNull(
                        contexts,
                        "contexts"
                );

        this.relationships =
                Objects.requireNonNull(
                        relationships,
                        "relationships"
                );
    }

    public NpcState chooseTarget(
            NpcState actor,
            List<NpcState> candidates,
            SplittableRandom random
    ) {

        Objects.requireNonNull(
                actor,
                "actor"
        );

        Objects.requireNonNull(
                candidates,
                "candidates"
        );

        Objects.requireNonNull(
                random,
                "random"
        );

        List<NpcState> valid =
                new ArrayList<>();

        List<Double> weights =
                new ArrayList<>();

        double totalWeight =
                0.0;

        for (
                NpcState candidate :
                candidates
        ) {

            if (!candidate.isAlive()) {
                continue;
            }

            if (candidate.id()
                    .equals(
                            actor.id()
                    )) {

                continue;
            }

            double weight =
                    contexts.encounterWeight(
                            actor.id(),
                            candidate.id()
                    );

            /*
             * Familiarity can reinforce an existing social network,
             * but it should not outweigh strong structural context such as
             * household or residence.
             */
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
                                * 0.75;
            }

            if (weight <= 0.0) {
                continue;
            }

            valid.add(
                    candidate
            );

            weights.add(
                    weight
            );

            totalWeight +=
                    weight;
        }

        if (valid.isEmpty()
                || totalWeight <= 0.0) {

            return null;
        }

        double roll =
                random.nextDouble(
                        totalWeight
                );

        for (
                int index = 0;
                index < valid.size();
                index++
        ) {

            roll -=
                    weights.get(
                            index
                    );

            if (roll <= 0.0) {

                return valid.get(
                        index
                );
            }
        }

        return valid.get(
                valid.size()
                        - 1
        );
    }
}