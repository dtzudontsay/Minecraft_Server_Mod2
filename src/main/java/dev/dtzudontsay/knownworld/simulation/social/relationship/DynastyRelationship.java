package dev.dtzudontsay.knownworld.simulation.social.relationship;

import dev.dtzudontsay.knownworld.simulation.social.dynasty.DynastyId;

import java.util.Objects;

/**
 * One dynasty/house's relationship toward another dynasty/house.
 *
 * This is directional.
 *
 * House Stark -> House Bolton may differ from
 * House Bolton -> House Stark.
 *
 * Values are normalized simulation values, not literal canon numbers.
 */
public final class DynastyRelationship {

    private final DynastyId subject;
    private final DynastyId target;

    /*
     * Highest authored revision which has been applied to this runtime
     * relationship.
     *
     * Runtime political events can modify the actual values without
     * changing this number.
     */
    private int authoredVersion;

    private double affinity;
    private double trust;
    private double respect;
    private double fear;
    private double familiarity;

    public DynastyRelationship(
            DynastyId subject,
            DynastyId target
    ) {

        this(
                subject,
                target,
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }

    public DynastyRelationship(
            DynastyId subject,
            DynastyId target,
            int authoredVersion,
            double affinity,
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

        if (subject.equals(
                target
        )) {

            throw new IllegalArgumentException(
                    "Dynasty cannot have a relationship toward itself"
            );
        }

        if (authoredVersion < 0) {

            throw new IllegalArgumentException(
                    "authoredVersion cannot be negative"
            );
        }

        this.authoredVersion =
                authoredVersion;

        this.affinity =
                signed(
                        affinity,
                        "affinity"
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

    public DynastyId subject() {
        return subject;
    }

    public DynastyId target() {
        return target;
    }

    public int authoredVersion() {
        return authoredVersion;
    }

    public double affinity() {
        return affinity;
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

    /**
     * Applies a newer authored baseline.
     *
     * An equal or older authoring version never resets a relationship
     * that may already have evolved during gameplay.
     */
    public boolean applyAuthoredRevision(
            int version,
            double newAffinity,
            double newTrust,
            double newRespect,
            double newFear,
            double newFamiliarity
    ) {

        if (version <= authoredVersion) {
            return false;
        }

        authoredVersion =
                version;

        affinity =
                signed(
                        newAffinity,
                        "affinity"
                );

        trust =
                signed(
                        newTrust,
                        "trust"
                );

        respect =
                signed(
                        newRespect,
                        "respect"
                );

        fear =
                unit(
                        newFear,
                        "fear"
                );

        familiarity =
                unit(
                        newFamiliarity,
                        "familiarity"
                );

        return true;
    }

    public void changeAffinity(
            double amount
    ) {

        affinity =
                clampSigned(
                        affinity + amount
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

        if (!Double.isFinite(
                value
        )
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

        if (!Double.isFinite(
                value
        )
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
