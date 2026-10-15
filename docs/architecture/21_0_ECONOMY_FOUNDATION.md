
# Known World - Batch 21.0 Economy Foundation

Status: First implementation
Scope: Resource accounting primitives

## 1. Purpose

Introduce the first operational components of the
Known World economy.

This batch implements:

- Stable resource identifiers
- Resource definitions and catalog
- Typed economic ownership references
- Inventory identifiers
- Resource balances
- Production
- Consumption
- Resource transfers
- Resource conservation tests

## 2. Ownership

EconomicOwner references existing identities:

- NpcId
- SettlementId
- HoldingId
- OrganizationId

These references do not replace existing identity
managers.

Current limitation:
The new inventory manager does not yet validate that
the referenced entities exist in live simulation.

## 3. Inventory

Every inventory has:

- An InventoryId
- An EconomicOwner
- A SettlementId location
- Resource balances

Resource balances are nonnegative long integers.

Ownership and inventory location are distinct.

ResourceInventory exposes read operations but not
public balance mutation methods.

## 4. Accounting Operations

PRODUCE:
Creates resources for an identified cause.

CONSUME:
Removes resources for an identified cause.

TRANSFER:
Moves resources between two inventories.

Transfer conservation:

Total resource quantity before transfer equals
total resource quantity after transfer.

The manager rejects invalid or insufficient transfers.

Destination overflow is checked before mutation.

## 5. Current Limitations

Not yet implemented:

- Runtime integration
- Persistent inventory saves
- Transaction journal
- Save/load recovery
- Character and settlement reference validation
- Household membership
- Population cohorts
- Production constraints
- Labor requirements
- Consumption requirements
- Taxes
- Prices
- Markets
- Economic decision-making

A production operation currently trusts its caller.

This must not be exposed directly to uncontrolled
NPC actions or player commands.

## 6. Resource Units

Benchmark resources:

grain: kilogram
timber: kilogram
coin: minor_currency_unit

These are provisional reference definitions.

The final economic resource catalog should be
loaded from versioned scenario/reference data.

Never silently change the unit of a resource
after persistent balances begin using it.

## 7. Determinism

Inventory IDs are assigned sequentially by creation.

Resource and inventory iteration uses stable ordering.

Future persistent snapshots must preserve next-ID state
or safely derive an unused ID from validated records.

Economic randomness must use the existing
SimulationRandom contracts.

## 8. Tests

InventoryManagerTest covers:

- Production
- Consumption
- Resource conservation
- Insufficient balances
- Arithmetic overflow
- Unknown resources
- Invalid quantities
- Inventory identity
- Duplicate definitions

Tests execute through Gradle/JUnit.

## 9. Save Compatibility

This batch does not change the save format.

No inventory persistence is connected to the server.

Existing world saves remain unaffected by these classes.

## 10. Next Implementation

Batch 21.1 should implement:

1. Versioned inventory persistence.
2. Save/load round-trip tests.
3. Inventory/reference integrity validation.
4. A safe live integration path.
5. Explicit initialization of economic state.
6. A migration policy for existing worlds.
7. Transaction provenance.

Do not initialize arbitrary resource balances in
existing worlds before defining migration behavior.

## 11. Completion Status

Resource identifiers: Core implemented
Resource definitions: Initial implementation
Economic ownership references: Initial implementation
Inventory balances: Core implemented
Conserved transfers: Core implemented
Persistence: Not implemented
Production constraints: Not implemented
Runtime integration: Not implemented
Population integration: Not implemented

The full economy is not yet core complete.
