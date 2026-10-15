
package dev.dtzudontsay.knownworld.simulation.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class HouseholdFoodDemandModelTest {

    @Test
    void calculatesPopulationDemand() {
        var result = HouseholdFoodDemandModel.calculate(
                new HouseholdFoodDemandModel.Inputs(
                        5L, 10L, 800L
                )
        );

        assertEquals(40000L, result.requiredGrams());
        assertEquals(40L, result.requiredKg());
    }

    @Test
    void roundsFractionalKilogramsUp() {
        var result = HouseholdFoodDemandModel.calculate(
                new HouseholdFoodDemandModel.Inputs(
                        1L, 1L, 750L
                )
        );

        assertEquals(750L, result.requiredGrams());
        assertEquals(1L, result.requiredKg());
    }

    @Test
    void zeroPopulationNeedsNoFood() {
        var result = HouseholdFoodDemandModel.calculate(
                new HouseholdFoodDemandModel.Inputs(
                        0L, 30L, 800L
                )
        );

        assertEquals(0L, result.requiredKg());
    }

    @Test
    void zeroDaysNeedsNoFood() {
        var result = HouseholdFoodDemandModel.calculate(
                new HouseholdFoodDemandModel.Inputs(
                        10L, 0L, 800L
                )
        );

        assertEquals(0L, result.requiredKg());
    }

    @Test
    void oneKilogramRequiresNoRounding() {
        var result = HouseholdFoodDemandModel.calculate(
                new HouseholdFoodDemandModel.Inputs(
                        2L, 1L, 500L
                )
        );

        assertEquals(1L, result.requiredKg());
    }

    @Test
    void rejectsNegativePopulation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HouseholdFoodDemandModel.Inputs(
                        -1L, 1L, 800L
                )
        );
    }

    @Test
    void rejectsNegativeDays() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HouseholdFoodDemandModel.Inputs(
                        1L, -1L, 800L
                )
        );
    }

    @Test
    void rejectsNegativeRequirement() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HouseholdFoodDemandModel.Inputs(
                        1L, 1L, -1L
                )
        );
    }

    @Test
    void rejectsDemandOverflow() {
        assertThrows(
                ArithmeticException.class,
                () -> HouseholdFoodDemandModel.calculate(
                        new HouseholdFoodDemandModel.Inputs(
                                Long.MAX_VALUE,
                                2L,
                                1000L
                        )
                )
        );
    }

    @Test
    void demandIncreasesWithPopulation() {
        var small = HouseholdFoodDemandModel.calculate(
                new HouseholdFoodDemandModel.Inputs(
                        2L, 10L, 800L
                )
        );

        var large = HouseholdFoodDemandModel.calculate(
                new HouseholdFoodDemandModel.Inputs(
                        4L, 10L, 800L
                )
        );

        assertTrue(
                large.requiredKg() > small.requiredKg()
        );
    }
}
