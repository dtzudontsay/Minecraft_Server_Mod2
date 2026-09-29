package dev.dtzudontsay.knownworld.world.reference;

/**
 * Describes how strongly a world-reference definition is grounded
 * in source material.
 *
 * This is deliberately separate from simulation truth.
 *
 * CANON:
 * Explicitly established by source material.
 *
 * HYBRID_CANON:
 * Combined/adapted from book/show/map information without creating
 * a fundamentally new thing.
 *
 * PLAUSIBLE:
 * Added by the mod to fill a useful gap.
 *
 * DISPUTED:
 * Sources or adaptations disagree.
 *
 * UNKNOWN:
 * Intentionally unresolved.
 */
public enum ReferenceProvenance {

    CANON,

    HYBRID_CANON,

    PLAUSIBLE,

    DISPUTED,

    UNKNOWN
}