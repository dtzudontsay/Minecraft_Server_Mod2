
# Known World - Batch 21.9

## Authoritative Economy Save Selection

Status: Runtime integration

### Objective

Prevent ambiguous economic save formats from
silently changing the authoritative inventory state.

### Current authority

The legacy inventory file remains authoritative:

economy_inventories.tsv

Combined snapshots remain transitional:

economy_snapshot_v1.tsv

### Startup selection

NEW_EMPTY:
Neither file exists. Start with empty inventories.

LEGACY:
Only legacy persistence exists. Load it.

LEGACY_WITH_MATCHING_SNAPSHOT:
Both representations match. Load legacy.

SNAPSHOT_ONLY:
Reject automatic selection.

CONFLICT:
Reject automatic selection.

INVALID_DATA:
Reject automatic selection.

### Recovery artifacts

Orphaned backups and known temporary economic
files are rejected before startup selection.

They require explicit operator review.

### Runtime safety

EconomyRuntime uses EconomySaveSelector to load
the economy.

Live owner reference validation remains enabled.

There is no automatic population or resource seeding.

In the matching-snapshot state, the runtime does
not write the legacy file during normal saves.

This prevents an immediate silent divergence
between legacy and snapshot formats.

### Current restrictions

No autonomous economy is enabled.

No economic resource transactions are triggered
by this batch.

A matched snapshot does not automatically become
authoritative.

No startup migration occurs.

### Known limitations

- No cross-domain atomicity
- No process-wide filesystem lock
- No authoritative-format marker
- No automatic snapshot-primary selection
- No durable ledger replay validation
- No transaction journal compaction
- No rollback orchestration
- No automatic reconciliation
- No live end-to-end economic transaction test

### Acceptance

- All Gradle tests pass.
- Empty-world startup remains compatible.
- Valid legacy saves load.
- Matching saves load without rewriting snapshots.
- Conflicting files are rejected.
- Corrupted files are rejected.
- Recovery artifacts are recognized.
- Existing NPC and society runtimes initialize.

### Next batch

Batch 22.0 should prepare a controlled snapshot-primary
transition with a versioned world-level authority marker
and explicit backup/rollback procedures.

After the transition is reliable, begin the first
actual household food consumption and agricultural
production vertical slice.
