package dev.dtzudontsay.knownworld.simulation.npc.family;

public record MarriageId(long value)
        implements Comparable<MarriageId> {

    public MarriageId {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Marriage ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(
            MarriageId other
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