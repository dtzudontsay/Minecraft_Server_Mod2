
package dev.dtzudontsay.knownworld.simulation.random;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.SplittableRandom;

/**
 * Batch 20.2
 *
 * Deterministic random-stream derivation.
 *
 * A stable seed and an identical context produce
 * an identical pseudo-random sequence.
 *
 * Each simulation operation should derive a stream
 * from its own stable domain, entity, campaign day,
 * and operation key.
 *
 * Algorithm version 1.
 *
 * Do not change this algorithm after it becomes
 * part of authoritative simulation behavior
 * without an explicit compatibility migration.
 */
public final class SimulationRandom {

    public static final int ALGORITHM_VERSION = 1;

    private static final long FNV_OFFSET =
            0xcbf29ce484222325L;

    private static final long FNV_PRIME =
            0x100000001b3L;

    private SimulationRandom() {
    }

    /**
     * Stable UTF-8 FNV-1a hashing.
     *
     * Unlike String.hashCode(), this method
     * explicitly defines its byte encoding.
     */
    public static long stableHash(String value) {
        Objects.requireNonNull(value, "value");

        long hash = FNV_OFFSET;

        for (byte current :
                value.getBytes(StandardCharsets.UTF_8)) {

            hash ^= current & 0xffL;
            hash *= FNV_PRIME;
        }

        return hash;
    }

    /**
     * Stable 64-bit mixing function.
     */
    public static long mix64(long value) {
        value = (value ^ (value >>> 30))
                * 0xbf58476d1ce4e5b9L;

        value = (value ^ (value >>> 27))
                * 0x94d049bb133111ebL;

        return value ^ (value >>> 31);
    }

    /**
     * Derive an independent deterministic stream.
     *
     * Different entity IDs or operation keys should
     * result in different derived seeds.
     *
     * The resulting stream itself is mutable and
     * must not be shared between independent actions.
     */
    public static SplittableRandom stream(
            long worldSeed,
            String domain,
            String entityId,
            long campaignDay,
            String operationKey
    ) {
        return new SplittableRandom(
                derivedSeed(
                        worldSeed,
                        domain,
                        entityId,
                        campaignDay,
                        operationKey
                )
        );
    }

    public static long derivedSeed(
            long worldSeed,
            String domain,
            String entityId,
            long campaignDay,
            String operationKey
    ) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(
                operationKey,
                "operationKey"
        );

        if (domain.isBlank()
                || entityId.isBlank()
                || operationKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Random context keys must not be blank"
            );
        }

        if (campaignDay < 0L) {
            throw new IllegalArgumentException(
                    "Campaign day cannot be negative"
            );
        }

        long result = mix64(
                worldSeed ^ ALGORITHM_VERSION
        );

        result = mix64(
                result ^ stableHash(domain)
        );

        result = mix64(
                result ^ stableHash(entityId)
        );

        result = mix64(
                result ^ campaignDay
        );

        result = mix64(
                result ^ stableHash(operationKey)
        );

        return result;
    }
}
