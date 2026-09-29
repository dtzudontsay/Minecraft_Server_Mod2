package dev.dtzudontsay.knownworld.simulation.social.succession;

/**
 * Current automatic hereditary succession rules.
 *
 * More systems such as elective succession, designation,
 * legitimization, bastardy and realm-specific laws can be added later.
 */
public enum SuccessionLaw {

    /**
     * No automatic hereditary transfer.
     *
     * Useful for appointed offices such as Hand of the King.
     */
    NONE,

    /**
     * Sons and their descendants precede daughters and their descendants.
     */
    MALE_PREFERENCE_PRIMOGENITURE,

    /**
     * Oldest child line inherits regardless of sex.
     */
    ABSOLUTE_PRIMOGENITURE,

    /**
     * Only male-line descendants are eligible.
     */
    MALE_ONLY_PRIMOGENITURE
}