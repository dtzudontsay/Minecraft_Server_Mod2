
package dev.dtzudontsay.knownworld.simulation.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class AgriculturalYieldModelTest {

    private AgriculturalYieldModel.Inputs inputs(
            long hectares,
            long workers,
            int fertility,
            int weather,
            int season
    ) {
        return new AgriculturalYieldModel.Inputs(
                hectares,
                workers,
                1000L,
                2000L,
                fertility,
                weather,
                season
        );
    }

    @Test
    void normalHarvestUsesLandCapacity() {
        var result = AgriculturalYieldModel.calculate(
                inputs(10L, 10L, 1000, 1000, 1000)
        );

        assertEquals(10000L, result.harvestKg());
        assertEquals(10000L, result.landCapacityKg());
        assertFalse(result.laborLimited());
    }

    @Test
    void laborShortageReducesHarvest() {
        var result = AgriculturalYieldModel.calculate(
                inputs(10L, 3L, 1000, 1000, 1000)
        );

        assertEquals(6000L, result.harvestKg());
        assertTrue(result.laborLimited());
    }

    @Test
    void fertilityReducesProduction() {
        var result = AgriculturalYieldModel.calculate(
                inputs(10L, 10L, 500, 1000, 1000)
        );

        assertEquals(5000L, result.harvestKg());
    }

    @Test
    void favorableWeatherIncreasesProduction() {
        var result = AgriculturalYieldModel.calculate(
                inputs(10L, 10L, 1000, 1500, 1000)
        );

        assertEquals(15000L, result.harvestKg());
    }

    @Test
    void poorSeasonReducesProduction() {
        var result = AgriculturalYieldModel.calculate(
                inputs(10L, 10L, 1000, 1000, 250)
        );

        assertEquals(2500L, result.harvestKg());
    }

    @Test
    void zeroWorkersProduceNoGrain() {
        var result = AgriculturalYieldModel.calculate(
                inputs(10L, 0L, 1000, 1000, 1000)
        );

        assertEquals(0L, result.harvestKg());
    }

    @Test
    void zeroLandProducesNoGrain() {
        var result = AgriculturalYieldModel.calculate(
                inputs(0L, 10L, 1000, 1000, 1000)
        );

        assertEquals(0L, result.harvestKg());
    }

    @Test
    void zeroWeatherModifierMeansCropFailure() {
        var result = AgriculturalYieldModel.calculate(
                inputs(10L, 10L, 1000, 0, 1000)
        );

        assertEquals(0L, result.harvestKg());
    }

    @Test
    void rejectsNegativeInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AgriculturalYieldModel.calculate(
                        inputs(-1L, 10L, 1000, 1000, 1000)
                )
        );
    }

    @Test
    void rejectsInvalidProductivityModifier() {
        assertThrows(
                IllegalArgumentException.class,
                () -> inputs(10L, 10L, 2001, 1000, 1000)
        );
    }

    @Test
    void rejectsCapacityOverflow() {
        assertThrows(
                ArithmeticException.class,
                () -> AgriculturalYieldModel.calculate(
                        new AgriculturalYieldModel.Inputs(
                                Long.MAX_VALUE,
                                2L,
                                2L,
                                1L,
                                1000,
                                1000,
                                1000
                        )
                )
        );
    }

    @Test
    void identicalInputsAreDeterministic() {
        var input = inputs(
                12L, 4L, 1200, 750, 800
        );

        assertEquals(
                AgriculturalYieldModel.calculate(input),
                AgriculturalYieldModel.calculate(input)
        );
    }
}
