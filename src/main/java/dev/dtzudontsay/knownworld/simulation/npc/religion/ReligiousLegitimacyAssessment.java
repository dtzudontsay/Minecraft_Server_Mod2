package dev.dtzudontsay.knownworld.simulation.npc.religion;

public record ReligiousLegitimacyAssessment(
        String expectedReligionId,
        String characterReligionId,
        double faithAlignment,
        double devotionSupport,
        double observanceSupport,
        double institutionalStanding,
        double totalSupport
) {

    public boolean sameReligion() {

        return expectedReligionId.equals(
                characterReligionId
        );
    }
}