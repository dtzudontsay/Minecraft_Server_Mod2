
# Known World - Batch 21.6

## Economic Snapshot Recovery

Status: Isolated implementation

### Purpose

Provide a recoverable previous generation for
the combined economic inventory and journal snapshot.

### Storage

Primary:
economy_snapshot_v1.tsv

Backup:
economy_snapshot_v1.tsv.bak

The existing economy_inventories.tsv remains the
authoritative live runtime save.

### Save behavior

On the first save, write a validated primary.

On later saves:

1. Validate the existing primary.
2. Copy it to the previous-generation backup.
3. Atomically publish the backup.
4. Write the new primary snapshot.
5. Validate the new primary.

A corrupt existing primary must not be overwritten
through an ordinary save operation.

### Recovery behavior

readBest() first reads the primary.

If the primary is invalid, a backup can be loaded.

The returned result explicitly identifies whether
the primary or backup was used.

Reading a backup does not automatically modify
the primary file.

Restoring a backup as primary requires an explicit
call and must be preceded by operator review.

### Limitations

This is not yet a complete crash-proof system.

Remaining limitations include:

- No disk fsync guarantees
- No cross-domain save coordination
- No resource-catalog version migration
- No durable historical transaction IDs
- No bounded in-memory journal
- No unlimited historical generations
- No automatic recovery during server startup
- No migration from legacy inventory persistence
- No replay-based historical validation
- No production-scale stress tests

### World compatibility

This batch does not change EconomyRuntime.

It does not alter existing worlds.

New snapshot recovery code is only exercised
through isolated tests.

### Next implementation

Batch 21.7 should establish a bounded transaction
history policy and explicit migration preparation.

Only after migration and recovery testing should
the live runtime adopt combined economic snapshots.
