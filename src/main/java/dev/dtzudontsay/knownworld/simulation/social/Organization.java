package dev.dtzudontsay.knownworld.simulation.social;

import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.Objects;

/**
 * Persistent social organization.
 *
 * Organizations can optionally have a home/base settlement.
 */
public final class Organization {

    private final OrganizationId id;

    private final String name;

    private final OrganizationType type;

    private final SettlementId seatSettlement;

    public Organization(
            OrganizationId id,
            String name,
            OrganizationType type,
            SettlementId seatSettlement
    ) {
        this.id =
                Objects.requireNonNull(
                        id,
                        "id"
                );

        Objects.requireNonNull(
                name,
                "name"
        );

        this.name =
                name.trim();

        if (this.name.isEmpty()) {
            throw new IllegalArgumentException(
                    "Organization name cannot be empty"
            );
        }

        this.type =
                Objects.requireNonNull(
                        type,
                        "type"
                );

        this.seatSettlement =
                seatSettlement;
    }

    public OrganizationId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public OrganizationType type() {
        return type;
    }

    public SettlementId seatSettlement() {
        return seatSettlement;
    }
}