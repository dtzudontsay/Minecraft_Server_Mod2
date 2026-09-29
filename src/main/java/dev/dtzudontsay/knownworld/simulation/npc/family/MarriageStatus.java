package dev.dtzudontsay.knownworld.simulation.npc.family;

public enum MarriageStatus {

    /**
     * Formal promise to marry.
     */
    BETROTHED,

    /**
     * Active marriage.
     */
    MARRIED,

    /**
     * Historical union which is no longer active.
     *
     * Later we can add more detailed end reasons such as:
     *
     * - death
     * - annulment
     * - divorce
     */
    ENDED
}