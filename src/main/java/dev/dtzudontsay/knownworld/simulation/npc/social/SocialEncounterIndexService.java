package dev.dtzudontsay.knownworld.simulation.npc.social;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliation;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds an indexed social topology for a supplied active/abstract population.
 *
 * Cost is approximately O(population) instead of forcing every actor to scan
 * every other actor.
 */
public final class SocialEncounterIndexService {

    private final NpcAffiliationManager affiliations;

    public SocialEncounterIndexService(
            NpcAffiliationManager affiliations
    ) {

        this.affiliations =
                Objects.requireNonNull(
                        affiliations,
                        "affiliations"
                );
    }

    public SocialEncounterIndex build(
            List<NpcState> population
    ) {

        Objects.requireNonNull(
                population,
                "population"
        );

        Map<NpcId, SocialEncounterIndex.Membership> memberships =
                new LinkedHashMap<>();

        Map<SettlementId, List<NpcState>> residenceMembers =
                new LinkedHashMap<>();

        Map<OrganizationId, List<NpcState>> householdMembers =
                new LinkedHashMap<>();

        Map<OrganizationId, List<NpcState>> nobleHouseMembers =
                new LinkedHashMap<>();

        Map<OrganizationId, List<NpcState>> factionMembers =
                new LinkedHashMap<>();

        for (
                NpcState npc :
                population
        ) {

            if (!npc.isAlive()) {
                continue;
            }

            NpcAffiliation affiliation =
                    affiliations.getOrCreate(
                            npc.id()
                    );

            SocialEncounterIndex.Membership membership =
                    new SocialEncounterIndex.Membership(
                            affiliation.residenceSettlement(),
                            affiliation.household(),
                            affiliation.nobleHouse(),
                            affiliation.faction()
                    );

            memberships.put(
                    npc.id(),
                    membership
            );

            add(
                    residenceMembers,
                    affiliation.residenceSettlement(),
                    npc
            );

            add(
                    householdMembers,
                    affiliation.household(),
                    npc
            );

            add(
                    nobleHouseMembers,
                    affiliation.nobleHouse(),
                    npc
            );

            add(
                    factionMembers,
                    affiliation.faction(),
                    npc
            );
        }

        return new SocialEncounterIndex(
                memberships,
                residenceMembers,
                householdMembers,
                nobleHouseMembers,
                factionMembers
        );
    }

    private static <K> void add(
            Map<K, List<NpcState>> index,
            K key,
            NpcState npc
    ) {

        if (key == null) {
            return;
        }

        index.computeIfAbsent(
                        key,
                        ignored ->
                                new ArrayList<>()
                )
                .add(
                        npc
                );
    }
}