
# Known World - Batch 22.6

## Agricultural Production and Household Food Consumption

Status: Deterministic economic vertical slice

### Objective

Implement the first functioning resource-production
and consumption system using authoritative economic
inventories.

### Resource

grain

Unit:
kilogram

### Production

A farm inventory receives a specified harvest.

Production generates an economic transaction.

The harvest is currently supplied by the caller.

It is not yet calculated from fertility,
weather, labor, crops or seasonal conditions.

### Distribution

A farm may distribute grain to a household
inventory located in the same settlement.

Distribution cannot exceed available farm grain.

Existing farm stock is included.

A successful allocation creates a transfer
transaction.

### Household consumption

A household consumes grain up to its specified
food requirement.

Existing household stock is included.

Consumption produces a transaction.

An unmet food requirement is reported as
a shortage.

Food shortages do not create negative inventories.

### Accounting

The cycle records up to three transactions:

- Agricultural production
- Household grain allocation
- Household grain consumption

Zero-quantity operations are omitted.

The complete cycle is preflighted for stock,
arithmetic and journal capacity before mutation.

### Determinism

Identical initial inventories and input plans
produce identical results.

No random harvest outcomes are introduced.

### Persistence

All balances use the existing InventoryManager.

The inventory persistence and combined snapshot
formats require no changes.

Transactions use the existing accounting journal.

### Important limitations

This is not yet a global economy simulation.

Household identity is represented by a chosen
inventory and economic owner, not a new persisted
household entity.

Agricultural output is externally provided.

Food requirements are externally provided.

No annual or daily scheduling is attached.

No automatic provisioning or resource seeding.

No famine mortality or health effects.

No crop or livestock diversity.

No trade between settlements.

No pricing, wages, taxes or rents.

No climate, weather or seasonal modifiers.

No population-based demand calculation.

### Existing-world compatibility

EconomyRuntime remains unchanged.

New World (47) will not automatically produce
grain or create household inventories.

This batch does not modify campaign state.

### Acceptance

- Farm production increases grain stock.
- Distribution transfers existing resources.
- Household consumption reduces grain stock.
- Shortages are reported correctly.
- Existing stocks are respected.
- Insufficient journal capacity rejects a cycle.
- Arithmetic overflow rejects before mutation.
- Resource transactions have explicit causes.
- Inventories survive snapshot serialization.
- Repeated cycles preserve accounting consistency.
- Existing test suites continue passing.

### Next batch

Batch 22.7 should implement agricultural yield
and household demand models.

These should begin with explicit data inputs for:

- Cultivated area
- Land productivity
- Active agricultural labor
- Crop type
- Weather and season
- Household population
- Per-person food requirement

The models should calculate input quantities for
AgriculturalFoodCycle rather than directly modify
inventory balances.

After validation, integrate them with the
authoritative simulation calendar.
