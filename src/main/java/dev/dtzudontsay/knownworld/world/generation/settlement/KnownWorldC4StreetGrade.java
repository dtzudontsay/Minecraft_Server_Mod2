package dev.dtzudontsay.knownworld.world.generation.settlement;

import java.util.Arrays;

/** Pure, deterministic terrain/entrance grade arithmetic; no Minecraft bootstrap. */
public final class KnownWorldC4StreetGrade {
    private KnownWorldC4StreetGrade() { }

    /**
     * Mildly level terrain noise, then taper towards an actual approved door.
     * Never excavates or fills more than four blocks relative to natural ground.
     * This is bounded terracing, not a bridge or an arbitrary mountain tunnel.
     */
    public static int height(int natural, int[] samples, int entranceFloor, int stepsToEntrance) {
        if (samples == null || samples.length != 5)
            throw new IllegalArgumentException("Five cross-shaped terrain samples required");
        int[] ordered = samples.clone();
        Arrays.sort(ordered);
        int result = clamp(ordered[2], natural - 2, natural + 2);
        if (stepsToEntrance >= 0 && stepsToEntrance <= 5) {
            int desired = clamp(entranceFloor, natural - 4, natural + 4);
            int allowedTurn = 5 - stepsToEntrance;
            result += clamp(desired - result, -allowedTurn, allowedTurn);
        }
        return clamp(result, natural - 4, natural + 4);
    }

    public static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
