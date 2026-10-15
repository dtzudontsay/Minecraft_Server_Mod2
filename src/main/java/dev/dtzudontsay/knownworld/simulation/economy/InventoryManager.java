
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Authoritative inventory operations.
 *
 * Batch 21.4:
 * Adds a session-local mutation journal.
 *
 * The journal is not persisted. After loading a save,
 * a new recording session begins.
 *
 * No autonomous economy is activated here.
 */
public final class InventoryManager {

    private final ResourceCatalog resources;

    private final Map<InventoryId, ResourceInventory> inventories =
            new TreeMap<>();

    private final List<EconomicTransaction> transactions =
            new ArrayList<>();

    private long nextId = 1L;
    private long nextTransactionSequence = 1L;

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

        ResourceInventory inventory =
                new ResourceInventory(id, owner, location);

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
        return List.copyOf(inventories.values());
    }

    public synchronized long nextId() {
        return nextId;
    }

    /**
     * Returns an immutable snapshot of this session's
     * successful economic mutations.
     */
    public synchronized List<EconomicTransaction> transactions() {
        return List.copyOf(transactions);
    }

    public synchronized long transactionCount() {
        return transactions.size();
    }

    /**
     * Used only by trusted persistence code.
     */
    synchronized ResourceInventory restoreInventory(
            InventoryId id,
            EconomicOwner owner,
            SettlementId location,
            Map<ResourceId, Long> balances
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(balances, "balances");

        if (inventories.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Duplicate inventory ID: " + id
            );
        }

        if (id.value() == Long.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Inventory ID cannot be Long.MAX_VALUE"
            );
        }

        for (Map.Entry<ResourceId, Long> entry :
                balances.entrySet()) {

            resources.require(entry.getKey());

            Long quantity = Objects.requireNonNull(
                    entry.getValue(), "quantity"
            );

            if (quantity <= 0L) {
                throw new IllegalArgumentException(
                        "Stored resource quantity must be positive"
                );
            }
        }

        ResourceInventory inventory =
                new ResourceInventory(id, owner, location);

        for (Map.Entry<ResourceId, Long> entry :
                balances.entrySet()) {
            inventory.setQuantity(
                    entry.getKey(), entry.getValue()
            );
        }

        inventories.put(id, inventory);

        nextId = Math.max(nextId, id.value() + 1L);

        return inventory;
    }

    synchronized void restoreNextId(long savedNextId) {
        if (savedNextId < nextId
                || savedNextId <= 0L) {
            throw new IllegalArgumentException(
                    "Invalid saved next inventory ID"
            );
        }

        nextId = savedNextId;
    }

    public synchronized void produce(
            InventoryId destination,
            ResourceId resource,
            long quantity,
            String cause
    ) {
        validateOperation(resource, quantity, cause);
        ensureTransactionCapacity();

        ResourceInventory inventory = require(destination);

        long updated = Math.addExact(
                inventory.quantity(resource),
                quantity
        );

        EconomicTransaction transaction =
                new EconomicTransaction(
                        nextTransactionSequence,
                        EconomicTransaction.Kind.PRODUCTION,
                        null,
                        destination,
                        resource,
                        quantity,
                        cause
                );

        inventory.setQuantity(resource, updated);
        append(transaction);
    }

    public synchronized void consume(
            InventoryId source,
            ResourceId resource,
            long quantity,
            String cause
    ) {
        validateOperation(resource, quantity, cause);
        ensureTransactionCapacity();

        ResourceInventory inventory = require(source);
        long available = inventory.quantity(resource);

        if (available < quantity) {
            throw new IllegalStateException(
                    "Insufficient resource balance"
            );
        }

        EconomicTransaction transaction =
                new EconomicTransaction(
                        nextTransactionSequence,
                        EconomicTransaction.Kind.CONSUMPTION,
                        source,
                        null,
                        resource,
                        quantity,
                        cause
                );

        inventory.setQuantity(
                resource, available - quantity
        );
        append(transaction);
    }

    public synchronized void transfer(
            InventoryId source,
            InventoryId destination,
            ResourceId resource,
            long quantity,
            String cause
    ) {
        validateOperation(resource, quantity, cause);
        ensureTransactionCapacity();

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

        // Check overflow before changing either balance.
        long updatedDestination = Math.addExact(
                to.quantity(resource),
                quantity
        );

        EconomicTransaction transaction =
                new EconomicTransaction(
                        nextTransactionSequence,
                        EconomicTransaction.Kind.TRANSFER,
                        source,
                        destination,
                        resource,
                        quantity,
                        cause
                );

        from.setQuantity(
                resource, available - quantity
        );

        to.setQuantity(
                resource, updatedDestination
        );

        append(transaction);
    }

    public synchronized long totalQuantity(
            ResourceId resource
    ) {
        resources.require(resource);

        long total = 0L;

        for (ResourceInventory inventory :
                inventories.values()) {

            total = Math.addExact(
                    total, inventory.quantity(resource)
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

    private void ensureTransactionCapacity() {
        if (nextTransactionSequence <= 0L
                || nextTransactionSequence == Long.MAX_VALUE) {
            throw new IllegalStateException(
                    "Transaction sequence space exhausted"
            );
        }
    }

    private void append(EconomicTransaction transaction) {
        transactions.add(transaction);
        nextTransactionSequence++;
    }
}
