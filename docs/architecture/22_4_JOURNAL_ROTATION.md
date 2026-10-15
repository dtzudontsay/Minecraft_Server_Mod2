
# Known World - Batch 22.4

## Verified Journal Rotation and Committed Baselines

Status: Integrated in snapshot-primary save path

### Objective

Allow economic sessions to continue processing
transactions after successful verified checkpoints
without retaining every transaction in memory.

### Authority

Legacy compatibility remains unchanged.

Rotation is enabled only for explicitly activated
snapshot-primary economies.

No automatic world migration occurs.

### Checkpoint procedure

1. Verify snapshot authority.
2. Lock InventoryManager.
3. Capture the current transaction journal.
4. Capture balances and merge historical records.
5. Validate the checkpoint.
6. Write the snapshot through recovery persistence.
7. Read the published snapshot.
8. Verify the committed contents.
9. Clear the exactly matched current journal.
10. Advance the committed historical baseline.

### Rotation behavior

After a successful checkpoint:

- Authoritative inventory balances are unchanged.
- Current-session transaction count becomes zero.
- Transaction sequence restarts at 1.
- Journal generation increments.
- Committed historical records remain available.
- New transactions may proceed.

A failed checkpoint does not clear the journal.

### Historical retention

The current version-1 snapshot retains up to
10,000 recent transaction records.

Old records may be compacted across checkpoints.

Balances remain preserved in the checkpoint.

These records are therefore a bounded audit trail,
not a permanent complete ledger.

### Recovery

Snapshots continue using the existing
previous-generation backup system.

Corrupt primary snapshots fail closed.

There is no automatic recovery fallback.

### Important limitations

This implementation does not yet provide:

- Permanent transaction identity across generations
- Transaction actors or campaign timestamps
- Cross-domain Minecraft save atomicity
- Power-loss fsync guarantees
- A process-wide filesystem lock
- Automatic save scheduling based on journal capacity
- Durable full historical transaction retention
- Economic scheduling, production or consumption

The journal must be checkpointed before exceeding
10,000 current-session transactions.

A snapshot commit is not a cross-domain world
transaction.

### Existing-world safety

New World (47) remains in legacy compatibility
unless snapshot authority was explicitly activated.

No automatic resource allocation.

No automatic inventory creation.

No economic data reset.

### Acceptance

- All old tests pass.
- Rotation follows a verified checkpoint.
- Inventory balances do not change on rotation.
- Repeated saves do not duplicate transactions.
- Failed commits preserve pending transactions.
- Historical records survive multiple sessions.
- Legacy files remain untouched in snapshot mode.
- Existing worlds continue loading normally.

### Next step

Batch 22.5 should connect journal capacity to an
explicit checkpoint scheduling policy.

After that, begin the first small, testable
agricultural production and household consumption
vertical slice using controlled economic operations.
