
package dev.dtzudontsay.knownworld.simulation.economy;

import java.util.Objects;

/**
 * Immutable reference definition for a tradable resource.
 *
 * Quantity units must remain stable after saves begin
 * depending on this resource.
 */
public record ResourceDefinition(
        ResourceId id,
        String displayName,
        String unit
) {

    public ResourceDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(unit, "unit");

        displayName = displayName.trim();
        unit = unit.trim();

        if (displayName.isEmpty() || unit.isEmpty()) {
            throw new IllegalArgumentException(
                    "Resource name and unit must not be empty"
            );
        }
    }
}
