package dev.dtzudontsay.knownworld.simulation.npc.religion;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;

import java.util.Locale;
import java.util.Objects;

public final class ReligiousMembership {

    private final NpcId npc;

    private final OrganizationId organizationId;

    private String orderId;

    private String roleId;

    private double commitment;

    private boolean active;

    public ReligiousMembership(
            NpcId npc,
            String orderId,
            OrganizationId organizationId,
            String roleId,
            double commitment,
            boolean active
    ) {

        this.npc =
                Objects.requireNonNull(
                        npc,
                        "npc"
                );

        this.orderId =
                normalizeId(
                        orderId
                );

        this.organizationId =
                Objects.requireNonNull(
                        organizationId,
                        "organizationId"
                );

        this.roleId =
                normalizeOptionalId(
                        roleId
                );

        setCommitment(
                commitment
        );

        this.active =
                active;
    }

    public NpcId npc() {
        return npc;
    }

    public String orderId() {
        return orderId;
    }

    public void setOrderId(
            String orderId
    ) {

        this.orderId =
                normalizeId(
                        orderId
                );
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public String roleId() {
        return roleId;
    }

    public void setRoleId(
            String roleId
    ) {

        this.roleId =
                normalizeOptionalId(
                        roleId
                );
    }

    public double commitment() {
        return commitment;
    }

    public void setCommitment(
            double commitment
    ) {

        if (!Double.isFinite(
                commitment
        )) {

            throw new IllegalArgumentException(
                    "commitment must be finite"
            );
        }

        this.commitment =
                Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                commitment
                        )
                );
    }

    public boolean active() {
        return active;
    }

    public void setActive(
            boolean active
    ) {

        this.active =
                active;
    }

    private static String normalizeId(
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

    private static String normalizeOptionalId(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "";
        }

        return value.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }
}