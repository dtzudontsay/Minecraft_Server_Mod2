package dev.dtzudontsay.knownworld.simulation.npc.religion;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.npc.NpcRegistry;
import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ReligiousMembershipManager {

    private final NpcRegistry registry;

    private final OrganizationManager organizations;

    /*
     * 19.0C:
     *
     * Previously this map was keyed only by NPC, meaning a character
     * could belong to only one religious institution.
     *
     * Membership is now many-to-many:
     *
     * NPC + religious order -> membership.
     */
    private final Map<Key, ReligiousMembership> memberships =
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

    /**
     * Legacy-compatible lookup.
     *
     * Returns an active membership when possible, otherwise any
     * membership for the NPC.
     */
    public synchronized Optional<ReligiousMembership> find(
            NpcId npc
    ) {

        validateNpc(
                npc
        );

        Optional<ReligiousMembership> active =
                memberships.values()
                        .stream()
                        .filter(
                                membership ->
                                        membership.npc()
                                                .equals(
                                                        npc
                                                )
                        )
                        .filter(
                                ReligiousMembership::active
                        )
                        .findFirst();

        if (active.isPresent()) {
            return active;
        }

        return memberships.values()
                .stream()
                .filter(
                        membership ->
                                membership.npc()
                                        .equals(
                                                npc
                                        )
                )
                .findFirst();
    }

    public synchronized Optional<ReligiousMembership> find(
            NpcId npc,
            String orderId
    ) {

        validateNpc(
                npc
        );

        return Optional.ofNullable(
                memberships.get(
                        new Key(
                                npc,
                                normalizeOrderId(
                                        orderId
                                )
                        )
                )
        );
    }

    public synchronized List<ReligiousMembership> membershipsFor(
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

    public synchronized List<ReligiousMembership> activeMembershipsFor(
            NpcId npc
    ) {

        return membershipsFor(
                npc
        )
                .stream()
                .filter(
                        ReligiousMembership::active
                )
                .toList();
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

        Key key =
                new Key(
                        npc,
                        definition.id()
                );

        ReligiousMembership sameOrder =
                memberships.get(
                        key
                );

        if (sameOrder != null) {

            sameOrder.setRoleId(
                    roleId
            );

            sameOrder.setCommitment(
                    commitment
            );

            sameOrder.setActive(
                    true
            );

            return sameOrder;
        }

        List<ReligiousMembership> activeExisting =
                activeMembershipsFor(
                        npc
                );

        if (definition.exclusiveMembership()
                && !activeExisting.isEmpty()) {

            throw new IllegalStateException(
                    "NPC already has active religious membership(s); "
                            + definition.id()
                            + " is exclusive"
            );
        }

        for (
                ReligiousMembership existing :
                activeExisting
        ) {

            ReligiousOrderDefinition existingDefinition =
                    ReligiousInstitutionCatalog.get()
                            .find(
                                    existing.orderId()
                            )
                            .orElseThrow();

            if (existingDefinition.exclusiveMembership()) {

                throw new IllegalStateException(
                        "NPC is already a member of exclusive religious institution "
                                + existingDefinition.id()
                );
            }
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
                key,
                membership
        );

        return membership;
    }

    /**
     * Legacy behavior: leave all currently active religious
     * institution memberships.
     */
    public synchronized void leave(
            NpcId npc
    ) {

        validateNpc(
                npc
        );

        for (
                ReligiousMembership membership :
                membershipsFor(
                        npc
                )
        ) {

            if (membership.active()) {

                membership.setActive(
                        false
                );
            }
        }
    }

    public synchronized void leave(
            NpcId npc,
            String orderId
    ) {

        find(
                npc,
                orderId
        )
                .ifPresent(
                        membership ->
                                membership.setActive(
                                        false
                                )
                );
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

        Key key =
                new Key(
                        membership.npc(),
                        definition.id()
                );

        if (memberships.putIfAbsent(
                key,
                membership
        ) != null) {

            throw new IllegalStateException(
                    "Duplicate religious membership for NPC "
                            + membership.npc()
                            + " and order "
                            + definition.id()
            );
        }
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

    private static String normalizeOrderId(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "orderId cannot be blank"
            );
        }

        return value.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private record Key(
            NpcId npc,
            String orderId
    ) {
    }
}