package dev.dtzudontsay.knownworld.simulation.social.dynasty;

public record DynastyId(long value)
        implements Comparable<DynastyId> {

    public DynastyId {

        if (value <= 0) {

            throw new IllegalArgumentException(
                    "Dynasty ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(
            DynastyId other
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