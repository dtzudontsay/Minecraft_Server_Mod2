package dev.dtzudontsay.knownworld.simulation.social.dynasty;

/**
 * Identifies the fictional continuity/source family in which a
 * dynasty or lineage exists.
 *
 * One dynasty may belong to multiple continuities.
 *
 * Example:
 * House Stark -> BOOK + TELEVISION
 *
 * This is deliberately separate from ReferenceProvenance:
 *
 * continuity = where the entity exists
 * provenance = how strongly a particular authored statement is supported
 */
public enum DynastyContinuity {

    /**
     * ASOIAF book continuity, including the main novels and
     * canon supplementary written material.
     */
    BOOK,

    /**
     * HBO television continuity.
     */
    TELEVISION,

    /**
     * Official licensed game continuity when the game introduces
     * material not otherwise established in book/television canon.
     */
    OFFICIAL_GAME
}