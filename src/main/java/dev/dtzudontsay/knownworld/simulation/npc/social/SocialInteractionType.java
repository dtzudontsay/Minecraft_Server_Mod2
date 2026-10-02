package dev.dtzudontsay.knownworld.simulation.npc.social;

/**
 * A small initial vocabulary of social actions.
 *
 * These are NOT personality traits.
 *
 * They are things which actually happen between two characters.
 *
 * Later the decision AI will choose between these actions based on:
 *
 * - goals
 * - personality
 * - memories
 * - existing relationships
 * - political interests
 * - fear
 * - obligations
 * - opportunity
 *
 * For now they provide a deterministic testbed for relationship evolution.
 */
public enum SocialInteractionType {

    HELP(
            0.10,
            0.14,
            0.08,
            -0.02,
            0.10,
            0.50,
            "helped"
    ),

    PRAISE(
            0.07,
            0.03,
            0.08,
            0.00,
            0.06,
            0.25,
            "praised"
    ),

    INSULT(
            -0.12,
            -0.05,
            -0.10,
            0.00,
            0.07,
            0.35,
            "insulted"
    ),

    THREATEN(
            -0.10,
            -0.18,
            -0.05,
            0.28,
            0.08,
            0.60,
            "threatened"
    ),

    BETRAY(
            -0.32,
            -0.55,
            -0.22,
            0.08,
            0.18,
            0.90,
            "betrayed"
    );

    private final double affectionDelta;

    private final double trustDelta;

    private final double respectDelta;

    private final double fearDelta;

    private final double familiarityGain;

    private final double baseImportance;

    private final String summaryVerb;

    SocialInteractionType(
            double affectionDelta,
            double trustDelta,
            double respectDelta,
            double fearDelta,
            double familiarityGain,
            double baseImportance,
            String summaryVerb
    ) {

        this.affectionDelta =
                affectionDelta;

        this.trustDelta =
                trustDelta;

        this.respectDelta =
                respectDelta;

        this.fearDelta =
                fearDelta;

        this.familiarityGain =
                familiarityGain;

        this.baseImportance =
                baseImportance;

        this.summaryVerb =
                summaryVerb;
    }

    public double affectionDelta() {
        return affectionDelta;
    }

    public double trustDelta() {
        return trustDelta;
    }

    public double respectDelta() {
        return respectDelta;
    }

    public double fearDelta() {
        return fearDelta;
    }

    public double familiarityGain() {
        return familiarityGain;
    }

    public double baseImportance() {
        return baseImportance;
    }

    public String summaryVerb() {
        return summaryVerb;
    }
}