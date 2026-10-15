
# Known World - Batch 22.0

## Explicit Economy Save Authority

Status: Controlled runtime integration

### Purpose

Introduce explicit, persistent save-format authority
for the Known World economy.

### Authority marker

Filename:
economy_authority.tsv

Version:
KNOWNWORLD_ECONOMY_AUTHORITY 1

Supported explicit authority:
SNAPSHOT

An absent marker preserves legacy compatibility.

An invalid marker causes startup rejection.

### Legacy compatibility

Without a marker:

- No saves: empty economy
- Legacy only: legacy manager
- Matching legacy/snapshot: legacy manager
- Conflicting formats: reject
- Snapshot only: reject
- Corrupt files: reject

No automatic migration occurs.

### Snapshot-primary mode

With a valid snapshot authority marker:

- Load the combined snapshot
- Validate economic owner references
- Restore inventory balances and IDs
- Save using combined snapshots
- Retain the existing legacy file unchanged

A corrupt primary snapshot must not silently
fall back to a previous generation.

Manual recovery remains required.

### Explicit activation

Snapshot authority can only be activated through
EconomyAuthorityService.activateMatchingSnapshot().

The operation requires matching legacy and snapshot
inventory states.

It must run with Minecraft shut down.

The whole world must be backed up before activation.

### Transaction limitation

Snapshot persistence includes the current in-memory
transaction journal.

Restoration currently starts a new session journal.

Historical transactions from previous sessions are
not yet retained across repeated loads.

Persistent long-term ledger continuity must be
implemented separately.

### Remaining limitations

- No world-level cross-domain atomic snapshots
- No filesystem process locking
- No complete power-loss durability guarantee
- No durable journal continuity across sessions
- No bounded journal rotation
- No automated recovery authorization
- No autonomous economic production
- No household consumption
- No canonical historical resource catalog

### Acceptance criteria

- Legacy worlds continue loading normally
- Empty worlds do not create resources
- Matching snapshot does not activate itself
- Explicit activation selects snapshot authority
- Invalid markers fail closed
- Snapshot-only mode requires a marker
- Snapshot writes leave legacy data untouched
- Combined snapshot restores balances correctly
- Previous snapshot generations are retained
- All Gradle tests pass

### Next batch

Batch 22.1 should establish persistent accounting
continuity and bounded checkpoint rotation before
activating large-scale autonomous economic activity.
