
# Known World - Batch 22.2

## Journal Limits and Checkpoint Safety

Status: Hard journal limit implemented
Runtime rotation: Not yet enabled

### Purpose

Prevent unbounded economic transaction accumulation
while preserving inventory accounting integrity.

### Journal capacity

Default maximum:

100,000 current-session transactions

InventoryManager also supports a custom limit.

At the limit:

- Production is rejected
- Consumption is rejected
- Transfers are rejected
- Inventory balances remain unchanged
- No transaction entries are silently discarded

The journal limit does not prevent Minecraft
from saving existing inventory balances.

### Checkpoint validation

EconomyCheckpointGuard verifies:

- Next inventory ID matches
- Inventory identities and balances match
- Captured transactions include the full
  current-session journal
- Persisted snapshots match captured snapshots

The guard does not itself write or rotate journals.

### Existing snapshot format

The combined snapshot format remains version 1.

The existing retained historical transaction
limit remains 10,000 entries.

No save-format migration occurs in this batch.

### Critical limitation

The live journal capacity and historical retention
limits are not the same thing.

A session could accumulate more than 10,000
transactions before reaching the hard limit
of 100,000.

The existing snapshot retention code can omit
older current-session records in this situation.

Therefore:

- Hard journal limits prevent unlimited growth.
- They do not guarantee all session transactions
  will fit into a combined snapshot.
- Automatic rotation must not be enabled yet.
- Full checkpoint verification must precede
  any future journal clearing.

The current EconomyAuthorityService is unchanged.

### Remaining requirements

Before high-volume autonomous production:

- Integrate checkpoint verification into saving
- Define an authoritative committed baseline
- Rotate the current journal only after a
  verified durable checkpoint
- Preserve retained history across rotations
- Handle failed checkpoint writes without loss
- Establish transaction identities across rotations
- Test repeated save and recovery boundaries
- Keep historical memory use bounded

### Existing-world compatibility

No automatic economy initialization.

No automatic migration.

No resource creation.

Legacy persistence remains compatible.

New World (47) remains unaffected unless
economic operations are explicitly invoked.

### Acceptance

- All existing tests pass
- Journal rejects operations at capacity
- Rejected operations do not mutate balances
- Rejected operations do not consume sequence IDs
- Checkpoint capture validation passes
- Persisted snapshot equality validation passes
- Restored inventories retain balances
- No automatic journal clearing occurs

### Next batch

Batch 22.3:

Integrate safe checkpoint validation with the
snapshot-primary save path and establish a
verified rotation protocol.

Only after that should we enable significant
autonomous agricultural and household activity.
