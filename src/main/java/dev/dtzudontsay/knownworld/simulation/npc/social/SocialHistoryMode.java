package dev.dtzudontsay.knownworld.simulation.npc.social;

/**
 * Controls how much permanent historical detail an interaction creates.
 *
 * Relationship state always changes.
 *
 * FULL:
 *      Store event + memories.
 *
 * SIGNIFICANT_ONLY:
 *      Store only important interactions.
 *
 * NONE:
 *      Change simulation state but do not create permanent event/memory spam.
 *
 * NONE is important for very long stress simulations.
 */
public enum SocialHistoryMode {

    FULL,

    SIGNIFICANT_ONLY,

    NONE
}