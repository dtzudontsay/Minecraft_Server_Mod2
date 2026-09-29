package dev.dtzudontsay.knownworld.simulation.social.succession;

public record ClaimId(long value)
        implements Comparable<ClaimId> {

    public ClaimId {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Claim ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(
            ClaimId other
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