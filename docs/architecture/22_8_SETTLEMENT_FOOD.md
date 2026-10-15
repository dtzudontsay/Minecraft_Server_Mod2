
# Known World - Batch 22.8

## Interval-Aware Settlement Food Economy

Status: Deterministic vertical slice
Runtime scheduling: Not yet enabled

### Objective

Extend the single-household agricultural economy
to multiple households sharing one farm's food stock.

### Simulation intervals

AgriculturalInterval represents a half-open range
of simulation days.

Harvests occur at positive multiples of a
configured harvest period.

An interval may cross at most one harvest boundary.

The caller must supply the correct campaign days.

The model does not automatically advance time.

### Harvests

AgriculturalYieldModel calculates production.

A harvest is added to the farm's authoritative
inventory when the interval crosses a harvest
boundary.

Existing farm grain is retained and included.

Production parameters describe the output of
one complete harvest, not daily production.

### Household demand

Each household specifies:

- Inventory identity
- Population
- Food requirement per person per day

Demand is calculated over the interval's
number of days.

Existing household grain reduces the amount
the household needs from the farm.

### Allocation

Food is allocated proportionally to remaining
household requirements.

The allocation algorithm uses largest remainder
rounding to allocate whole kilograms.

When households have equal remainders,
the lower inventory ID wins.

No allocation can exceed available farm grain.

### Farm reserves

The cycle supports a protected farm reserve.

This can later represent:

- Seed grain
- Winter reserves
- Estate reserves
- Strategic food storage

Reserve values are currently supplied explicitly.

### Shortages

If available food is insufficient, the cycle
reports individual and total shortages.

A shortage does not create negative stock.

Health and mortality consequences are not
yet simulated.

### Atomicity

The entire cycle runs under the InventoryManager
monitor.

Before any mutation, it calculates:

- Harvest
- Household demand
- Distribution
- Consumption
- Shortages
- Expected remaining balances
- Required journal capacity

Expected arithmetic and journal-capacity
failures are rejected before mutation.

This is not a transaction across other systems,
such as health, population or weather.

### Accounting

Operations use InventoryManager:

- produce
- transfer
- consume

Each successful nonzero operation creates an
economic journal transaction.

Zero-quantity operations are omitted.

### Determinism

Households are processed in inventory-ID order.

Allocation ties are resolved deterministically.

Identical initial state and inputs produce
identical results.

### Existing-world compatibility

No changes to EconomyRuntime.

No automatic household inventories.

No automatic harvests.

No scenario data initialization.

New World (47) remains unchanged.

### Limitations

- One farm per settlement cycle
- No crop diversity
- No livestock production
- No regional market pricing
- No multi-farm coordination
- No class-based ration priorities
- No land ownership tax distribution
- No automatic campaign calendar integration
- No persistent processed-interval marker
- No prevention of duplicate interval execution

The caller must not execute the same harvest
interval twice against the same authoritative
economy state.

### Next batch

Batch 22.9 should connect agricultural inputs
to actual holding, settlement and population
registries.

It must also introduce persistent interval
execution tracking before autonomous simulation
is enabled.

This prevents restarting the server from
accidentally repeating a harvest.
