package dev.dtzudontsay.knownworld.simulation.npc.social;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliation;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.Objects;

/**
 * Resolves the durable social contexts shared by two NPCs.
 *
 * This class deliberately does NOT know anything about synthetic sandbox
 * surnames or Minecraft block distance.
 *
 * It uses persistent simulation structure:
 *
 * - household
 * - residence settlement
 * - noble house
 * - faction
 *
 * Later temporary contexts such as:
 *
 * - army unit
 * - ship crew
 * - caravan
 * - travelling party
 * - feast
 * - tournament
 * - council meeting
 * - battle
 *
 * can be added here or composed into this result without replacing the
 * encounter system.
 */
public final class SocialContextService {

    private final NpcAffiliationManager affiliations;

    public SocialContextService(
            NpcAffiliationManager affiliations
    ) {

        this.affiliations =
                Objects.requireNonNull(
                        affiliations,
                        "affiliations"
                );
    }

    public Context between(
            NpcId first,
            NpcId second
    ) {

        Objects.requireNonNull(
                first,
                "first"
        );

        Objects.requireNonNull(
                second,
                "second"
        );

        if (first.equals(
                second
        )) {

            throw new IllegalArgumentException(
                    "Cannot calculate social context against self"
            );
        }

        NpcAffiliation firstAffiliation =
                affiliations.getOrCreate(
                        first
                );

        NpcAffiliation secondAffiliation =
                affiliations.getOrCreate(
                        second
                );

        boolean sameResidence =
                same(
                        firstAffiliation.residenceSettlement(),
                        secondAffiliation.residenceSettlement()
                );

        boolean sameHousehold =
                same(
                        firstAffiliation.household(),
                        secondAffiliation.household()
                );

        boolean sameNobleHouse =
                same(
                        firstAffiliation.nobleHouse(),
                        secondAffiliation.nobleHouse()
                );

        boolean sameFaction =
                same(
                        firstAffiliation.faction(),
                        secondAffiliation.faction()
                );

        return new Context(
                sameResidence,
                sameHousehold,
                sameNobleHouse,
                sameFaction
        );
    }

    public double encounterWeight(
            NpcId first,
            NpcId second
    ) {

        Context context =
                between(
                        first,
                        second
                );

        /*
         * A non-zero background weight remains for now because the caller
         * already supplies a socially relevant candidate population.
         *
         * Later the candidate population itself will be generated from
         * geographical/local event context, at which point unrelated people
         * on different continents will never enter the candidate set.
         */
        double weight =
                0.15;

        if (context.sameResidence()) {

            weight +=
                    4.00;
        }

        if (context.sameHousehold()) {

            weight +=
                    8.00;
        }

        if (context.sameNobleHouse()) {

            weight +=
                    2.50;
        }

        if (context.sameFaction()) {

            weight +=
                    2.00;
        }

        return weight;
    }

    private static boolean same(
            SettlementId first,
            SettlementId second
    ) {

        return first != null
                && first.equals(
                second
        );
    }

    private static boolean same(
            OrganizationId first,
            OrganizationId second
    ) {

        return first != null
                && first.equals(
                second
        );
    }

    public record Context(
            boolean sameResidence,
            boolean sameHousehold,
            boolean sameNobleHouse,
            boolean sameFaction
    ) {

        public boolean sharesAnyContext() {

            return sameResidence
                    || sameHousehold
                    || sameNobleHouse
                    || sameFaction;
        }
    }
}