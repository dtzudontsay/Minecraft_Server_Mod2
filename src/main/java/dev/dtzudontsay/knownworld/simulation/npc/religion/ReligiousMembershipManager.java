package dev.dtzudontsay.knownworld.simulation.npc.religion;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ReligiousMembershipManager {

    private final NpcRegistry registry;

    private final OrganizationManager organizations;

    private final Map<NpcId, ReligiousMembership> memberships =
            new LinkedHashMap<>();

    public ReligiousMembershipManager(
            NpcRegistry registry,
            OrganizationManager organizations
    ) {

        this.registry =
                Objects.requireNonNull(
                        registry,
                        "registry"
                );

        this.organizations =
                Objects.requireNonNull(
                        organizations,
                        "organizations"
                );
    }

    public synchronized Optional<ReligiousMembership> find(
            NpcId npc
    ) {

        validateNpc(
                npc
        );

        return Optional.ofNullable(
                memberships.get(
                        npc
                )
        );
    }

    public synchronized ReligiousMembership join(
            NpcId npc,
            String orderId,
            OrganizationId organizationId,
            String roleId,
            double commitment
    ) {

        validateNpc(
                npc
        );

        ReligiousOrderDefinition definition =
                ReligiousInstitutionCatalog.get()
                        .find(
                                orderId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown religious institution "
                                                        + orderId
                                        )
                        );

        validateOrganization(
                organizationId
        );

        ReligiousMembership existing =
                memberships.get(
                        npc
                );

        if (existing != null
                && existing.active()
                && definition.exclusiveMembership()) {

            throw new IllegalStateException(
                    "NPC already has an active exclusive religious membership"
            );
        }

        ReligiousMembership membership =
                new ReligiousMembership(
                        npc,
                        definition.id(),
                        organizationId,
                        roleId,
                        commitment,
                        true
                );

        memberships.put(
                npc,
                membership
        );

        return membership;
    }

    public synchronized void leave(
            NpcId npc
    ) {

        validateNpc(
                npc
        );

        ReligiousMembership membership =
                memberships.get(
                        npc
                );

        if (membership != null) {

            membership.setActive(
                    false
            );
        }
    }

    public synchronized void registerLoaded(
            ReligiousMembership membership
    ) {

        Objects.requireNonNull(
                membership,
                "membership"
        );

        validateNpc(
                membership.npc()
        );

        ReligiousOrderDefinition definition =
                ReligiousInstitutionCatalog.get()
                        .find(
                                membership.orderId()
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown religious institution "
                                                        + membership.orderId()
                                        )
                        );

        validateOrganization(
                membership.organizationId()
        );

        memberships.put(
                membership.npc(),
                membership
        );
    }

    public synchronized List<ReligiousMembership> activeForOrder(
            String orderId
    ) {

        String normalized =
                ReligiousInstitutionCatalog.get()
                        .find(
                                orderId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown religious institution "
                                                        + orderId
                                        )
                        )
                        .id();

        return memberships.values()
                .stream()
                .filter(
                        ReligiousMembership::active
                )
                .filter(
                        membership ->
                                membership.orderId()
                                        .equals(
                                                normalized
                                        )
                )
                .toList();
    }

    public synchronized List<ReligiousMembership> activeForOrganization(
            OrganizationId organizationId
    ) {

        validateOrganization(
                organizationId
        );

        return memberships.values()
                .stream()
                .filter(
                        ReligiousMembership::active
                )
                .filter(
                        membership ->
                                membership.organizationId()
                                        .equals(
                                                organizationId
                                        )
                )
                .toList();
    }

    public synchronized List<ReligiousMembership> all() {

        return List.copyOf(
                memberships.values()
        );
    }

    public synchronized int size() {

        return memberships.size();
    }

    private void validateNpc(
            NpcId npc
    ) {

        Objects.requireNonNull(
                npc,
                "npc"
        );

        if (!registry.contains(
                npc
        )) {

            throw new IllegalArgumentException(
                    "Unknown NPC "
                            + npc
            );
        }
    }

    private void validateOrganization(
            OrganizationId organizationId
    ) {

        Objects.requireNonNull(
                organizationId,
                "organizationId"
        );

        Organization organization =
                organizations.find(
                                organizationId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Unknown organization "
                                                        + organizationId
                                        )
                        );

        if (organization.type()
                != OrganizationType.RELIGIOUS_ORDER) {

            throw new IllegalArgumentException(
                    "Organization #"
                            + organizationId
                            + " is "
                            + organization.type()
                            + ", expected RELIGIOUS_ORDER"
            );
        }
    }
}