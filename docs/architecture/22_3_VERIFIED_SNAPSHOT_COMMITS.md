
# Known World - Batch 22.3

## Verified Economic Snapshot Commits

Status: Snapshot-primary save integration

### Purpose

Integrate economic checkpoint validation into the
authoritative snapshot save path.

### Commit protocol

In snapshot-primary mode:

1. Check the authority marker.
2. Acquire the InventoryManager monitor.
3. Check current-session journal capacity.
4. Capture balances and journal together.
5. Validate the captured checkpoint.
6. Validate the previous primary snapshot.
7. Preserve its previous generation.
8. Publish the new snapshot atomically.
9. Read the published primary.
10. Compare its contents with the captured state.

### Current-session protection

The existing snapshot v1 history limit is
10,000 retained transactions.

If a current session has more than 10,000
transactions, the commit must fail before writing.

Current-session records must not be silently
discarded through retention.

### Failure behavior

A failed commit must not clear the in-memory
journal or change inventory balances.

If a primary is corrupt, ordinary saving refuses
to overwrite it.

Manual recovery remains necessary.

### Legacy compatibility

Legacy authority behavior remains unchanged.

An empty world must not create economy files.

No automatic snapshot activation is performed.

### Limitations

This is a verified snapshot commit protocol,
not yet automatic journal rotation.

It does not implement:

- Clearing a committed current-session journal
- Updating the in-memory historical baseline
- Permanent transaction identities
- Cross-domain atomicity
- Filesystem process locking
- Directory fsync and power-loss guarantees
- Economic event timestamps
- Population or agricultural production

The 100,000-entry InventoryManager limit still
exceeds the 10,000-entry snapshot retention limit.

Snapshot-primary sessions are currently constrained
by the smaller checkpoint limit.

### Acceptance

- Valid snapshots commit and reload
- Previous generations are preserved
- Corrupt primaries are not overwritten
- Oversized sessions are rejected before writing
- Failure does not clear journal data
- Repeated saves do not duplicate records
- Legacy mode still loads
- Existing worlds are not migrated automatically

### Next step

Batch 22.4 should introduce a controlled in-memory
checkpoint rotation that updates its committed
baseline and journal together only after a
verified durable snapshot commit.

This must include rollback and restart tests.

Once rotation and accounting continuity are stable,
the project can begin the first autonomous
agriculture-and-household economy vertical slice.
