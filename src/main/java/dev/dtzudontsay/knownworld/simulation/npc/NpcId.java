package dev.dtzudontsay.knownworld.simulation.npc;

/**
 * Permanent simulation identity of an NPC.
 *
 * IDs are never reused.
 */
public record NpcId(long value)
        implements Comparable<NpcId> {

    public NpcId {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "NPC ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(NpcId other) {
        return Long.compare(
                value,
                other.value
        );
    }

    @Override
    public String toString() {
        return Long.toUnsignedString(value);
    }
}