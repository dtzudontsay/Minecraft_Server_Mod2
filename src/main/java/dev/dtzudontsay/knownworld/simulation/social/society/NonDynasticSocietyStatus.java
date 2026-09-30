package dev.dtzudontsay.knownworld.simulation.social.society;

public enum NonDynasticSocietyStatus {

    /**
     * Functioning social organization.
     */
    ACTIVE,

    /**
     * Currently assembling from several smaller groups.
     */
    FORMING,

    /**
     * Temporarily inactive without being historically destroyed.
     */
    DORMANT,

    /**
     * Former society which has broken into successor groups.
     */
    SPLINTERED,

    /**
     * Society no longer exists.
     */
    DISSOLVED
}