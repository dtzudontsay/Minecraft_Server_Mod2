package dev.dtzudontsay.knownworld.simulation.social.membership;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.OrganizationId;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class OrganizationMembership {

    private final NpcId npc;

    private final OrganizationId organization;

    private final long joinedTick;

    private long leftTick;

    private OrganizationMembershipStatus status;

    private double loyalty;

    private final Set<String> roles =
            new LinkedHashSet<>();

    public OrganizationMembership(
            NpcId npc,
            OrganizationId organization,
            long joinedTick,
            long leftTick,
            OrganizationMembershipStatus status,
            double loyalty,
            Set<String> roles
    ) {

        this.npc =
                Objects.requireNonNull(
                        npc,
                        "npc"
                );

        this.organization =
                Objects.requireNonNull(
                        organization,
                        "organization"
                );

        this.joinedTick =
                Math.max(
                        0,
                        joinedTick
                );

        this.leftTick =
                leftTick;

        this.status =
                Objects.requireNonNull(
                        status,
                        "status"
                );

        this.loyalty =
                clamp01(
                        loyalty
                );

        if (roles != null) {

            for (
                    String role :
                    roles
            ) {

                addRole(
                        role
                );
            }
        }
    }

    public NpcId npc() {
        return npc;
    }

    public OrganizationId organization() {
        return organization;
    }

    public long joinedTick() {
        return joinedTick;
    }

    public long leftTick() {
        return leftTick;
    }

    public boolean hasLeft() {
        return leftTick >= 0;
    }

    public OrganizationMembershipStatus status() {
        return status;
    }

    public boolean isActive() {

        return status
                == OrganizationMembershipStatus.ACTIVE;
    }

    public double loyalty() {
        return loyalty;
    }

    public void setLoyalty(
            double loyalty
    ) {

        this.loyalty =
                clamp01(
                        loyalty
                );
    }

    public Set<String> roles() {

        return Set.copyOf(
                roles
        );
    }

    public void addRole(
            String role
    ) {

        if (role == null
                || role.isBlank()) {

            return;
        }

        roles.add(
                normalizeRole(
                        role
                )
        );
    }

    public void removeRole(
            String role
    ) {

        if (role == null
                || role.isBlank()) {

            return;
        }

        roles.remove(
                normalizeRole(
                        role
                )
        );
    }

    public void leave(
            long tick,
            OrganizationMembershipStatus reason
    ) {

        if (reason
                == OrganizationMembershipStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "Leaving membership cannot use ACTIVE status"
            );
        }

        this.status =
                Objects.requireNonNull(
                        reason,
                        "reason"
                );

        this.leftTick =
                Math.max(
                        joinedTick,
                        tick
                );
    }

    private static String normalizeRole(
            String role
    ) {

        String result =
                role.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (!result.matches(
                "[a-z0-9_.\\-]+"
        )) {

            throw new IllegalArgumentException(
                    "Invalid organization membership role "
                            + role
            );
        }

        return result;
    }

    private static double clamp01(
            double value
    ) {

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}