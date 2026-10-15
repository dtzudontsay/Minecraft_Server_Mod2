
package dev.dtzudontsay.knownworld.simulation.economy;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Batch 22.7.
 *
 * Deterministic household grain demand.
 *
 * Uses grams internally to avoid fractional
 * kilogram arithmetic.
 *
 * Requirement values are configurable simulation
 * assumptions, not historical or canonical facts.
 */
public final class HouseholdFoodDemandModel {

    public record Inputs(
            long householdPopulation,
            long days,
            long grainGramsPerPersonPerDay
    ) {
        public Inputs {
            if (householdPopulation < 0L
                    || days < 0L
                    || grainGramsPerPersonPerDay < 0L) {
                throw new IllegalArgumentException(
                        "Food demand inputs must not be negative"
                );
            }
        }
    }

    public record Result(
            long requiredGrams,
            long requiredKg
    ) {
        public Result {
            if (requiredGrams < 0L || requiredKg < 0L) {
                throw new IllegalArgumentException(
                        "Food demand result cannot be negative"
                );
            }
        }
    }

    private HouseholdFoodDemandModel() {
    }

    /**
     * Rounds positive demand up to whole kilograms.
     *
     * This is a conservative approximation while
     * InventoryManager tracks grain in kilograms.
     */
    public static Result calculate(Inputs inputs) {
        Objects.requireNonNull(inputs, "inputs");

        BigInteger grams =
                BigInteger.valueOf(
                        inputs.householdPopulation()
                ).multiply(BigInteger.valueOf(
                        inputs.days()
                )).multiply(BigInteger.valueOf(
                        inputs.grainGramsPerPersonPerDay()
                ));

        long exactGrams = grams.longValueExact();

        BigInteger kg = grams
                .add(BigInteger.valueOf(999L))
                .divide(BigInteger.valueOf(1000L));

        return new Result(
                exactGrams,
                kg.longValueExact()
        );
    }
}
