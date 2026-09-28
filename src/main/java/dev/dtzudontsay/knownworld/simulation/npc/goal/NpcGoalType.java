package dev.dtzudontsay.knownworld.simulation.npc.goal;

/**
 * Broad categories of NPC intention.
 *
 * More specialized systems can later attach additional data to these
 * goals without requiring every behavior to become a new enum value.
 */
public enum NpcGoalType {

    /**
     * Move toward a world-space destination.
     */
    TRAVEL,

    /**
     * Speak with or seek out another NPC.
     */
    TALK_TO,

    /**
     * Investigate a person, location or event.
     */
    INVESTIGATE,

    /**
     * Deliver information to somebody.
     */
    REPORT_INFORMATION,

    /**
     * Protect a person, group or place.
     */
    PROTECT,

    /**
     * Escape perceived danger.
     */
    FLEE,

    /**
     * Engage a hostile target.
     */
    ATTACK,

    /**
     * Perform job/duty activity.
     */
    WORK,

    /**
     * Rest or recover.
     */
    REST,

    /**
     * Generic goal for systems not yet specialized.
     */
    GENERAL
}