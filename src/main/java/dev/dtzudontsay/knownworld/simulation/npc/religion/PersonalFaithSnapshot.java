package dev.dtzudontsay.knownworld.simulation.npc.religion;

/**
 * Read-only view of one character's current personal faith state.
 *
 * All numeric fields use [0.0, 1.0].
 *
 * Religion identity itself is stored on CharacterProfile.
 */
public record PersonalFaithSnapshot(
        String religionId,
        double devotion,
        double doctrinalKnowledge,
        double identityImportance,
        double tolerance,
        double conversionOpenness,
        double concealmentWillingness,
        double syncretism,
        double observance
) {
}