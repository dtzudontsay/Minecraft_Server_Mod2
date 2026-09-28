package dev.dtzudontsay.knownworld.simulation.npc.relationship;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;

/**
 * One NPC's relationship toward another NPC.
 *
 * Relationships are directional.
 *
 * Eddard may trust Robert more than Robert trusts Eddard.
 */
public final class NpcRelationship {

    private final NpcId subject;
    private final NpcId target;

    private double affection;
    private double trust;
    private double respect;
    private double fear;

    private double familiarity;

    public NpcRelationship(
            NpcId subject,
            NpcId target
    ) {
        this(
                subject,
                target,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }

    public NpcRelationship(
            NpcId subject,
            NpcId target,
            double affection,
            double trust,
            double respect,
            double fear,
            double familiarity
    ) {
        this.subject =
                Objects.requireNonNull(
                        subject,
                        "subject"
                );

        this.target =
                Objects.requireNonNull(
                        target,
                        "target"
                );

        if (subject.equals(target)) {
            throw new IllegalArgumentException(
                    "NPC cannot have a relationship toward itself"
            );
        }

        this.affection =
                signed(
                        affection,
                        "affection"
                );

        this.trust =
                signed(
                        trust,
                        "trust"
                );

        this.respect =
                signed(
                        respect,
                        "respect"
                );

        this.fear =
                unit(
                        fear,
                        "fear"
                );

        this.familiarity =
                unit(
                        familiarity,
                        "familiarity"
                );
    }

    public NpcId subject() {
        return subject;
    }

    public NpcId target() {
        return target;
    }

    public double affection() {
        return affection;
    }

    public double trust() {
        return trust;
    }

    public double respect() {
        return respect;
    }

    public double fear() {
        return fear;
    }

    public double familiarity() {
        return familiarity;
    }

    public void changeAffection(
            double amount
    ) {
        affection =
                clampSigned(
                        affection + amount
                );
    }

    public void changeTrust(
            double amount
    ) {
        trust =
                clampSigned(
                        trust + amount
                );
    }

    public void changeRespect(
            double amount
    ) {
        respect =
                clampSigned(
                        respect + amount
                );
    }

    public void changeFear(
            double amount
    ) {
        fear =
                clampUnit(
                        fear + amount
                );
    }

    public void increaseFamiliarity(
            double amount
    ) {
        familiarity =
                clampUnit(
                        familiarity
                                + Math.max(
                                0.0,
                                amount
                        )
                );
    }

    private static double signed(
            double value,
            String name
    ) {
        if (!Double.isFinite(value)
                || value < -1.0
                || value > 1.0) {

            throw new IllegalArgumentException(
                    name
                            + " must be between -1.0 and 1.0"
            );
        }

        return value;
    }

    private static double unit(
            double value,
            String name
    ) {
        if (!Double.isFinite(value)
                || value < 0.0
                || value > 1.0) {

            throw new IllegalArgumentException(
                    name
                            + " must be between 0.0 and 1.0"
            );
        }

        return value;
    }

    private static double clampSigned(
            double value
    ) {
        return Math.max(
                -1.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    private static double clampUnit(
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