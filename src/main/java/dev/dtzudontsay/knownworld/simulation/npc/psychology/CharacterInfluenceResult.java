package dev.dtzudontsay.knownworld.simulation.npc.psychology;

/**
 * Result of one event-driven influence operation.
 *
 * Useful both for simulation logic and debugging.
 */
public record CharacterInfluenceResult(
        double before,
        double target,
        double after,
        double delta,
        double susceptibility,
        double resistance,
        double convictionResistance,
        double sourceFactor,
        double effectiveStrength
) {

    public boolean changed() {

        return Math.abs(
                delta
        ) > 1.0e-9;
    }
}