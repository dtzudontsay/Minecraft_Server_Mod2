
package dev.dtzudontsay.knownworld.simulation.testing;

import dev.dtzudontsay.knownworld.simulation.random.SimulationRandom;
import dev.dtzudontsay.knownworld.simulation.random.SimulationRandomContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

public final class SimulationDeterminismTest {

    @Test
    void identicalContextsProduceIdenticalSequences() {
        SplittableRandom first = SimulationRandom.stream(
                12345L, "economy", "winterfell",
                108770L, "harvest"
        );

        SplittableRandom second = SimulationRandom.stream(
                12345L, "economy", "winterfell",
                108770L, "harvest"
        );

        for (int i = 0; i < 100; i++) {
            assertEquals(first.nextLong(), second.nextLong());
        }
    }

    @Test
    void entityIdentityChangesSeed() {
        long first = SimulationRandom.derivedSeed(
                99L, "npc", "character_a",
                500L, "decision"
        );

        long second = SimulationRandom.derivedSeed(
                99L, "npc", "character_b",
                500L, "decision"
        );

        assertNotEquals(first, second);
    }

    @Test
    void operationChangesSeed() {
        long first = SimulationRandom.derivedSeed(
                99L, "npc", "character_a",
                500L, "marriage"
        );

        long second = SimulationRandom.derivedSeed(
                99L, "npc", "character_a",
                500L, "employment"
        );

        assertNotEquals(first, second);
    }

    @Test
    void campaignDayChangesSeed() {
        long first = SimulationRandom.derivedSeed(
                99L, "npc", "character_a",
                500L, "decision"
        );

        long second = SimulationRandom.derivedSeed(
                99L, "npc", "character_a",
                501L, "decision"
        );

        assertNotEquals(first, second);
    }

    @Test
    void contextMatchesDirectSeedDerivation() {
        SimulationRandomContext context =
                new SimulationRandomContext(
                        123L,
                        "politics",
                        "house_stark",
                        1000L,
                        "monthly_strategy"
                );

        long expected = SimulationRandom.derivedSeed(
                123L,
                "politics",
                "house_stark",
                1000L,
                "monthly_strategy"
        );

        assertEquals(expected, context.seed());

        assertEquals(
                new SplittableRandom(expected).nextLong(),
                context.createStream().nextLong()
        );
    }

    @Test
    void differentDaysProduceDifferentContexts() {
        SimulationRandomContext context =
                new SimulationRandomContext(
                        123L, "npc", "character_a",
                        1000L, "decision"
                );

        assertNotEquals(
                context.seed(),
                context.onDay(1001L).seed()
        );
    }

    @Test
    void fingerprintsAreRepeatable() {
        List<String> records = List.of(
                "npc:001:alive",
                "npc:002:alive",
                "holding:winterfell:stark"
        );

        assertEquals(
                SimulationStateFingerprint.fingerprint(records),
                SimulationStateFingerprint.fingerprint(records)
        );
    }

    @Test
    void changedStateChangesFingerprint() {
        String before = SimulationStateFingerprint.fingerprint(
                List.of("npc:001:alive", "npc:002:alive")
        );

        String after = SimulationStateFingerprint.fingerprint(
                List.of("npc:001:alive", "npc:002:dead")
        );

        assertNotEquals(before, after);
    }

    @Test
    void recordOrderingAffectsFingerprint() {
        String first = SimulationStateFingerprint.fingerprint(
                List.of("a", "b")
        );

        String second = SimulationStateFingerprint.fingerprint(
                List.of("b", "a")
        );

        assertNotEquals(first, second);
    }

    @Test
    void rejectsInvalidContexts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SimulationRandomContext(
                        123L, "", "npc_1",
                        100L, "decision"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> SimulationRandom.derivedSeed(
                        123L, "npc", "npc_1",
                        -1L, "decision"
                )
        );
    }

    @Test
    void rejectsNullFingerprintRecords() {
        assertThrows(
                NullPointerException.class,
                () -> SimulationStateFingerprint.fingerprint(
                        java.util.Arrays.asList("valid", null)
                )
        );
    }
}
