
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Runtime resource inventory.
 *
 * All balance mutations are package-private and must
 * be invoked through InventoryManager.
 */
public final class ResourceInventory {

    private final InventoryId id;
    private final EconomicOwner owner;
    private final SettlementId location;

    private final Map<ResourceId, Long> balances =
            new TreeMap<>();

    ResourceInventory(
            InventoryId id,
            EconomicOwner owner,
            SettlementId location
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.location = Objects.requireNonNull(
                location, "location"
        );
    }

    public InventoryId id() {
        return id;
    }

    public EconomicOwner owner() {
        return owner;
    }

    public SettlementId location() {
        return location;
    }

    public long quantity(ResourceId resource) {
        return balances.getOrDefault(
                Objects.requireNonNull(resource, "resource"),
                0L
        );
    }

    public Map<ResourceId, Long> balances() {
        return Map.copyOf(balances);
    }

    void setQuantity(ResourceId resource, long quantity) {
        Objects.requireNonNull(resource, "resource");

        if (quantity < 0L) {
            throw new IllegalArgumentException(
                    "Inventory cannot have negative quantities"
            );
        }

        if (quantity == 0L) {
            balances.remove(resource);
        } else {
            balances.put(resource, quantity);
        }
    }
}
