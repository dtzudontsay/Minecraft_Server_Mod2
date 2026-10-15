
package dev.dtzudontsay.knownworld.simulation.economy;

import java.util.Objects;

/**
 * Batch 22.7.
 *
 * Converts deterministic agricultural yield and
 * household demand into an executable food cycle.
 *
 * Planning does not mutate any inventory.
 */
public final class AgriculturalCyclePlanner {

    public record Inputs(
            InventoryId farm,
            InventoryId household,
            AgriculturalYieldModel.Inputs agriculture,
            HouseholdFoodDemandModel.Inputs foodDemand,
            int allocationPermille
    ) {
        public Inputs {
            Objects.requireNonNull(farm, "farm");
            Objects.requireNonNull(household, "household");
            Objects.requireNonNull(
                    agriculture, "agriculture"
            );
            Objects.requireNonNull(
                    foodDemand, "foodDemand"
            );

            if (allocationPermille < 0
                    || allocationPermille > 1000) {
                throw new IllegalArgumentException(
                        "Household allocation must be "
                                + "between 0 and 1000 permille"
                );
            }
        }
    }

    public record Planned(
            AgriculturalYieldModel.Result yield,
            HouseholdFoodDemandModel.Result demand,
            AgriculturalFoodCycle.Plan cycle
    ) {
        public Planned {
            Objects.requireNonNull(yield, "yield");
            Objects.requireNonNull(demand, "demand");
            Objects.requireNonNull(cycle, "cycle");
        }
    }

    private AgriculturalCyclePlanner() {
    }

    public static Planned plan(Inputs inputs) {
        Objects.requireNonNull(inputs, "inputs");

        AgriculturalYieldModel.Result yield =
                AgriculturalYieldModel.calculate(
                        inputs.agriculture()
                );

        HouseholdFoodDemandModel.Result demand =
                HouseholdFoodDemandModel.calculate(
                        inputs.foodDemand()
                );

        /*
         * Only a specified share of the new harvest
         * is allocated to this particular household.
         *
         * Existing farm stock remains available
         * to AgriculturalFoodCycle, which may
         * distribute up to the requested allocation.
         */
        long allocation = java.math.BigInteger.valueOf(
                yield.harvestKg()
        ).multiply(java.math.BigInteger.valueOf(
                inputs.allocationPermille()
        )).divide(java.math.BigInteger.valueOf(
                1000L
        )).longValueExact();

        AgriculturalFoodCycle.Plan cycle =
                new AgriculturalFoodCycle.Plan(
                        inputs.farm(),
                        inputs.household(),
                        yield.harvestKg(),
                        allocation,
                        demand.requiredKg()
                );

        return new Planned(
                yield,
                demand,
                cycle
        );
    }
}
