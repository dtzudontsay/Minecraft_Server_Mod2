package dev.dtzudontsay.knownworld.simulation.npc;

public enum NpcLifeState {

    ALIVE,

    /**
     * Confirmed dead.
     *
     * The NPC record remains permanently available for history,
     * genealogy, memories and world events.
     */
    DEAD
}