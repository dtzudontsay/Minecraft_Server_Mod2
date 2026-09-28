package dev.dtzudontsay.knownworld.simulation.npc;

import java.util.Objects;

/**
 * Persistent identity information for one person.
 *
 * Dynamic simulation state does not belong here.
 */
public record NpcIdentity(
        NpcId id,
        String givenName,
        String familyName,
        NpcSex sex,
        int birthYear
) {

    public NpcIdentity {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(givenName, "givenName");
        Objects.requireNonNull(familyName, "familyName");
        Objects.requireNonNull(sex, "sex");

        givenName = givenName.trim();
        familyName = familyName.trim();

        if (givenName.isEmpty()) {
            throw new IllegalArgumentException(
                    "givenName cannot be empty"
            );
        }
    }

    public String fullName() {
        if (familyName.isEmpty()) {
            return givenName;
        }

        return givenName + " " + familyName;
    }

    public int ageAtYear(int year) {
        return Math.max(
                0,
                year - birthYear
        );
    }
}