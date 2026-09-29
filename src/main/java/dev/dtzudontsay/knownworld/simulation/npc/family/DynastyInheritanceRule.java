package dev.dtzudontsay.knownworld.simulation.npc.family;

/**
 * Determines which parent's dynastic identity normally passes to
 * children of a marriage.
 *
 * This currently controls:
 *
 * - family name
 * - noble-house affiliation
 *
 * It does NOT yet determine title succession.
 */
public enum DynastyInheritanceRule {

    /**
     * Father's family name / noble house normally passes to children.
     */
    PATRILINEAL,

    /**
     * Mother's family name / noble house normally passes to children.
     */
    MATRILINEAL
}