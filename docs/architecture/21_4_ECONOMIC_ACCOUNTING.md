
# Known World - Batch 21.4

## Economic Transaction Provenance and Accounting

Status: Initial implementation

### 1. Purpose

Record successful inventory mutations and establish
a repeatable reconciliation procedure.

This batch builds on InventoryManager and
InventoryPersistence from Batches 21.0-21.3.

### 2. Transaction Types

PRODUCTION:
An identified inventory gains resources.

CONSUMPTION:
An identified inventory loses resources.

TRANSFER:
One inventory loses the same quantity another gains.

### 3. Transaction Records

EconomicTransaction contains:

- Session-local sequence number
- Transaction kind
- Source inventory, where applicable
- Destination inventory, where applicable
- Resource ID
- Quantity
- Cause

Transactions are immutable records.

Only successful operations enter the journal.

### 4. Journal Ownership

InventoryManager owns its in-memory journal.

Inventories remain the authoritative source
of current resource balances.

The journal describes how balances changed
during the current session.

It does not replace inventory state.

### 5. Accounting Reconciliation

EconomicAccountingAudit creates checkpoints containing:

- Current inventory balances
- Current journal position

The audit replays transactions recorded after
the checkpoint.

The result must equal authoritative balances.

This can detect inconsistencies between recorded
operations and the resulting inventory state.

### 6. Persistence Limitation

The journal is NOT persisted in Batch 21.4.

Loading an inventory save begins a new journal session.

Previous inventory balances remain intact.

Historical journal entries are not reconstructed.

This is not yet durable transaction provenance.

A production-grade durable ledger must coordinate
its commit boundary with inventory persistence.

Writing two unrelated files during a save is
not sufficient for crash-safe economic accounting.

### 7. Current Provenance Limitations

Transaction records do not yet include:

- Campaign day
- Simulation actor
- Transaction request ID
- Source system
- World event linkage
- Durable transaction ID
- Schema version
- Persistence generation

These fields must be introduced before the journal
is treated as long-term campaign history.

### 8. Memory and Performance

The in-memory journal currently retains all
successful operations in a running manager.

It has no retention or compaction policy.

Do not enable high-volume autonomous economic
production until bounded journal storage or
durable checkpoint rotation is implemented.

### 9. Safety and Compatibility

This batch does not alter economy_inventories.tsv.

No automatic inventory provisioning occurs.

No automatic production occurs.

Existing save data remains compatible.

### 10. Acceptance Criteria

- Transaction records compile
- Successful operations are recorded
- Rejected operations do not enter the journal
- Session-local transaction ordering is stable
- Reconciliation passes for valid operations
- Loaded inventories retain balances
- Loaded managers start a new journal session
- Existing economy tests still pass
- Minecraft launches normally

### 11. Next Batch

Batch 21.5:

Durable economic transaction boundaries.

Priorities:

- Session journal persistence
- Consistent inventory and ledger snapshots
- Transaction generation identity
- Corruption and interrupted-save recovery
- Journal size and compaction policy
- Deterministic replay tests

This should be completed before scheduling
large-scale autonomous economic activity.
