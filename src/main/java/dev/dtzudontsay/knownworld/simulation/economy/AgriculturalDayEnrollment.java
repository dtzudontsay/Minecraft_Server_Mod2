
package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;
import dev.dtzudontsay.knownworld.world.reference.WorldLocationDefinition;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Existing explicit farm enrollment, now also constructible from scenario
 * agricultural capacity. No automatic farm enrollment is performed.
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
        Objects.requireNonNull(farmInventory, "farmInventory");
        Objects.requireNonNull(settlement, "settlement");
        Objects.requireNonNull(harvestInputs, "harvestInputs");
        if (harvestPeriodDays <= 0L) {
            throw new IllegalArgumentException("Harvest period must be positive");
        }
        if (protectedReserveKg < 0L || gramsPerPersonPerDay < 0L) {
            throw new IllegalArgumentException("Negative agricultural reserve or ration");
        }
    }

    /**
     * Existing runtime holding identity and inventory are supplied by the caller.
     * Workers must come from actual population/labor state, never from the
     * political holding's normalized populationWeight.
     *
     * Weather and season modifiers are explicit until the environmental
     * simulation supplies them authoritatively.
     */
    public static AgriculturalDayEnrollment fromHolding(
            LandedHolding holding,
            InventoryId farmInventory,
            SettlementId settlement,
            HoldingAgricultureCatalog catalog,
            WorldReferenceCatalog references,
            long agriculturalWorkers,
            int weatherPermille,
            int seasonPermille
    ) {
        Objects.requireNonNull(references, "references");
        return fromHolding(holding, farmInventory, settlement, catalog,
                references::location, agriculturalWorkers,
                weatherPermille, seasonPermille);
    }

    /** Deterministic lookup-based equivalent for tests and scenario tooling. */
    public static AgriculturalDayEnrollment fromHolding(
            LandedHolding holding,
            InventoryId farmInventory,
            SettlementId settlement,
            HoldingAgricultureCatalog catalog,
            Function<String, Optional<WorldLocationDefinition>> locationLookup,
            long agriculturalWorkers,
            int weatherPermille,
            int seasonPermille
    ) {
        Objects.requireNonNull(holding, "holding");
        Objects.requireNonNull(catalog, "catalog");
        HoldingAgricultureCatalog.Capacity capacity = catalog
                .resolve(holding, locationLookup)
                .orElseThrow(() -> new IllegalStateException(
                        "No eligible agricultural capacity for holding "
                                + holding.authoredId() + " at " + holding.worldLocationId()
                ));
        return new AgriculturalDayEnrollment(
                holding.id(), farmInventory, settlement,
                capacity.harvestPeriodDays(),
                capacity.yieldInputs(agriculturalWorkers,
                        weatherPermille, seasonPermille),
                capacity.protectedReserveKg(),
                capacity.gramsPerPersonPerDay()
        );
    }

    public AgriculturalInterval interval(
            long startDayInclusive,
            long endDayExclusive
    ) {
        return new AgriculturalInterval(
                startDayInclusive, endDayExclusive, harvestPeriodDays
        );
    }
}
