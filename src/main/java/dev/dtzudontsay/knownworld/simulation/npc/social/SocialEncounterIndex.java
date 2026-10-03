package dev.dtzudontsay.knownworld.simulation.npc.social;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcState;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable lookup structure used during one abstract simulation step.
 *
 * It prevents encounter generation from repeatedly scanning the entire
 * population for every NPC.
 */
public final class SocialEncounterIndex {

    private final Map<NpcId, Membership> memberships;

    private final Map<SettlementId, List<NpcState>> residenceMembers;

    private final Map<OrganizationId, List<NpcState>> householdMembers;

    private final Map<OrganizationId, List<NpcState>> nobleHouseMembers;

    private final Map<OrganizationId, List<NpcState>> factionMembers;

    public SocialEncounterIndex(
            Map<NpcId, Membership> memberships,
            Map<SettlementId, List<NpcState>> residenceMembers,
            Map<OrganizationId, List<NpcState>> householdMembers,
            Map<OrganizationId, List<NpcState>> nobleHouseMembers,
            Map<OrganizationId, List<NpcState>> factionMembers
    ) {

        this.memberships =
                Map.copyOf(
                        Objects.requireNonNull(
                                memberships,
                                "memberships"
                        )
                );

        this.residenceMembers =
                copyLists(
                        residenceMembers
                );

        this.householdMembers =
                copyLists(
                        householdMembers
                );

        this.nobleHouseMembers =
                copyLists(
                        nobleHouseMembers
                );

        this.factionMembers =
                copyLists(
                        factionMembers
                );
    }

    public Membership membership(
            NpcId npc
    ) {

        return memberships.get(
                npc
        );
    }

    public List<NpcState> residenceMembers(
            SettlementId settlement
    ) {

        if (settlement == null) {
            return List.of();
        }

        return residenceMembers.getOrDefault(
                settlement,
                List.of()
        );
    }

    public List<NpcState> householdMembers(
            OrganizationId household
    ) {

        if (household == null) {
            return List.of();
        }

        return householdMembers.getOrDefault(
                household,
                List.of()
        );
    }

    public List<NpcState> nobleHouseMembers(
            OrganizationId nobleHouse
    ) {

        if (nobleHouse == null) {
            return List.of();
        }

        return nobleHouseMembers.getOrDefault(
                nobleHouse,
                List.of()
        );
    }

    public List<NpcState> factionMembers(
            OrganizationId faction
    ) {

        if (faction == null) {
            return List.of();
        }

        return factionMembers.getOrDefault(
                faction,
                List.of()
        );
    }

    public int populationSize() {
        return memberships.size();
    }

    private static <K> Map<K, List<NpcState>> copyLists(
            Map<K, List<NpcState>> source
    ) {

        Objects.requireNonNull(
                source,
                "source"
        );

        java.util.LinkedHashMap<K, List<NpcState>> copied =
                new java.util.LinkedHashMap<>();

        for (
                Map.Entry<K, List<NpcState>> entry :
                source.entrySet()
        ) {

            copied.put(
                    entry.getKey(),
                    List.copyOf(
                            entry.getValue()
                    )
            );
        }

        return Map.copyOf(
                copied
        );
    }

    public record Membership(
            SettlementId residence,
            OrganizationId household,
            OrganizationId nobleHouse,
            OrganizationId faction
    ) {
    }
}