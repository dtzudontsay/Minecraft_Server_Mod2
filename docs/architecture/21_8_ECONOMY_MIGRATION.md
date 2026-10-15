
# Known World - Batch 21.8

## Explicit Economy Migration

Status: Implemented, not integrated into live runtime

### Objective

Safely convert the legacy inventory representation
into the combined economic snapshot representation.

### Source

economy_inventories.tsv

### Destination

economy_snapshot_v1.tsv

### Requirements

- Minecraft server must be stopped.
- Economic files must not change during migration.
- The legacy save must exist and validate.
- Existing conflicting snapshots must be rejected.
- Existing corrupted snapshots must be rejected.
- No automatic resource creation is permitted.

### Migration procedure

1. Check the legacy save exists.
2. Check for ambiguous orphaned backups.
3. Perform read-only migration preflight.
4. Load the legacy inventory manager.
5. Capture its state.
6. Write the combined snapshot.
7. Verify the new snapshot.
8. Compare legacy and snapshot inventories.
9. Confirm original legacy bytes are unchanged.
10. Return the migration result.

### Transaction history

The legacy format stores inventory balances,
not historical economic transactions.

Migration therefore creates a snapshot containing
an empty transaction history.

No economic transaction history is invented.

### Idempotency

A second migration finds MATCHING source and
destination data.

The operation returns ALREADY_MATCHING without
writing the snapshot again.

### Failure handling

Migration refuses to proceed if:

- The legacy file is missing.
- The legacy file is corrupt.
- The snapshot is corrupt.
- Inventory representations conflict.
- An orphaned backup is present.
- Post-migration comparison fails.

Migration does not automatically delete or
roll back the new snapshot on failure.

The operator must review failed migration data.

### Existing world policy

New World (47) remains on legacy inventory
persistence until the authoritative-format
switch is explicitly implemented.

No automatic migration is performed at startup.

A successful migration test must not be confused
with approval to migrate a live campaign.

### Limitations

- No cross-domain atomic save
- No persistent transaction command IDs
- No transaction-journal compaction
- No automatic runtime migration
- No world-level operator command
- No filesystem locking across processes
- No guaranteed power-loss durability
- No live recovery selection

### Acceptance

All migration regression tests must pass.

Original legacy files must remain byte-identical.

Resource balances, inventory IDs, owner references
and settlement locations must survive migration.

### Next batch

Batch 21.9:

- Explicit authoritative-format selection
- Startup consistency verification
- Read-only compatibility handling
- Recovery-mode reporting
- Safe runtime transition strategy

No old save may be discarded silently.
