
package dev.dtzudontsay.knownworld.simulation.testing;

import dev.dtzudontsay.knownworld.simulation.random.SimulationRandom;
import dev.dtzudontsay.knownworld.simulation.random.SimulationRandomContext;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Batch 20.2 deterministic contract tests.
 *
 * Does not require Minecraft or Fabric.
 */
public final class SimulationDeterminismTest {

    private SimulationDeterminismTest() {
    }

    public static void main(String[] args) {
        testRepeatability();
        testIndependentEntityStreams();
        testIndependentOperationStreams();
        testCampaignDayVariation();
        testContextEquivalence();
        testFingerprintRepeatability();
        testFingerprintSensitivity();
        testInvalidContext();

        System.out.println(
                "Batch 20.2 determinism tests passed."
        );
    }

    private static void testRepeatability() {
        SplittableRandom first =
                SimulationRandom.stream(
                        12345L,
                        "economy",
                        "winterfell",
                        108770L,
                        "harvest"
                );

        SplittableRandom second =
                SimulationRandom.stream(
                        12345L,
                        "economy",
                        "winterfell",
                        108770L,
                        "harvest"
                );

        for (int i = 0; i < 100; i++) {
            check(
                    first.nextLong()
                            == second.nextLong(),
                    "Identical contexts must replay identically"
            );
        }
    }

    private static void testIndependentEntityStreams() {
        long first = SimulationRandom.derivedSeed(
                99L, "npc", "character_a",
                500L, "decision"
        );

        long second = SimulationRandom.derivedSeed(
                99L, "npc", "character_b",
                500L, "decision"
        );

        check(
                first != second,
                "Different entities should derive different seeds"
        );
    }

    private static void testIndependentOperationStreams() {
        long first = SimulationRandom.derivedSeed(
                99L, "npc", "character_a",
                500L, "marriage"
        );

        long second = SimulationRandom.derivedSeed(
                99L, "npc", "character_a",
                500L, "employment"
        );

        check(
                first != second,
                "Different operations should derive different seeds"
        );
    }

    private static void testCampaignDayVariation() {
        long first = SimulationRandom.derivedSeed(
                99L, "npc", "character_a",
                500L, "decision"
        );

        long second = SimulationRandom.derivedSeed(
                99L, "npc", "character_a",
                501L, "decision"
        );

        check(
                first != second,
                "Different days should derive different seeds"
        );
    }

    private static void testContextEquivalence() {
        SimulationRandomContext context =
                new SimulationRandomContext(
                        123L,
                        "politics",
                        "house_stark",
                        1000L,
                        "monthly_strategy"
                );

        long expected =
                SimulationRandom.derivedSeed(
                        123L,
                        "politics",
                        "house_stark",
                        1000L,
                        "monthly_strategy"
                );

        check(
                context.seed() == expected,
                "Context seed differs from direct derivation"
        );

        check(
                context.createStream().nextLong()
                        == new SplittableRandom(
                        expected
                ).nextLong(),
                "Context stream must match direct construction"
        );

        check(
                context.onDay(1001L).seed()
                        != context.seed(),
                "Changing context day should change seed"
        );
    }

    private static void testFingerprintRepeatability() {
        List<String> records = List.of(
                "npc:001:alive",
                "npc:002:alive",
                "holding:winterfell:stark"
        );

        String first =
                SimulationStateFingerprint.fingerprint(
                        records
                );

        String second =
                SimulationStateFingerprint.fingerprint(
                        records
                );

        check(
                first.equals(second),
                "Identical records must produce identical fingerprints"
        );
    }

    private static void testFingerprintSensitivity() {
        List<String> before = List.of(
                "npc:001:alive",
                "npc:002:alive"
        );

        List<String> after = List.of(
                "npc:001:alive",
                "npc:002:dead"
        );

        String first =
                SimulationStateFingerprint.fingerprint(
                        before
                );

        String second =
                SimulationStateFingerprint.fingerprint(
                        after
                );

        check(
                !first.equals(second),
                "Changed state should change fingerprint"
        );

        List<String> reordered =
                new ArrayList<>(before);

        java.util.Collections.reverse(reordered);

        check(
                !first.equals(
                        SimulationStateFingerprint.fingerprint(
                                reordered
                        )
                ),
                "Fingerprinting requires canonical record ordering"
        );
    }

    private static void testInvalidContext() {
        boolean rejected = false;

        try {
            new SimulationRandomContext(
                    123L,
                    "",
                    "npc_1",
                    100L,
                    "decision"
            );
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }

        check(
                rejected,
                "Blank domain must be rejected"
        );
    }

    private static void check(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
