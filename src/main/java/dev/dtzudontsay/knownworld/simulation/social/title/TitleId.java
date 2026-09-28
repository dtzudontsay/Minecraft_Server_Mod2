package dev.dtzudontsay.knownworld.simulation.social.title;

public record TitleId(long value)
        implements Comparable<TitleId> {

    public TitleId {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Title ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(
            TitleId other
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