package dev.dtzudontsay.knownworld.simulation.npc.psychology;

/**
 * Describes the broad route through which psychological/social
 * influence reaches a character.
 *
 * This is intentionally independent from any particular culture,
 * religion or region.
 *
 * Batch 18C can therefore say:
 *
 *     "The North applies a traditionalism pressure through REGIONAL"
 *
 * while this psychology layer decides how strongly the individual
 * actually absorbs that pressure.
 */
public enum CharacterInfluenceChannel {

    PARENTAL(
            1.15
    ),

    HOUSEHOLD(
            1.10
    ),

    GUARDIAN(
            1.10
    ),

    EDUCATION(
            1.00
    ),

    PEER(
            0.90
    ),

    REGIONAL(
            0.70
    ),

    CULTURAL(
            0.85
    ),

    RELIGIOUS(
            0.85
    ),

    INSTITUTIONAL(
            0.80
    ),

    DIRECT_PERSUASION(
            1.00
    ),

    COERCION(
            0.80
    ),

    EVENT(
            1.00
    );

    private final double strengthMultiplier;

    CharacterInfluenceChannel(
            double strengthMultiplier
    ) {
        this.strengthMultiplier =
                strengthMultiplier;
    }

    public double strengthMultiplier() {
        return strengthMultiplier;
    }
}