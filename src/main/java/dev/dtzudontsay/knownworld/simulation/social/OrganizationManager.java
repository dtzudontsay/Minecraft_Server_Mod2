package dev.dtzudontsay.knownworld.simulation.social;

import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementManager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class OrganizationManager {

    private final SettlementManager settlements;

    private final Map<OrganizationId, Organization> organizations =
            new LinkedHashMap<>();

    private long nextId =
            1L;

    public OrganizationManager(
            SettlementManager settlements
    ) {
        this.settlements =
                Objects.requireNonNull(
                        settlements,
                        "settlements"
                );
    }

    public synchronized Organization create(
            String name,
            OrganizationType type,
            SettlementId seatSettlement
    ) {
        validateSettlementIfPresent(
                seatSettlement
        );

        Organization organization =
                new Organization(
                        allocateId(),
                        name,
                        type,
                        seatSettlement
                );

        organizations.put(
                organization.id(),
                organization
        );

        return organization;
    }

    public synchronized void registerLoaded(
            Organization organization
    ) {
        Objects.requireNonNull(
                organization,
                "organization"
        );

        validateSettlementIfPresent(
                organization.seatSettlement()
        );

        if (organizations.containsKey(
                organization.id()
        )) {
            throw new IllegalStateException(
                    "Duplicate organization ID: "
                            + organization.id()
            );
        }

        organizations.put(
                organization.id(),
                organization
        );

        if (organization.id().value()
                == Long.MAX_VALUE) {

            nextId =
                    Long.MAX_VALUE;

        } else {

            nextId =
                    Math.max(
                            nextId,
                            organization.id()
                                    .value()
                                    + 1
                    );
        }
    }

    public synchronized Optional<Organization> find(
            OrganizationId id
    ) {
        return Optional.ofNullable(
                organizations.get(
                        id
                )
        );
    }

    public synchronized Collection<Organization> all() {
        return List.copyOf(
                organizations.values()
        );
    }

    public synchronized int size() {
        return organizations.size();
    }

    private void validateSettlementIfPresent(
            SettlementId settlement
    ) {
        if (settlement != null
                && settlements.find(
                settlement
        ).isEmpty()) {

            throw new IllegalArgumentException(
                    "Unknown settlement ID: "
                            + settlement
            );
        }
    }

    private OrganizationId allocateId() {

        if (nextId <= 0
                || nextId == Long.MAX_VALUE) {

            throw new IllegalStateException(
                    "Organization ID space exhausted"
            );
        }

        OrganizationId id =
                new OrganizationId(
                        nextId
                );

        nextId++;

        return id;
    }
}