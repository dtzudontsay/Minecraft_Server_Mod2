package dev.dtzudontsay.knownworld.simulation.social.holding;

public record HoldingId(long value)
        implements Comparable<HoldingId> {

    public HoldingId {

        if (value <= 0) {

            throw new IllegalArgumentException(
                    "Holding ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(
            HoldingId other
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