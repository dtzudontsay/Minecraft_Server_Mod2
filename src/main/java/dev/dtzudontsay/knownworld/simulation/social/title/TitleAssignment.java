package dev.dtzudontsay.knownworld.simulation.social.title;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;

/**
 * Historical assignment of one title to one NPC.
 *
 * revokedTick == null means the assignment is currently active.
 *
 * We deliberately preserve revoked assignments because title history
 * will later matter for succession, politics and historical context.
 */
public final class TitleAssignment {

    private final TitleId title;

    private final NpcId holder;

    private final long grantedTick;

    private Long revokedTick;

    public TitleAssignment(
            TitleId title,
            NpcId holder,
            long grantedTick,
            Long revokedTick
    ) {
        this.title =
                Objects.requireNonNull(
                        title,
                        "title"
                );

        this.holder =
                Objects.requireNonNull(
                        holder,
                        "holder"
                );

        if (grantedTick < 0) {
            throw new IllegalArgumentException(
                    "grantedTick cannot be negative"
            );
        }

        if (revokedTick != null
                && revokedTick < grantedTick) {

            throw new IllegalArgumentException(
                    "revokedTick cannot be earlier than grantedTick"
            );
        }

        this.grantedTick =
                grantedTick;

        this.revokedTick =
                revokedTick;
    }

    public TitleId title() {
        return title;
    }

    public NpcId holder() {
        return holder;
    }

    public long grantedTick() {
        return grantedTick;
    }

    public Long revokedTick() {
        return revokedTick;
    }

    public boolean isActive() {
        return revokedTick == null;
    }

    public void revoke(
            long tick
    ) {
        if (tick < grantedTick) {
            throw new IllegalArgumentException(
                    "Revocation tick cannot precede grant tick"
            );
        }

        if (revokedTick == null) {
            revokedTick =
                    tick;
        }
    }
}