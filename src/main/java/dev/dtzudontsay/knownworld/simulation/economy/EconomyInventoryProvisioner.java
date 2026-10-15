
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.Objects;

/**
 * Batch 21.3.
 *
 * Explicit, validated provisioning of empty inventories.
 *
 * Does not produce resources, money or population.
 *
 * A matching owner/location pair is provisioned at most
 * once through this service.
 *
 * Multiple inventories for an owner can still be supported
 * later through explicit named inventory identities.
 */
public final class EconomyInventoryProvisioner {

    private final InventoryManager manager;
    private final EconomyReferenceValidator validator;

    public EconomyInventoryProvisioner(
            InventoryManager manager,
            EconomyReferenceValidator validator
    ) {
        this.manager = Objects.requireNonNull(
                manager, "manager"
        );
        this.validator = Objects.requireNonNull(
                validator, "validator"
        );
    }

    public synchronized ResourceInventory ensureEmptyInventory(
            EconomicOwner owner,
            SettlementId location
    ) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(location, "location");

        // Validate references before a new ID is allocated.
        // The validator also accepts an individual inventory.
        ResourceInventory candidate = new ResourceInventory(
                new InventoryId(1L),
                owner,
                location
        );

        validator.validate(candidate);

        synchronized (manager) {
            for (ResourceInventory existing : manager.all()) {
                if (existing.owner().equals(owner)
                        && existing.location().equals(location)) {
                    return existing;
                }
            }

            return manager.create(owner, location);
        }
    }
}
