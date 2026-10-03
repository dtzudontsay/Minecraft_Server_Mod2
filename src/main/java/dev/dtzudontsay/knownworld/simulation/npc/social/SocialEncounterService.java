package dev.dtzudontsay.knownworld.simulation.npc.social;

import dev.dtzudontsay.knownworld.simulation.npc.NpcState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;

/**
 * Selects social encounters from indexed structural contexts.
 *
 * SIM 06:
 *
 * This service no longer scans the entire population.
 *
 * Instead, an actor first selects a plausible context:
 *
 * - household
 * - residence
 * - noble house
 * - faction
 *
 * and then selects another NPC directly from that indexed context.
 */
public final class SocialEncounterService {

    private static final double HOUSEHOLD_WEIGHT =
            8.0;

    private static final double RESIDENCE_WEIGHT =
            5.0;

    private static final double NOBLE_HOUSE_WEIGHT =
            3.0;

    private static final double FACTION_WEIGHT =
            0.60;

    public NpcState chooseTarget(
            NpcState actor,
            SocialEncounterIndex index,
            SplittableRandom random
    ) {

        Objects.requireNonNull(
                actor,
                "actor"
        );

        Objects.requireNonNull(
                index,
                "index"
        );

        Objects.requireNonNull(
                random,
                "random"
        );

        SocialEncounterIndex.Membership membership =
                index.membership(
                        actor.id()
                );

        if (membership == null) {
            return null;
        }

        List<PoolChoice> availablePools =
                new ArrayList<>(
                        4
                );

        addPoolIfUsable(
                availablePools,
                index.householdMembers(
                        membership.household()
                ),
                HOUSEHOLD_WEIGHT
        );

        addPoolIfUsable(
                availablePools,
                index.residenceMembers(
                        membership.residence()
                ),
                RESIDENCE_WEIGHT
        );

        addPoolIfUsable(
                availablePools,
                index.nobleHouseMembers(
                        membership.nobleHouse()
                ),
                NOBLE_HOUSE_WEIGHT
        );

        addPoolIfUsable(
                availablePools,
                index.factionMembers(
                        membership.faction()
                ),
                FACTION_WEIGHT
        );

        if (availablePools.isEmpty()) {
            return null;
        }

        PoolChoice chosenPool =
                choosePool(
                        availablePools,
                        random
                );

        return chooseOtherMember(
                actor,
                chosenPool.members(),
                random
        );
    }

    private static void addPoolIfUsable(
            List<PoolChoice> choices,
            List<NpcState> members,
            double weight
    ) {

        if (members == null
                || members.size() < 2
                || weight <= 0.0) {

            return;
        }

        choices.add(
                new PoolChoice(
                        members,
                        weight
                )
        );
    }

    private static PoolChoice choosePool(
            List<PoolChoice> choices,
            SplittableRandom random
    ) {

        double total =
                0.0;

        for (
                PoolChoice choice :
                choices
        ) {

            total +=
                    choice.weight();
        }

        double roll =
                random.nextDouble(
                        total
                );

        for (
                PoolChoice choice :
                choices
        ) {

            roll -=
                    choice.weight();

            if (roll <= 0.0) {
                return choice;
            }
        }

        return choices.get(
                choices.size()
                        - 1
        );
    }

    private static NpcState chooseOtherMember(
            NpcState actor,
            List<NpcState> members,
            SplittableRandom random
    ) {

        if (members.size() < 2) {
            return null;
        }

        /*
         * Random attempts avoid building a second temporary list in the
         * overwhelmingly common case.
         */
        for (
                int attempt = 0;
                attempt < 6;
                attempt++
        ) {

            NpcState candidate =
                    members.get(
                            random.nextInt(
                                    members.size()
                            )
                    );

            if (!candidate.id()
                    .equals(
                            actor.id()
                    )
                    &&
                    candidate.isAlive()) {

                return candidate;
            }
        }

        /*
         * Deterministic fallback guarantees success when another valid
         * member exists.
         */
        for (
                NpcState candidate :
                members
        ) {

            if (!candidate.id()
                    .equals(
                            actor.id()
                    )
                    &&
                    candidate.isAlive()) {

                return candidate;
            }
        }

        return null;
    }

    private record PoolChoice(
            List<NpcState> members,
            double weight
    ) {
    }
}