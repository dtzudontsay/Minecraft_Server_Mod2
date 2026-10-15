
package dev.dtzudontsay.knownworld.simulation.random;

import java.util.Objects;
import java.util.SplittableRandom;

/**
 * Immutable deterministic random context.
 *
 * This is a value object, not a random-number
 * generator with shared mutable state.
 */
public record SimulationRandomContext(
        long worldSeed,
        String domain,
        String entityId,
        long campaignDay,
        String operationKey
) {

    public SimulationRandomContext {
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
                    "campaignDay cannot be negative"
            );
        }
    }

    public long seed() {
        return SimulationRandom.derivedSeed(
                worldSeed,
                domain,
                entityId,
                campaignDay,
                operationKey
        );
    }

    public SplittableRandom createStream() {
        return new SplittableRandom(seed());
    }

    /**
     * Create a distinct named operation context.
     */
    public SimulationRandomContext forOperation(
            String newOperationKey
    ) {
        return new SimulationRandomContext(
                worldSeed,
                domain,
                entityId,
                campaignDay,
                newOperationKey
        );
    }

    /**
     * Create the same operation for another
     * campaign day.
     */
    public SimulationRandomContext onDay(
            long newCampaignDay
    ) {
        return new SimulationRandomContext(
                worldSeed,
                domain,
                entityId,
                newCampaignDay,
                operationKey
        );
    }
}
