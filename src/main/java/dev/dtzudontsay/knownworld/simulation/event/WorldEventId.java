package dev.dtzudontsay.knownworld.simulation.event;

/**
 * Permanent identity of one objective world event.
 */
public record WorldEventId(long value)
        implements Comparable<WorldEventId> {

    public WorldEventId {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "World event ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(
            WorldEventId other
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