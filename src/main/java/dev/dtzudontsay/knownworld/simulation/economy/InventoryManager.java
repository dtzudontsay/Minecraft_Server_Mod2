
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Batch 21.0
 *
 * Authoritative resource inventory operations.
 *
 * Transfers must conserve resource quantity.
 * Failed operations must leave state unchanged.
 *
 * Production and consumption are explicit changes
 * with required cause strings.
 *
 * This manager is not yet attached to NpcSimulation
 * or runtime persistence.
 */
public final class InventoryManager {

    private final ResourceCatalog resources;

    private final Map<InventoryId, ResourceInventory> inventories =
            new TreeMap<>();

    private long nextId = 1L;

    public InventoryManager(ResourceCatalog resources) {
        this.resources = Objects.requireNonNull(
                resources, "resources"
        );
    }

    public synchronized ResourceInventory create(
            EconomicOwner owner,
            SettlementId location
    ) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(location, "location");

        if (nextId <= 0L || nextId == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Inventory ID space exhausted"
            );
        }

        InventoryId id = new InventoryId(nextId++);
        ResourceInventory inventory = new ResourceInventory(
                id, owner, location
        );

        inventories.put(id, inventory);
        return inventory;
    }

    public synchronized ResourceInventory require(
            InventoryId id
    ) {
        ResourceInventory inventory = inventories.get(
                Objects.requireNonNull(id, "id")
        );

        if (inventory == null) {
            throw new IllegalArgumentException(
                    "Unknown inventory: " + id
            );
        }

        return inventory;
    }

    public synchronized Collection<ResourceInventory> all() {
        return java.util.List.copyOf(inventories.values());
    }

    /**
     * Introduce newly produced resources.
     *
     * This must not be used for moving existing stock.
     */
    public synchronized void produce(
            InventoryId destination,
            ResourceId resource,
            long quantity,
            String cause
    ) {
        validateOperation(resource, quantity, cause);

        ResourceInventory inventory = require(destination);

        long updated = Math.addExact(
                inventory.quantity(resource),
                quantity
        );

        inventory.setQuantity(resource, updated);
    }

    /**
     * Consume or destroy existing resources.
     */
    public synchronized void consume(
            InventoryId source,
            ResourceId resource,
            long quantity,
            String cause
    ) {
        validateOperation(resource, quantity, cause);

        ResourceInventory inventory = require(source);

        long available = inventory.quantity(resource);

        if (available < quantity) {
            throw new IllegalStateException(
                    "Insufficient resource balance"
            );
        }

        inventory.setQuantity(
                resource,
                available - quantity
        );
    }

    /**
     * Move stock between two inventories atomically
     * with respect to this manager's synchronized calls.
     */
    public synchronized void transfer(
            InventoryId source,
            InventoryId destination,
            ResourceId resource,
            long quantity,
            String cause
    ) {
        validateOperation(resource, quantity, cause);

        ResourceInventory from = require(source);
        ResourceInventory to = require(destination);

        if (source.equals(destination)) {
            throw new IllegalArgumentException(
                    "Source and destination must differ"
            );
        }

        long available = from.quantity(resource);

        if (available < quantity) {
            throw new IllegalStateException(
                    "Insufficient resource balance"
            );
        }

        // Validate destination overflow before mutation.
        long updatedDestination = Math.addExact(
                to.quantity(resource),
                quantity
        );

        from.setQuantity(
                resource,
                available - quantity
        );

        to.setQuantity(
                resource,
                updatedDestination
        );
    }

    public synchronized long totalQuantity(
            ResourceId resource
    ) {
        resources.require(resource);

        long total = 0L;

        for (ResourceInventory inventory :
                inventories.values()) {
            total = Math.addExact(
                    total,
                    inventory.quantity(resource)
            );
        }

        return total;
    }

    private void validateOperation(
            ResourceId resource,
            long quantity,
            String cause
    ) {
        resources.require(resource);

        if (quantity <= 0L) {
            throw new IllegalArgumentException(
                    "Resource quantity must be positive"
            );
        }

        if (cause == null || cause.isBlank()) {
            throw new IllegalArgumentException(
                    "Economic operation requires a cause"
            );
        }
    }
}
