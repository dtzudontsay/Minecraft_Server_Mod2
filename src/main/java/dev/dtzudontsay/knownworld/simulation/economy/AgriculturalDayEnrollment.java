
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;

import java.util.Objects;

/**
 * Batch 22.11.
 *
 * Explicit enrollment for one agricultural holding.
 *
 * The identities refer to established Known World
 * registries; this record does not create a new
 * holding or settlement identity system.
 *
 * The enrollment is a caller-supplied configuration,
 * not yet an automatically populated scenario asset.
 */
public record AgriculturalDayEnrollment(
        HoldingId holding,
        InventoryId farmInventory,
        SettlementId settlement,
        long harvestPeriodDays,
        AgriculturalYieldModel.Inputs harvestInputs,
        long protectedReserveKg,
        long gramsPerPersonPerDay
) {
    public AgriculturalDayEnrollment {
        Objects.requireNonNull(holding, "holding");
        Objects.requireNonNull(
                farmInventory, "farmInventory"
        );
        Objects.requireNonNull(settlement, "settlement");
        Objects.requireNonNull(
                harvestInputs, "harvestInputs"
        );

        if (harvestPeriodDays <= 0L) {
            throw new IllegalArgumentException(
                    "Harvest period must be positive"
            );
        }

        if (protectedReserveKg < 0L) {
            throw new IllegalArgumentException(
                    "Protected reserve cannot be negative"
            );
        }

        if (gramsPerPersonPerDay < 0L) {
            throw new IllegalArgumentException(
                    "Food requirement cannot be negative"
            );
        }
    }

    public AgriculturalInterval interval(
            long startDayInclusive,
            long endDayExclusive
    ) {
        return new AgriculturalInterval(
                startDayInclusive,
                endDayExclusive,
                harvestPeriodDays
        );
    }
}
