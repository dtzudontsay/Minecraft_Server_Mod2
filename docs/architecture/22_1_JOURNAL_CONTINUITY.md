
# Known World - Batch 22.1

## Persistent Journal Continuity

Status: Implemented for snapshot-primary sessions

### 1. Purpose

Preserve a bounded economic transaction history
across server reloads.

### 2. Authority

Legacy compatibility mode remains unchanged.

Snapshot-primary mode reads the saved transaction
history and restores inventory balances.

The InventoryManager itself begins a new session
journal on load.

Historical records remain in the loaded economy
session context.

### 3. Snapshot creation

A snapshot includes:

- Current inventory identities
- Current owners and locations
- Current resource balances
- Next inventory ID
- Retained historical transactions
- Current-session transactions

The current balances and current-session journal
are captured under the same manager lock.

### 4. Retention

Default maximum snapshot history:

10,000 transactions

If the combined history exceeds this amount,
the oldest transactions are omitted.

The retained records are renumbered from 1
to remain compatible with snapshot format v1.

These sequence numbers are NOT permanent
transaction IDs.

### 5. Repeated saves

The historical baseline comes from the last
successful world load.

A save does not append entries to that baseline.

The same current-session journal is merged on
each save.

Repeated saves must not duplicate transactions.

### 6. Load behavior

Inventory balances are restored as authoritative.

Retained historical transactions are accessible
through EconomyAuthorityService.Loaded.

The new InventoryManager starts its local
transaction sequence at 1.

No transactions are replayed into balances
during loading.

### 7. Limitations

This is not a permanent, complete ledger.

Older transactions can be compacted away.

Transaction sequence numbers are snapshot-local.

Historical actors, campaign dates, transaction
request IDs and event links remain unimplemented.

The live InventoryManager session journal remains
unbounded in memory.

Cross-domain atomicity and hard power-loss
durability are not guaranteed.

The snapshot checksum does not prove the
historical transactions explain all resource
balances before the saved checkpoint.

### 8. Safety

No automatic inventory provisioning occurs.

No resource production is enabled.

No authority markers are created automatically.

Existing legacy worlds remain compatible.

Corrupt snapshots still fail closed.

### 9. Acceptance

- Existing Gradle tests pass
- History survives multiple snapshot sessions
- Current balances survive reload
- Old records precede new records
- Retention removes oldest records first
- Repeated saves do not duplicate entries
- Invalid sequences are rejected
- No existing-world migration occurs

### 10. Next steps

Batch 22.2 should implement:

- Hard current-session journal limits
- Explicit durable checkpoint rotation
- Crash-safe transition checks
- Permanent transaction IDs
- Accounting metadata for campaign time and actors

After the reliability boundary is established,
the next focus is the first functioning agricultural
production and household consumption vertical slice.
