package dev.dtzudontsay.knownworld.simulation.social.membership;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliation;
import dev.dtzudontsay.knownworld.simulation.social.NpcAffiliationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class OrganizationMembershipManager {

    private final NpcRegistry npcs;

    private final OrganizationManager organizations;

    private final Map<Key, OrganizationMembership> memberships =
            new LinkedHashMap<>();

    public OrganizationMembershipManager(
            NpcRegistry npcs,
            OrganizationManager organizations
    ) {

        this.npcs =
                Objects.requireNonNull(
                        npcs,
                        "npcs"
                );

        this.organizations =
                Objects.requireNonNull(
                        organizations,
                        "organizations"
                );
    }

    public synchronized OrganizationMembership join(
            NpcId npc,
            OrganizationId organization,
            long tick,
            double loyalty
    ) {

        validateNpc(
                npc
        );

        validateOrganization(
                organization
        );

        Key key =
                new Key(
                        npc,
                        organization
                );

        OrganizationMembership existing =
                memberships.get(
                        key
                );

        if (existing != null
                && existing.isActive()) {

            existing.setLoyalty(
                    loyalty
            );

            return existing;
        }

        OrganizationMembership membership =
                new OrganizationMembership(
                        npc,
                        organization,
                        tick,
                        -1,
                        OrganizationMembershipStatus.ACTIVE,
                        loyalty,
                        Set.of()
                );

        memberships.put(
                key,
                membership
        );

        return membership;
    }

    public synchronized void registerLoaded(
            OrganizationMembership membership
    ) {

        Objects.requireNonNull(
                membership,
                "membership"
        );

        validateNpc(
                membership.npc()
        );

        validateOrganization(
                membership.organization()
        );

        memberships.put(
                new Key(
                        membership.npc(),
                        membership.organization()
                ),
                membership
        );
    }

    public synchronized Optional<OrganizationMembership> find(
            NpcId npc,
            OrganizationId organization
    ) {

        return Optional.ofNullable(
                memberships.get(
                        new Key(
                                npc,
                                organization
                        )
                )
        );
    }

    public synchronized List<OrganizationMembership> membershipsFor(
            NpcId npc
    ) {

        validateNpc(
                npc
        );

        return memberships.values()
                .stream()
                .filter(
                        membership ->
                                membership.npc()
                                        .equals(
                                                npc
                                        )
                )
                .toList();
    }

    public synchronized List<OrganizationMembership> activeMembershipsFor(
            NpcId npc
    ) {

        return membershipsFor(
                npc
        )
                .stream()
                .filter(
                        OrganizationMembership::isActive
                )
                .toList();
    }

    public synchronized List<OrganizationMembership> activeMembersOf(
            OrganizationId organization
    ) {

        validateOrganization(
                organization
        );

        return memberships.values()
                .stream()
                .filter(
                        membership ->
                                membership.organization()
                                        .equals(
                                                organization
                                        )
                                        && membership.isActive()
                )
                .toList();
    }

    public synchronized void addRole(
            NpcId npc,
            OrganizationId organization,
            String role
    ) {

        if (WorldReferenceCatalog.get()
                .role(
                        role
                )
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown organization role "
                            + role
            );
        }

        find(
                npc,
                organization
        )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "NPC "
                                                + npc
                                                + " is not a member of organization "
                                                + organization
                                )
                )
                .addRole(
                        role
                );
    }

    public synchronized void leave(
            NpcId npc,
            OrganizationId organization,
            long tick,
            OrganizationMembershipStatus reason
    ) {

        find(
                npc,
                organization
        )
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "No membership between NPC "
                                                + npc
                                                + " and organization "
                                                + organization
                                )
                )
                .leave(
                        tick,
                        reason
                );
    }

    /**
     * Transitional bridge between the old primary-affiliation system
     * and the new many-to-many membership model.
     *
     * No information is removed from NpcAffiliation.
     */
    public synchronized void ensurePrimaryAffiliations(
            NpcAffiliationManager affiliations,
            long tick
    ) {

        for (
                NpcAffiliation affiliation :
                affiliations.all()
        ) {

            ensureIfPresent(
                    affiliation.owner(),
                    affiliation.household(),
                    tick,
                    0.70
            );

            ensureIfPresent(
                    affiliation.owner(),
                    affiliation.nobleHouse(),
                    tick,
                    0.80
            );

            ensureIfPresent(
                    affiliation.owner(),
                    affiliation.faction(),
                    tick,
                    0.65
            );
        }
    }

    public synchronized Collection<OrganizationMembership> all() {

        return List.copyOf(
                memberships.values()
        );
    }

    public synchronized int size() {

        return memberships.size();
    }

    private void ensureIfPresent(
            NpcId npc,
            OrganizationId organization,
            long tick,
            double loyalty
    ) {

        if (organization == null) {
            return;
        }

        Optional<OrganizationMembership> existing =
                find(
                        npc,
                        organization
                );

        if (existing.isPresent()) {
            return;
        }

        join(
                npc,
                organization,
                tick,
                loyalty
        );
    }

    private void validateNpc(
            NpcId npc
    ) {

        Objects.requireNonNull(
                npc,
                "npc"
        );

        if (!npcs.contains(
                npc
        )) {

            throw new IllegalArgumentException(
                    "Unknown NPC "
                            + npc
            );
        }
    }

    private void validateOrganization(
            OrganizationId organization
    ) {

        Objects.requireNonNull(
                organization,
                "organization"
        );

        if (organizations.find(
                organization
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown organization "
                            + organization
            );
        }
    }

    private record Key(
            NpcId npc,
            OrganizationId organization
    ) {
    }
}