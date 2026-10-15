
package dev.dtzudontsay.knownworld.simulation.economy;

import java.util.Objects;

/**
 * One successful inventory mutation.
 *
 * Sequence numbers are local to an InventoryManager
 * session and start at 1.
 *
 * The journal is not yet persisted.
 */
public record EconomicTransaction(
        long sequence,
        Kind kind,
        InventoryId source,
        InventoryId destination,
        ResourceId resource,
        long quantity,
        String cause
) {

    public enum Kind {
        PRODUCTION,
        CONSUMPTION,
        TRANSFER
    }

    public EconomicTransaction {
        if (sequence <= 0L) {
            throw new IllegalArgumentException(
                    "Transaction sequence must be positive"
            );
        }

        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(cause, "cause");

        cause = cause.trim();

        if (cause.isEmpty()) {
            throw new IllegalArgumentException(
                    "Transaction cause cannot be blank"
            );
        }

        if (quantity <= 0L) {
            throw new IllegalArgumentException(
                    "Transaction quantity must be positive"
            );
        }

        switch (kind) {
            case PRODUCTION -> {
                if (source != null || destination == null) {
                    throw new IllegalArgumentException(
                            "Production needs a destination only"
                    );
                }
            }

            case CONSUMPTION -> {
                if (source == null || destination != null) {
                    throw new IllegalArgumentException(
                            "Consumption needs a source only"
                    );
                }
            }

            case TRANSFER -> {
                if (source == null
                        || destination == null
                        || source.equals(destination)) {
                    throw new IllegalArgumentException(
                            "Transfer requires two distinct inventories"
                    );
                }
            }
        }
    }
}
