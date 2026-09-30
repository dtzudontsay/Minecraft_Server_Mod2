package dev.dtzudontsay.knownworld.simulation.npc.psychology;

/**
 * Behavioural tendencies derived from several underlying
 * personality/profile dimensions.
 *
 * These are NOT separately persisted character stats.
 *
 * They are calculated from the character's current state whenever
 * another system needs them. This avoids contradictory duplicated
 * data such as a persisted "vengeful tendency" drifting away from
 * the actual vengeance, aggression and vindictiveness values that
 * should produce it.
 *
 * Returned tendency strengths use [0.0, 1.0].
 */
public enum CharacterDerivedTendency {

    DECEITFUL_ADVANCEMENT,

    INTERVENTION,

    IMPULSIVE_CONFRONTATION,

    PEER_CONFORMITY,

    RISK_TAKING,

    MERCY,

    VENGEANCE,

    LOYAL_DUTY,

    POLITICAL_ASSERTIVENESS,

    RELIGIOUS_RECEPTIVENESS,

    CULTURAL_OPENNESS,

    MANIPULATION_SUSCEPTIBILITY
}