
# Known World - Batch 21.7

## Economic Migration Preflight

Status: Implemented, not wired to live runtime

### Purpose

Assess whether existing economic inventory data can
safely move from the legacy inventory save to the
combined inventory-and-journal snapshot format.

### Save formats

Legacy:
economy_inventories.tsv

Combined:
economy_snapshot_v1.tsv

Neither format is modified by the assessment.

### Preflight outcomes

EMPTY_WORLD:
Neither save exists.

LEGACY_ONLY:
Only legacy inventory persistence exists.
Migration is required.

SNAPSHOT_ONLY:
Only a combined snapshot exists.
Runtime migration policy is required.

MATCHING:
Both files exist and their inventory state matches.

CONFLICT:
Both files exist but their inventory state differs.

INVALID_DATA:
A present save failed validation.

A conflict must never trigger automatic resetting.

### Comparison

The assessment compares:

- Inventory IDs
- Economic owner types and IDs
- Inventory settlement locations
- Resource quantities
- Next inventory ID

Transaction histories are not compared.

The result is not a cross-domain consistency audit.

### Journal capacity policy

The initial advisory thresholds are:

Warning: 50,000 session transactions
Rotation required: 100,000 session transactions

These thresholds are provisional.

Batch 21.7 does not rotate or remove journal entries.

The InventoryManager remains unchanged.

### Existing world compatibility

New World (47) must remain unaffected.

No automatic migration is enabled.

No resources or inventories are seeded.

The existing EconomyRuntime remains unchanged.

### Testing

The JUnit suite tests:

- Empty worlds
- Legacy-only saves
- Snapshot-only saves
- Matching data
- Conflicting balances
- Corrupt files
- Read-only behavior
- Journal thresholds

### Next step

Batch 21.8 should provide explicit migration
execution with validation, backups and rollback.

It must be possible to migrate legacy inventories
without losing balances, stable IDs or ownership.

Only after migration tests should the live server
switch its authoritative economic format.

Real journal compaction must have a durable checkpoint
before discarding historical entries.
