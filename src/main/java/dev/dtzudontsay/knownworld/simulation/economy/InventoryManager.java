
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Authoritative resource inventories and session journal.
 *
 * Batch 22.2:
 * Enforces a configurable hard transaction limit.
 *
 * No operation may change inventory balances if it
 * cannot also append its transaction record.
 *
 * Restored inventory balances begin with an empty
 * current-session journal.
 */
public final class InventoryManager {

    public static final int DEFAULT_MAX_JOURNAL_ENTRIES =
            100_000;

    private final ResourceCatalog resources;

    private final int maxJournalEntries;

    private final Map<InventoryId, ResourceInventory> inventories =
            new TreeMap<>();

    private final List<EconomicTransaction> transactions =
            new ArrayList<>();

    private long nextId = 1L;
    private long nextTransactionSequence = 1L;

    public InventoryManager(ResourceCatalog resources) {
        this(resources, DEFAULT_MAX_JOURNAL_ENTRIES);
    }

    public InventoryManager(
            ResourceCatalog resources,
            int maxJournalEntries
    ) {
        this.resources = Objects.requireNonNull(
                resources, "resources"
        );

        if (maxJournalEntries <= 0) {
            throw new IllegalArgumentException(
                    "Maximum journal entries must be positive"
            );
        }

        this.maxJournalEntries = maxJournalEntries;
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

    public synchronized int maxJournalEntries() {
        return maxJournalEntries;
    }

    public synchronized int remainingJournalCapacity() {
        return maxJournalEntries - transactions.size();
    }

    public synchronized boolean journalFull() {
        return transactions.size() >= maxJournalEntries;
    }

    public synchronized List<EconomicTransaction> transactions() {
        return List.copyOf(transactions);
    }

    public synchronized long transactionCount() {
        return transactions.size();
    }

    /**
     * Trusted persistence restoration.
     *
     * Restored balances are not new production and
     * must not generate transaction entries.
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

        nextId = Math.max(
                nextId, id.value() + 1L
        );

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
        requireJournalCapacity();

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
        requireJournalCapacity();

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
        requireJournalCapacity();

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

    /**
     * This check happens before any inventory
     * balance is modified.
     */
    private void requireJournalCapacity() {
        if (transactions.size() >= maxJournalEntries) {
            throw new IllegalStateException(
                    "Economic transaction journal is full "
                            + "(" + maxJournalEntries + " entries). "
                            + "A durable checkpoint and journal "
                            + "rotation are required."
            );
        }

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
