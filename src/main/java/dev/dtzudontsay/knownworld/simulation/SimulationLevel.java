package dev.dtzudontsay.knownworld.simulation;

/**
 * Describes how much simulation detail an object currently receives.
 *
 * IMPORTANT:
 * This is independent from whether a Minecraft Entity currently exists.
 */
public enum SimulationLevel {

    /**
     * Very distant / currently irrelevant.
     *
     * Updates can be extremely coarse.
     */
    DORMANT,

    /**
     * Off-screen, but actively participating in the world simulation.
     *
     * Travel, work, orders, battles and other events may be resolved
     * abstractly.
     */
    ABSTRACT,

    /**
     * Relevant to nearby world activity.
     *
     * Receives more detailed simulation without necessarily requiring
     * a physical Minecraft entity.
     */
    DETAILED,

    /**
     * Represented physically in the loaded Minecraft world.
     */
    PHYSICAL
}