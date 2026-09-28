package dev.dtzudontsay.knownworld.simulation.npc.goal;

public record NpcGoalId(long value)
        implements Comparable<NpcGoalId> {

    public NpcGoalId {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Goal ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(
            NpcGoalId other
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