package dev.dtzudontsay.knownworld.simulation.npc.formation;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Locale;
import java.util.Objects;

public final class UpbringingRecord {

    private final NpcId child;

    private String birthLocationId;

    private String upbringingLocationId;

    private NpcId guardian;

    private NpcId fosterParent;

    private int lastFormationAge;

    public UpbringingRecord(
            NpcId child,
            String birthLocationId,
            String upbringingLocationId,
            NpcId guardian,
            NpcId fosterParent,
            int lastFormationAge
    ) {

        this.child =
                Objects.requireNonNull(
                        child,
                        "child"
                );

        this.birthLocationId =
                normalizeLocation(
                        birthLocationId
                );

        this.upbringingLocationId =
                normalizeLocation(
                        upbringingLocationId
                );

        validateOtherNpc(
                child,
                guardian,
                "guardian"
        );

        validateOtherNpc(
                child,
                fosterParent,
                "fosterParent"
        );

        this.guardian =
                guardian;

        this.fosterParent =
                fosterParent;

        setLastFormationAge(
                lastFormationAge
        );
    }

    public NpcId child() {
        return child;
    }

    public String birthLocationId() {
        return birthLocationId;
    }

    public void setBirthLocationId(
            String birthLocationId
    ) {

        this.birthLocationId =
                normalizeLocation(
                        birthLocationId
                );
    }

    public String upbringingLocationId() {
        return upbringingLocationId;
    }

    public void setUpbringingLocationId(
            String upbringingLocationId
    ) {

        this.upbringingLocationId =
                normalizeLocation(
                        upbringingLocationId
                );
    }

    public NpcId guardian() {
        return guardian;
    }

    public void setGuardian(
            NpcId guardian
    ) {

        validateOtherNpc(
                child,
                guardian,
                "guardian"
        );

        this.guardian =
                guardian;
    }

    public NpcId fosterParent() {
        return fosterParent;
    }

    public void setFosterParent(
            NpcId fosterParent
    ) {

        validateOtherNpc(
                child,
                fosterParent,
                "fosterParent"
        );

        this.fosterParent =
                fosterParent;
    }

    public int lastFormationAge() {
        return lastFormationAge;
    }

    public void setLastFormationAge(
            int lastFormationAge
    ) {

        if (lastFormationAge < -1) {

            throw new IllegalArgumentException(
                    "lastFormationAge cannot be below -1"
            );
        }

        this.lastFormationAge =
                lastFormationAge;
    }

    private static void validateOtherNpc(
            NpcId child,
            NpcId value,
            String name
    ) {

        if (value != null
                && child.equals(
                value
        )) {

            throw new IllegalArgumentException(
                    "Child cannot be their own "
                            + name
            );
        }
    }

    private static String normalizeLocation(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "unknown";
        }

        return value.trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }
}