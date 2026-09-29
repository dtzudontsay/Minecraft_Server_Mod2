package dev.dtzudontsay.knownworld.simulation.npc.lifecycle;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;

/**
 * Campaign-scale life history.
 *
 * This exists separately from NpcIdentity.birthYear because generated
 * people need an exact historical birth date.
 */
public final class NpcLifeRecord {

    private final NpcId owner;

    private long birthDay;

    private Long deathDay;

    public NpcLifeRecord(
            NpcId owner,
            long birthDay,
            Long deathDay
    ) {
        this.owner =
                Objects.requireNonNull(
                        owner,
                        "owner"
                );

        if (birthDay < 0) {

            throw new IllegalArgumentException(
                    "birthDay cannot be negative"
            );
        }

        if (deathDay != null
                && deathDay < birthDay) {

            throw new IllegalArgumentException(
                    "deathDay cannot precede birthDay"
            );
        }

        this.birthDay =
                birthDay;

        this.deathDay =
                deathDay;
    }

    public NpcId owner() {
        return owner;
    }

    public long birthDay() {
        return birthDay;
    }

    public Long deathDay() {
        return deathDay;
    }

    public void setBirthDay(
            long birthDay
    ) {
        if (birthDay < 0) {
            throw new IllegalArgumentException(
                    "birthDay cannot be negative"
            );
        }

        if (deathDay != null
                && deathDay < birthDay) {

            throw new IllegalArgumentException(
                    "birthDay cannot be after deathDay"
            );
        }

        this.birthDay =
                birthDay;
    }

    public void markDead(
            long deathDay
    ) {
        if (deathDay < birthDay) {

            throw new IllegalArgumentException(
                    "deathDay cannot precede birthDay"
            );
        }

        this.deathDay =
                deathDay;
    }
}