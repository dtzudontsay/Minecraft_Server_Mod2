package dev.dtzudontsay.knownworld.simulation.npc.legal;

public enum CharacterFreedomStatus {

    UNSPECIFIED,

    /**
     * Born legally free.
     */
    FREEBORN,

    /**
     * Free, but exact origin is not otherwise encoded.
     */
    FREE,

    /**
     * Formerly enslaved and now legally free.
     */
    FREEDPERSON,

    /**
     * Chattel slavery or equivalent enslavement.
     */
    ENSLAVED,

    /**
     * Ironborn thralldom.
     *
     * Intentionally distinct from slavery.
     */
    THRALL
}