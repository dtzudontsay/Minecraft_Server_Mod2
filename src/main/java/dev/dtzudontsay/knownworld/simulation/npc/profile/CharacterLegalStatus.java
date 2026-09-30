package dev.dtzudontsay.knownworld.simulation.npc.profile;

/**
 * Legacy single-axis legal status.
 *
 * @deprecated Batch 18H introduces the multi-axis legal-state system in
 *             simulation.npc.legal. Existing profile saves and authored
 *             character data continue to use this enum for compatibility.
 *
 * New gameplay systems should use:
 *
 * - CharacterBirthStatus
 * - CharacterFreedomStatus
 * - CharacterCustodyStatus
 * - CharacterCivilStatus
 */
@Deprecated
public enum CharacterLegalStatus {

    UNSPECIFIED,

    FREE,

    FREEBORN,

    FREEDPERSON,

    ENSLAVED,

    THRALL,

    LEGITIMATE,

    ACKNOWLEDGED_BASTARD,

    UNACKNOWLEDGED_BASTARD,

    LEGITIMIZED,

    DISINHERITED,

    OUTLAW,

    EXILE,

    HOSTAGE,

    WARD,

    PRISONER,

    CONDEMNED
}