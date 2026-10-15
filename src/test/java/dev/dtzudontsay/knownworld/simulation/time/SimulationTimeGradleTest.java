
package dev.dtzudontsay.knownworld.simulation.time;

import org.junit.jupiter.api.Test;

/**
 * Gradle / JUnit integration for Batch 20.1.
 *
 * Runs the existing standalone regression suite.
 *
 * The original test class remains executable
 * independently through its main() method.
 */
public final class SimulationTimeGradleTest {

    @Test
    void simulationTimeRegressionSuite() {

        SimulationTimeContractTest.main(
                new String[0]
        );
    }
}
