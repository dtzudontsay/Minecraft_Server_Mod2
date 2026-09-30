package dev.dtzudontsay.knownworld.simulation.social.title;

/**
 * Broad categories of formal titles and offices.
 *
 * These are deliberately generic enough to represent both Westerosi and
 * Essosi government without hard-coding specific cultures into the core
 * title system.
 */
public enum TitleType {

    /**
     * Crowned monarch or dynastic royal title.
     *
     * Examples:
     * King, Queen.
     */
    ROYAL,

    /**
     * Landed or dynastic nobility.
     *
     * Examples:
     * Lord, Lady, Head of House.
     */
    NOBLE,

    /**
     * Sovereign or quasi-sovereign ruler title that is not necessarily
     * royal in the Westerosi sense.
     *
     * Examples:
     * Khal and certain Essosi ruler forms.
     */
    RULER,

    /**
     * Civic or republican leadership position.
     *
     * Examples:
     * Sealord, Triarch, Archon.
     */
    CIVIC,

    /**
     * Appointed governmental office.
     *
     * Example:
     * Hand of the King.
     */
    OFFICE,

    /**
     * Military command.
     */
    MILITARY,

    /**
     * Religious leadership or office.
     */
    RELIGIOUS,

    /**
     * Honorific without necessarily conveying direct governing authority.
     */
    HONORARY
}