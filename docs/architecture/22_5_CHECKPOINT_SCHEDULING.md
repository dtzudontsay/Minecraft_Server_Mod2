
# Known World - Batch 22.5

## Automatic Economy Checkpoint Scheduling

Status: Integrated server-tick checkpoint trigger

### Purpose

Schedule economic snapshot checkpoints before
the current-session transaction journal reaches
the snapshot format's retention limit.

### Authority

Automatic checkpoint scheduling applies only
to explicitly activated snapshot-primary worlds.

Legacy compatibility remains unchanged.

No automatic migration or resource initialization.

### Default policy

Check interval: 200 server ticks
Checkpoint threshold: 8,000 pending transactions

Snapshot retention limit: 10,000 transactions

InventoryManager hard journal limit:
100,000 transactions

The checkpoint threshold leaves headroom before
the smaller snapshot retention limit.

### Tick integration

EconomyRuntime registers END_SERVER_TICK.

The scheduler counts ticks and evaluates whether
the economy requires a checkpoint.

If snapshot authority is active and the pending
journal reaches the threshold:

1. Validate current economic references.
2. Commit and verify the combined snapshot.
3. Rotate the committed current-session journal.
4. Update the committed historical baseline.
5. Resume normal tick processing.

### Failure behavior

If a scheduled checkpoint fails:

- The scheduler does not report success.
- The pending journal is not intentionally cleared.
- EconomyRuntime propagates the failure.
- Existing save files are not deliberately reset.

The server must not silently continue a normal
economic workload after checkpoint failure.

### Current limitations

- No autonomous production has been enabled.
- No guaranteed limit on transactions submitted
  between successive scheduler checks.
- No permanent historical transaction IDs.
- No campaign timestamps or acting NPC IDs.
- No cross-domain world-save atomicity.
- No process-wide filesystem lock.
- No guaranteed power-loss fsync durability.
- No automatic recovery from damaged snapshots.
- The journal remains bounded by existing limits.

The current implementation is suited to a
server-thread-driven simulation.

A future background economic worker must
coordinate its mutations and checkpoint timing
with the same authoritative inventory manager.

### Existing-world compatibility

New World (47) stays in legacy compatibility
unless snapshot authority was explicitly enabled.

No inventories are provisioned automatically.

No grain, timber or coin is generated.

No existing world data is reset.

### Acceptance

- Existing Gradle tests pass.
- Scheduler respects its tick interval.
- Below-threshold journals do not trigger saves.
- Legacy mode never schedules a checkpoint.
- Snapshot-primary mode checkpoints at threshold.
- Successful checkpoints rotate the journal.
- Failed checkpoints preserve pending transactions.
- Repeated checkpoints preserve inventory balances.
- Invalid scheduler policies are rejected.

### Next batch

Batch 22.6:

Begin the first functioning agricultural-production
and household food-consumption vertical slice.

Start with isolated, deterministic economic
operations before enabling the system globally.

Population, location, resource production and food
consumption must use real simulation identities
and remain save-compatible.
