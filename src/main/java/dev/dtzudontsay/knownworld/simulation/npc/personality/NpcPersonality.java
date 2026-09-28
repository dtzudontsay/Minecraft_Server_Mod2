package dev.dtzudontsay.knownworld.simulation.npc.personality;

/**
 * Stable personality tendencies belonging to one NPC.
 *
 * Every value is normalized to the range [-1.0, 1.0].
 *
 * These are not temporary emotions.
 *
 * Examples:
 *
 * courage:
 *      -1 = extremely fearful
 *       0 = ordinary
 *      +1 = extremely brave
 *
 * ambition:
 *      -1 = actively avoids status/power
 *      +1 = intensely ambitious
 *
 * compassion:
 *      -1 = cruel/callous tendency
 *      +1 = highly compassionate
 *
 * honor:
 *      -1 = strongly opportunistic/deceitful tendency
 *      +1 = strongly duty/honor-oriented
 *
 * patience:
 *      -1 = impulsive
 *      +1 = patient/deliberate
 *
 * sociability:
 *      -1 = withdrawn
 *      +1 = highly social
 */
public record NpcPersonality(
        double courage,
        double ambition,
        double compassion,
        double honor,
        double patience,
        double sociability
) {

    public static final NpcPersonality NEUTRAL =
            new NpcPersonality(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );

    public NpcPersonality {
        courage =
                validate(
                        courage,
                        "courage"
                );

        ambition =
                validate(
                        ambition,
                        "ambition"
                );

        compassion =
                validate(
                        compassion,
                        "compassion"
                );

        honor =
                validate(
                        honor,
                        "honor"
                );

        patience =
                validate(
                        patience,
                        "patience"
                );

        sociability =
                validate(
                        sociability,
                        "sociability"
                );
    }

    private static double validate(
            double value,
            String name
    ) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    name + " must be finite"
            );
        }

        if (value < -1.0
                || value > 1.0) {

            throw new IllegalArgumentException(
                    name
                            + " must be between -1.0 and 1.0"
            );
        }

        return value;
    }
}