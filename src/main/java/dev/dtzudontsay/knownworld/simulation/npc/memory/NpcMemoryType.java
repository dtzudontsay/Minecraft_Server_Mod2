package dev.dtzudontsay.knownworld.simulation.npc.memory;

/**
 * Broad categories of persistent NPC memory.
 *
 * These categories will later help us select relevant memories for
 * decision-making and LLM context construction.
 */
public enum NpcMemoryType {

    /**
     * The NPC personally experienced or observed something.
     */
    PERSONAL_EXPERIENCE,

    /**
     * Somebody told this NPC information.
     */
    HEARD_INFORMATION,

    /**
     * This NPC told somebody else information.
     */
    TOLD_INFORMATION,

    /**
     * Social interaction with another person.
     */
    SOCIAL_INTERACTION,

    /**
     * Important combat or military experience.
     */
    COMBAT,

    /**
     * Death of somebody relevant to the NPC.
     */
    DEATH,

    /**
     * Political/social event.
     */
    POLITICAL,

    /**
     * Generic fallback for memories that do not yet have a more
     * specific category.
     */
    GENERAL
}