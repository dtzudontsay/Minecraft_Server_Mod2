package dev.dtzudontsay.knownworld.simulation.social;

public record OrganizationId(long value)
        implements Comparable<OrganizationId> {

    public OrganizationId {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Organization ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(
            OrganizationId other
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