
package dev.dtzudontsay.knownworld.simulation.population;

import dev.dtzudontsay.knownworld.simulation.economy.AgriculturalYieldModel;
import dev.dtzudontsay.knownworld.simulation.economy.HoldingAgricultureCatalog;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingId;
import dev.dtzudontsay.knownworld.simulation.social.holding.LandedHolding;
import dev.dtzudontsay.knownworld.world.reference.WorldLocationDefinition;
import dev.dtzudontsay.knownworld.world.reference.WorldReferenceCatalog;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Read-only population/workforce projection for UNPLACED holdings.
 * Uses the existing yield model and existing catalog. Does not create a
 * settlement, an inventory or an NPC, and does not perform a harvest.
 *
 * The total concerns anonymous inhabitants only; named individual NPCs are
 * NOT inferred to reside at a holding without an authored association.
 */
public final class HoldingPopulationProjection {
    public record Result(HoldingId holding, long anonymousResidents,
                         long anonymousAdultFarmers, long potentialHarvestKg,
                         String agriculturalProvenance) {}

    private HoldingPopulationProjection() {}

    public static Result project(LandedHolding holding,
                                 PopulationCohortManager cohorts,
                                 HoldingAgricultureCatalog agriculture,
                                 WorldReferenceCatalog references,
                                 int weatherPermille, int seasonPermille) {
        Objects.requireNonNull(references, "references");
        return project(holding, cohorts, agriculture, references::location,
                weatherPermille, seasonPermille);
    }

    public static Result project(LandedHolding holding,
                                 PopulationCohortManager cohorts,
                                 HoldingAgricultureCatalog agriculture,
                                 Function<String, Optional<WorldLocationDefinition>> references,
                                 int weatherPermille, int seasonPermille) {
        Objects.requireNonNull(holding, "holding");
        Objects.requireNonNull(cohorts, "cohorts");
        Objects.requireNonNull(agriculture, "agriculture");
        Objects.requireNonNull(references, "references");
        if (!cohorts.holdingSurveyed(holding.id()))
            throw new IllegalStateException("No demographic survey for holding " + holding.id());
        HoldingAgricultureCatalog.Capacity capacity = agriculture
                .resolve(holding, references)
                .orElseThrow(() -> new IllegalStateException(
                        "No agricultural production profile for holding " + holding.id()));
        long residents = 0L;
        long workers = 0L;
        for (var row : cohorts.extendedSnapshot().holdingCohorts()) {
            var key = row.key();
            if (!holding.id().equals(key.holding())) continue;
            residents = Math.addExact(residents, row.people());
            if (key.age() == PopulationCohortManager.AgeBand.ADULT
                    && "farmer".equals(key.occupationId()))
                workers = Math.addExact(workers, row.people());
        }
        AgriculturalYieldModel.Inputs inputs = capacity.yieldInputs(
                workers, weatherPermille, seasonPermille);
        long harvest = AgriculturalYieldModel.calculate(inputs).harvestKg();
        return new Result(holding.id(), residents, workers, harvest,
                capacity.provenance());
    }
}
