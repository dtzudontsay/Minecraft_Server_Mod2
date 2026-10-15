
package dev.dtzudontsay.knownworld.simulation.testing;

import dev.dtzudontsay.knownworld.simulation.random.SimulationRandom;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;

/**
 * Batch 20.2
 *
 * Deterministic fingerprinting of canonical
 * simulation state.
 *
 * This tool does not determine which fields are
 * authoritative. Domain-specific canonical
 * serializers will be added in later batches.
 */
public final class SimulationStateFingerprint {

    public static final int FORMAT_VERSION = 1;

    private static final long OFFSET =
            0xcbf29ce484222325L;

    private static final long PRIME =
            0x100000001b3L;

    private SimulationStateFingerprint() {
    }

    /**
     * Fingerprint an ordered sequence of records.
     *
     * The caller must supply a stable ordering.
     * For entities this usually means sorting by
     * stable entity ID before serialization.
     *
     * Records must use a canonical representation:
     * no locale-sensitive formatting, unstable map
     * ordering, or environment-specific text.
     */
    public static String fingerprint(
            Collection<String> canonicalRecords
    ) {
        Objects.requireNonNull(
                canonicalRecords,
                "canonicalRecords"
        );

        long hash = OFFSET;

        hash = updateLong(
                hash,
                FORMAT_VERSION
        );

        hash = updateLong(
                hash,
                canonicalRecords.size()
        );

        for (String record : canonicalRecords) {
            Objects.requireNonNull(
                    record,
                    "canonical record"
            );

            byte[] bytes =
                    record.getBytes(
                            StandardCharsets.UTF_8
                    );

            hash = updateLong(
                    hash,
                    bytes.length
            );

            for (byte value : bytes) {
                hash ^= value & 0xffL;
                hash *= PRIME;
            }
        }

        return String.format(
                java.util.Locale.ROOT,
                "%016x",
                hash
        );
    }

    private static long updateLong(
            long hash,
            long value
    ) {
        for (int shift = 0; shift < 64; shift += 8) {
            hash ^= (value >>> shift) & 0xffL;
            hash *= PRIME;
        }

        return hash;
    }

    /**
     * Useful for checking whether a random seed
     * derivation changed across builds.
     */
    public static String randomContextFingerprint(
            long worldSeed,
            String domain,
            String entity,
            long campaignDay,
            String operation
    ) {
        long seed = SimulationRandom.derivedSeed(
                worldSeed,
                domain,
                entity,
                campaignDay,
                operation
        );

        return fingerprint(
                java.util.List.of(
                        Long.toUnsignedString(seed)
                )
        );
    }
}
