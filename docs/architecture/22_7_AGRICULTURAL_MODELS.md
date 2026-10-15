
# Known World - Batch 22.7

## Agricultural Yield and Household Food Demand

Status: Deterministic models implemented
Runtime scheduling: Not enabled

### Objective

Calculate agricultural production and household
food needs from explicit simulation inputs.

These calculations supply the agricultural cycle
introduced in Batch 22.6.

### Agricultural output

Inputs:

- Cultivated land in hectares
- Number of agricultural workers
- Grain output per hectare
- Grain output per worker
- Fertility modifier
- Weather modifier
- Seasonal productivity modifier

Land capacity is:

hectares * grain kg per hectare

Labor capacity is:

workers * grain kg per worker

Base harvest is the smaller capacity.

Fertility, weather and seasonal modifiers are
applied multiplicatively.

Each modifier uses permille:

1000 = neutral
500 = half productivity
1500 = 150 percent productivity

All arithmetic is deterministic.

### Food demand

Inputs:

- Household population
- Number of simulated days
- Grain requirement in grams per person per day

Total grams:

population * days * daily requirement

Whole kilograms are rounded upward.

The grain requirement is configurable and
is not a historical nutritional constant.

### Agricultural cycle planning

The planner calculates:

- Harvested grain
- Household food requirement
- Planned share of harvest for the household

Allocation is expressed in permille.

The planner constructs an AgriculturalFoodCycle.Plan.

It does not modify inventories.

The agricultural cycle executes all resource
production, distribution and consumption.

### Current limitations

The simulation inputs are not yet derived
automatically from canonical scenario data.

No cultivated-land database is attached.

No historical grain yields are asserted.

No automatic weather effects.

No household population registry is attached.

No simulated calendar schedule is attached.

No crop diversity or seed reserves.

No economic taxation or rent distribution.

No multiple-household production allocation.

No famine effects on health or mortality.

The grain inventory currently uses whole
kilograms rather than grams.

### Consistency requirements

One agricultural cycle should represent an
explicit simulation interval.

Production coefficients must use that same
interval or be converted before planning.

The planner does not infer an interval for
yield coefficients.

Demand uses the supplied number of days.

Do not apply annual yield coefficients to
daily cycles without converting them.

### Acceptance

- Land and labor both constrain production
- Fertility, weather and season affect output
- Food demand scales with population and days
- Planning does not mutate inventory state
- Planned cycles execute through InventoryManager
- Identical inputs generate identical results
- No world inventories are seeded automatically
- Existing economic persistence remains compatible

### Next batch

Batch 22.8 should introduce interval-aware
agricultural scheduling and multi-household
allocation.

It should also establish explicit model inputs
from the population, holding and settlement
registries before enabling autonomous production.
