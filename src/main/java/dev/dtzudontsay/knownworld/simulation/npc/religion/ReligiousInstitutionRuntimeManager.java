package dev.dtzudontsay.knownworld.simulation.npc.religion;

import dev.dtzudontsay.knownworld.simulation.social.Organization;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationManager;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ReligiousInstitutionRuntimeManager {

    private final OrganizationManager organizations;

    private final Map<String, OrganizationId> organizationByOrder =
            new LinkedHashMap<>();

    public ReligiousInstitutionRuntimeManager(
            OrganizationManager organizations
    ) {

        this.organizations =
                Objects.requireNonNull(
                        organizations,
                        "organizations"
                );
    }

    public synchronized Organization ensureOrganization(
            String orderId
    ) {

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

        OrganizationId existingId =
                organizationByOrder.get(
                        definition.id()
                );

        if (existingId != null) {

            return organizations.find(
                            existingId
                    )
                    .orElseThrow(
                            () ->
                                    new IllegalStateException(
                                            "Religious institution "
                                                    + definition.id()
                                                    + " references missing organization "
                                                    + existingId
                                    )
                    );
        }

        Organization organization =
                organizations.create(
                        definition.displayName(),
                        OrganizationType.RELIGIOUS_ORDER,
                        null
                );

        organizationByOrder.put(
                definition.id(),
                organization.id()
        );

        return organization;
    }

    public synchronized void ensureAll() {

        for (
                ReligiousOrderDefinition definition :
                ReligiousInstitutionCatalog.get()
                        .all()
        ) {

            ensureOrganization(
                    definition.id()
            );
        }
    }

    public synchronized void registerBinding(
            String orderId,
            OrganizationId organizationId
    ) {

        Objects.requireNonNull(
                organizationId,
                "organizationId"
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

        OrganizationId previous =
                organizationByOrder.putIfAbsent(
                        definition.id(),
                        organizationId
                );

        if (previous != null
                && !previous.equals(
                organizationId
        )) {

            throw new IllegalStateException(
                    "Religious institution "
                            + definition.id()
                            + " already bound to organization "
                            + previous
            );
        }
    }

    public synchronized Optional<OrganizationId> findOrganizationId(
            String orderId
    ) {

        if (orderId == null
                || orderId.isBlank()) {

            return Optional.empty();
        }

        return Optional.ofNullable(
                organizationByOrder.get(
                        orderId.trim()
                                .toLowerCase()
                )
        );
    }

    public synchronized Optional<Organization> findOrganization(
            String orderId
    ) {

        return findOrganizationId(
                orderId
        )
                .flatMap(
                        organizations::find
                );
    }

    public synchronized Map<String, OrganizationId> bindings() {

        return Map.copyOf(
                organizationByOrder
        );
    }

    public synchronized int size() {

        return organizationByOrder.size();
    }
}