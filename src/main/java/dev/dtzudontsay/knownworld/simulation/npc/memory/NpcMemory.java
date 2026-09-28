package dev.dtzudontsay.knownworld.simulation.npc.memory;

import dev.dtzudontsay.knownworld.simulation.npc.NpcId;

import java.util.Objects;
import java.util.Optional;

/**
 * One persistent memory belonging to an NPC.
 *
 * Memories are subjective records.
 *
 * A memory does not imply that the remembered information is
 * objectively true.
 */
public record NpcMemory(
        NpcMemoryId id,
        NpcId owner,
        NpcMemoryType type,
        String summary,
        double importance,
        NpcId relatedNpc,
        String factKey,
        long createdTick
) {

    public NpcMemory {
        Objects.requireNonNull(
                id,
                "id"
        );

        Objects.requireNonNull(
                owner,
                "owner"
        );

        Objects.requireNonNull(
                type,
                "type"
        );

        Objects.requireNonNull(
                summary,
                "summary"
        );

        summary =
                summary.trim();

        if (summary.isEmpty()) {
            throw new IllegalArgumentException(
                    "Memory summary cannot be empty"
            );
        }

        if (!Double.isFinite(
                importance
        )
                || importance < 0.0
                || importance > 1.0) {

            throw new IllegalArgumentException(
                    "Memory importance must be between 0.0 and 1.0"
            );
        }

        if (factKey != null) {

            factKey =
                    factKey.trim();

            if (factKey.isEmpty()) {
                factKey =
                        null;
            }
        }

        if (createdTick < 0) {
            throw new IllegalArgumentException(
                    "createdTick cannot be negative"
            );
        }
    }

    public Optional<NpcId> relatedNpcOptional() {
        return Optional.ofNullable(
                relatedNpc
        );
    }

    public Optional<String> factKeyOptional() {
        return Optional.ofNullable(
                factKey
        );
    }
}