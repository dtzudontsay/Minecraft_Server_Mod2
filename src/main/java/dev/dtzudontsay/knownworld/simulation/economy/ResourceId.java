
package dev.dtzudontsay.knownworld.simulation.economy;

import java.util.Locale;

/**
 * Stable identifier for a resource definition.
 *
 * IDs are independent of localized display names.
 */
public record ResourceId(String value)
        implements Comparable<ResourceId> {

    public ResourceId {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Resource ID cannot be null"
            );
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        if (!value.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException(
                    "Invalid resource ID: " + value
            );
        }
    }

    @Override
    public int compareTo(ResourceId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
