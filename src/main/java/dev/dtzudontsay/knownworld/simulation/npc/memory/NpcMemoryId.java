package dev.dtzudontsay.knownworld.simulation.npc.memory;

/**
 * Permanent identity of one stored NPC memory.
 *
 * Memory IDs are never reused.
 */
public record NpcMemoryId(long value)
        implements Comparable<NpcMemoryId> {

    public NpcMemoryId {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Memory ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(
            NpcMemoryId other
    ) {
        return Long.compare(
                value,
                other.value
        );
    }

    @Override
    public String toString() {
        return Long.toString(
                value
        );
    }
}