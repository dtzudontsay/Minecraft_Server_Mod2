
# Known World - Batch 21.5

## Combined Economic Snapshot Prototype

Status: Isolated persistence implementation

### 1. Purpose

Introduce a single-file serialization boundary for
inventory balances and session transaction history.

### 2. Format

Filename:
economy_snapshot_v1.tsv

Header:
KNOWNWORLD_ECONOMY_SNAPSHOT, version 1

Record types:
- NEXT_ID
- INVENTORY
- BALANCE
- TX
- CHECKSUM

Encoding:
UTF-8

Transaction causes use Base64 URL encoding.

### 3. Consistency

Snapshots capture inventory balances and journal
entries under the InventoryManager monitor.

All fields are serialized into one file.

A SHA-256 checksum detects accidental changes.

The writer requires atomic file replacement.

### 4. Restoration

The loader returns an immutable snapshot record.

The restore method constructs a new InventoryManager.

The restored manager contains the recorded balances.

The captured transactions remain accessible through
Snapshot.transactions().

A new InventoryManager starts a new journal session.

### 5. Compatibility

The current live authoritative save remains:

economy_inventories.tsv

The new prototype uses:

economy_snapshot_v1.tsv

EconomyRuntime does not yet load or write
the new snapshot format.

No existing worlds are migrated automatically.

### 6. Reliability Limitations

This implementation does not yet provide:

- Full historical journal replay
- Transaction actors and campaign timestamps
- Cross-domain atomic world snapshots
- Durable sync guarantees after power loss
- Backup generation rotation
- Crash recovery selection
- Journal compaction
- Immutable historical transaction retention
- Migration from the legacy inventory save
- Bounded operation history in memory

The full economy must not be enabled until its
persistence protocol meets its requirements.

### 7. Acceptance

The isolated tests must confirm:

- Balances survive save/load
- IDs and next-ID survive
- Transaction entries survive
- Non-ASCII causes survive
- Checksum corruption is rejected
- Missing snapshots are errors
- Empty snapshots round-trip
- Snapshot data does not change after capture
- Repeated replacement works
- Temporary files are cleaned up

### 8. Next Batch

Batch 21.6 should implement recovery generations
and a bounded historical journal policy.

Before switching EconomyRuntime to the new format,
a tested migration procedure must ensure that an
existing legacy economy save remains recoverable.

The migration must never discard nonempty balances.
