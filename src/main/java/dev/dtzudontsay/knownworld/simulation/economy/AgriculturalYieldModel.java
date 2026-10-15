
package dev.dtzudontsay.knownworld.simulation.economy;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Batch 22.7.
 *
 * Deterministic agricultural yield estimator.
 *
 * Uses fixed-point environmental modifiers, where:
 * 1000 = normal
 *  500 = half productivity
 * 1500 = 150 percent productivity
 *
 * All output is measured in kilograms of grain.
 *
 * These are simulation inputs, NOT canonical
 * Westerosi agricultural statistics.
 */
public final class AgriculturalYieldModel {

    public static final int NORMAL = 1000;

    public record Inputs(
            long cultivatedHectares,
            long agriculturalWorkers,
            long grainKgPerHectare,
            long grainKgPerWorker,
            int fertilityPermille,
            int weatherPermille,
            int seasonPermille
    ) {
        public Inputs {
            if (cultivatedHectares < 0L
                    || agriculturalWorkers < 0L
                    || grainKgPerHectare < 0L
                    || grainKgPerWorker < 0L) {
                throw new IllegalArgumentException(
                        "Agricultural quantities must not be negative"
                );
            }

            checkModifier(fertilityPermille);
            checkModifier(weatherPermille);
            checkModifier(seasonPermille);
        }
    }

    public record Result(
            long landCapacityKg,
            long laborCapacityKg,
            long harvestKg,
            boolean laborLimited
    ) {
        public Result {
            if (landCapacityKg < 0L
                    || laborCapacityKg < 0L
                    || harvestKg < 0L) {
                throw new IllegalArgumentException(
                        "Yield result cannot be negative"
                );
            }
        }
    }

    private AgriculturalYieldModel() {
    }

    /**
     * Capacity model:
     *
     * Land capacity = area * yield / hectare
     * Labor capacity = workers * output / worker
     *
     * Environmental modifiers apply to the smaller
     * capacity. Each modifier uses integer permille.
     */
    public static Result calculate(Inputs inputs) {
        Objects.requireNonNull(inputs, "inputs");

        long landCapacity = Math.multiplyExact(
                inputs.cultivatedHectares(),
                inputs.grainKgPerHectare()
        );

        long laborCapacity = Math.multiplyExact(
                inputs.agriculturalWorkers(),
                inputs.grainKgPerWorker()
        );

        long base = Math.min(
                landCapacity,
                laborCapacity
        );

        BigInteger adjusted = BigInteger.valueOf(base)
                .multiply(BigInteger.valueOf(
                        inputs.fertilityPermille()
                ))
                .multiply(BigInteger.valueOf(
                        inputs.weatherPermille()
                ))
                .multiply(BigInteger.valueOf(
                        inputs.seasonPermille()
                ))
                .divide(BigInteger.valueOf(
                        1_000_000_000L
                ));

        long harvest = adjusted.longValueExact();

        return new Result(
                landCapacity,
                laborCapacity,
                harvest,
                laborCapacity < landCapacity
        );
    }

    private static void checkModifier(int modifier) {
        if (modifier < 0 || modifier > 2000) {
            throw new IllegalArgumentException(
                    "Productivity modifier must be 0 to 2000"
            );
        }
    }
}
