
package dev.dtzudontsay.knownworld.simulation.economy;

/**
 * Permanent identity of an economic inventory.
 */
public record InventoryId(long value)
        implements Comparable<InventoryId> {

    public InventoryId {
        if (value <= 0L) {
            throw new IllegalArgumentException(
                    "Inventory ID must be positive"
            );
        }
    }

    @Override
    public int compareTo(InventoryId other) {
        return Long.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
