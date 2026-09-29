package dev.dtzudontsay.knownworld.simulation.social.succession;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;

import java.util.Objects;

public final class ClaimRecord {

    private final ClaimId id;

    private final NpcId claimant;

    private final TitleId title;

    private final ClaimStrength strength;

    private final NpcId inheritedFrom;

    private final long createdTick;

    private boolean active;

    public ClaimRecord(
            ClaimId id,
            NpcId claimant,
            TitleId title,
            ClaimStrength strength,
            NpcId inheritedFrom,
            long createdTick,
            boolean active
    ) {
        this.id =
                Objects.requireNonNull(
                        id,
                        "id"
                );

        this.claimant =
                Objects.requireNonNull(
                        claimant,
                        "claimant"
                );

        this.title =
                Objects.requireNonNull(
                        title,
                        "title"
                );

        this.strength =
                Objects.requireNonNull(
                        strength,
                        "strength"
                );

        if (createdTick < 0) {
            throw new IllegalArgumentException(
                    "createdTick cannot be negative"
            );
        }

        this.inheritedFrom =
                inheritedFrom;

        this.createdTick =
                createdTick;

        this.active =
                active;
    }

    public ClaimId id() {
        return id;
    }

    public NpcId claimant() {
        return claimant;
    }

    public TitleId title() {
        return title;
    }

    public ClaimStrength strength() {
        return strength;
    }

    public NpcId inheritedFrom() {
        return inheritedFrom;
    }

    public long createdTick() {
        return createdTick;
    }

    public boolean active() {
        return active;
    }

    public void deactivate() {
        active =
                false;
    }
}