
package dev.dtzudontsay.knownworld.simulation.economy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Reconciles session-local economic mutations.
 *
 * A checkpoint captures authoritative balances and
 * the current journal position.
 *
 * Later verification replays only operations recorded
 * after that checkpoint and compares the result with
 * current authoritative balances.
 *
 * This does not audit historical operations before
 * the checkpoint or across server restarts.
 */
public final class EconomicAccountingAudit {

    private EconomicAccountingAudit() {
    }

    public record Checkpoint(
            Map<InventoryId, Map<ResourceId, Long>> balances,
            int transactionIndex
    ) {
        public Checkpoint {
            Objects.requireNonNull(balances, "balances");

            Map<InventoryId, Map<ResourceId, Long>> copy =
                    new TreeMap<>();

            for (var entry : balances.entrySet()) {
                copy.put(
                        entry.getKey(),
                        Map.copyOf(entry.getValue())
                );
            }

            balances = Map.copyOf(copy);

            if (transactionIndex < 0) {
                throw new IllegalArgumentException(
                        "Negative transaction index"
                );
            }
        }
    }

    /**
     * Create a consistent baseline for this manager.
     */
    public static Checkpoint checkpoint(
            InventoryManager manager
    ) {
        Objects.requireNonNull(manager, "manager");

        synchronized (manager) {
            Map<InventoryId, Map<ResourceId, Long>> state =
                    new TreeMap<>();

            for (ResourceInventory inventory : manager.all()) {
                state.put(
                        inventory.id(),
                        inventory.balances()
                );
            }

            return new Checkpoint(
                    state,
                    manager.transactions().size()
            );
        }
    }

    /**
     * Verify that the journal explains all balance
     * changes since the checkpoint.
     *
     * New, empty inventories are allowed.
     *
     * Existing inventories cannot disappear, because
     * the current manager has no deletion operation.
     */
    public static void verify(
            InventoryManager manager,
            Checkpoint checkpoint
    ) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(checkpoint, "checkpoint");

        synchronized (manager) {
            List<EconomicTransaction> journal =
                    manager.transactions();

            if (checkpoint.transactionIndex()
                    > journal.size()) {
                throw new IllegalStateException(
                        "Checkpoint belongs to a later journal position"
                );
            }

            Map<InventoryId, Map<ResourceId, Long>> expected =
                    new TreeMap<>();

            for (var entry :
                    checkpoint.balances().entrySet()) {

                expected.put(
                        entry.getKey(),
                        new HashMap<>(entry.getValue())
                );
            }

            Map<InventoryId, Map<ResourceId, Long>> actual =
                    new TreeMap<>();

            for (ResourceInventory inventory : manager.all()) {
                actual.put(
                        inventory.id(),
                        inventory.balances()
                );

                expected.putIfAbsent(
                        inventory.id(),
                        new HashMap<>()
                );
            }

            if (!actual.keySet().equals(expected.keySet())) {
                throw new IllegalStateException(
                        "Inventory identities changed unexpectedly"
                );
            }

            for (int i = checkpoint.transactionIndex();
                 i < journal.size(); i++) {

                EconomicTransaction transaction = journal.get(i);

                if (transaction.sequence() != (long) i + 1L) {
                    throw new IllegalStateException(
                            "Transaction sequence mismatch at index " + i
                    );
                }

                switch (transaction.kind()) {
                    case PRODUCTION -> increase(
                            expected,
                            transaction.destination(),
                            transaction.resource(),
                            transaction.quantity()
                    );

                    case CONSUMPTION -> decrease(
                            expected,
                            transaction.source(),
                            transaction.resource(),
                            transaction.quantity()
                    );

                    case TRANSFER -> {
                        decrease(
                                expected,
                                transaction.source(),
                                transaction.resource(),
                                transaction.quantity()
                        );

                        increase(
                                expected,
                                transaction.destination(),
                                transaction.resource(),
                                transaction.quantity()
                        );
                    }
                }
            }

            for (var entry : actual.entrySet()) {
                Map<ResourceId, Long> expectedBalances =
                        expected.get(entry.getKey());

                if (!entry.getValue().equals(expectedBalances)) {
                    throw new IllegalStateException(
                            "Accounting discrepancy in inventory "
                                    + entry.getKey()
                                    + ": expected " + expectedBalances
                                    + ", actual " + entry.getValue()
                    );
                }
            }
        }
    }

    private static void increase(
            Map<InventoryId, Map<ResourceId, Long>> state,
            InventoryId inventoryId,
            ResourceId resource,
            long quantity
    ) {
        Map<ResourceId, Long> balances = require(
                state, inventoryId
        );

        long updated = Math.addExact(
                balances.getOrDefault(resource, 0L),
                quantity
        );

        balances.put(resource, updated);
    }

    private static void decrease(
            Map<InventoryId, Map<ResourceId, Long>> state,
            InventoryId inventoryId,
            ResourceId resource,
            long quantity
    ) {
        Map<ResourceId, Long> balances = require(
                state, inventoryId
        );

        long available = balances.getOrDefault(
                resource, 0L
        );

        if (available < quantity) {
            throw new IllegalStateException(
                    "Journal withdraws nonexistent stock "
                            + "from inventory " + inventoryId
            );
        }

        long updated = available - quantity;

        if (updated == 0L) {
            balances.remove(resource);
        } else {
            balances.put(resource, updated);
        }
    }

    private static Map<ResourceId, Long> require(
            Map<InventoryId, Map<ResourceId, Long>> state,
            InventoryId id
    ) {
        Map<ResourceId, Long> balances = state.get(id);

        if (balances == null) {
            throw new IllegalStateException(
                    "Transaction references unknown inventory: " + id
            );
        }

        return balances;
    }
}
