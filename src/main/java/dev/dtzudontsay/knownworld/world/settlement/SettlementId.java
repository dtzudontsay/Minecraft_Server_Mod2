package dev.dtzudontsay.knownworld.simulation.world.settlement;

public record SettlementId(long value)
        implements Comparable<SettlementId> {

    public SettlementId {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Settlement ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(
            SettlementId other
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