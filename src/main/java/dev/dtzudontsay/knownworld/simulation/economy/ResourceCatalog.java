
package dev.dtzudontsay.knownworld.simulation.economy;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Resource reference registry.
 *
 * Seed definitions are examples for the first economic
 * benchmark, not a complete Known World resource catalog.
 *
 * Future scenario data can populate this registry
 * without changing inventory logic.
 */
public final class ResourceCatalog {

    private final Map<ResourceId, ResourceDefinition> definitions =
            new TreeMap<>();

    public ResourceCatalog() {
    }

    public static ResourceCatalog benchmarkDefaults() {
        ResourceCatalog catalog = new ResourceCatalog();

        catalog.register(new ResourceDefinition(
                new ResourceId("grain"),
                "Grain",
                "kilogram"
        ));

        catalog.register(new ResourceDefinition(
                new ResourceId("timber"),
                "Timber",
                "kilogram"
        ));

        catalog.register(new ResourceDefinition(
                new ResourceId("coin"),
                "Coin",
                "minor_currency_unit"
        ));

        return catalog;
    }

    public void register(ResourceDefinition definition) {
        Objects.requireNonNull(definition, "definition");

        if (definitions.putIfAbsent(
                definition.id(), definition
        ) != null) {
            throw new IllegalArgumentException(
                    "Duplicate resource: " + definition.id()
            );
        }
    }

    public ResourceDefinition require(ResourceId id) {
        ResourceDefinition definition = definitions.get(
                Objects.requireNonNull(id, "id")
        );

        if (definition == null) {
            throw new IllegalArgumentException(
                    "Unknown resource: " + id
            );
        }

        return definition;
    }

    public Collection<ResourceDefinition> all() {
        return java.util.List.copyOf(definitions.values());
    }
}
