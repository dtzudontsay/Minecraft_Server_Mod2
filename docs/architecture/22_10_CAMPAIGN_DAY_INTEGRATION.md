
# Known World — Batch 22.10

## Shared Campaign-Day Economic Scheduling

Status: Runtime observation integrated
Autonomous agriculture: Disabled

### 1. Purpose

Establish a common campaign-day scheduling
contract for normal gameplay and accelerated
simulation.

Reuse existing systems instead of creating an
independent economic calendar.

### 2. Authoritative time

SimulationClock remains the authoritative
simulation-tick counter.

CampaignCalendar remains the authoritative
historical calendar.

SimulationTimeContract provides immutable
snapshots of the current combined time.

No new authoritative clock was introduced.

### 3. Existing scheduling contracts

Agricultural progression is assigned to:

SimulationPhase.ECONOMY

Scheduling policy:

SimulationSchedule.campaignDaily(
SimulationPhase.ECONOMY
)

An initial calendar snapshot does not count
as an advanced campaign day.

Day advancement must be observed explicitly.

### 4. Fast-forward integration

SimulationFastForwardService now owns a
CampaignDayObserverHub.

After advancing the campaign calendar and
executing existing life-cycle processing,
it publishes an immutable time snapshot.

The existing abstract social simulation then
continues unchanged.

Observers receive events in registration order.

Exceptions propagate rather than being silently
ignored.

There is no automatic retry after an observer
fails.

### 5. Normal gameplay integration

EconomyRuntime observes the authoritative
NpcSimulation campaign calendar during server
tick processing.

The current integration is observation-only.

Because this callback is outside the central
NpcSimulation tick method, its exact relative
ordering with other future mutating systems
is not yet guaranteed.

Before autonomous economic processing is
activated, central phase ordering must be
established and regression-tested.

### 6. Shared coordinator

CampaignAgricultureDayCoordinator consumes
SimulationTimeContract snapshots.

It has two modes.

MONITOR_ONLY:
- Records observed day transitions
- Tracks skipped observations
- Does not execute economic operations
- Does not manufacture missed campaign days
- Does not write persistence files

STRICT_EXECUTION:
- Requires exact one-day increments
- Uses the existing ECONOMY phase schedule
- Invokes one supplied day operation
- Ignores duplicate observations
- Blocks after an operation failure
- Blocks on skipped campaign days
- Never silently retries a failed operation

### 7. Agricultural interval semantics

The coordinator dispatches windows:

[startDayInclusive, endDayExclusive)

For ordinary progression, each window
contains exactly one campaign day.

The consuming agricultural system can use
its own holding-specific harvest period.

A daily boundary is not equivalent to a
daily harvest.

AgriculturalInterval remains the existing
harvest-boundary representation.

### 8. Existing-world compatibility

No automatic agricultural enrollment.

No household inventories are created.

No agricultural ledgers are initialized.

No grain is produced automatically.

No existing NPCs or settlements are changed.

No save formats are migrated.

Existing snapshot and checkpoint behavior
remains in place.

The runtime monitors campaign advancement
regardless of whether snapshot authority
has been activated.

Monitoring does not imply economic mutation.

### 9. Persistence and recovery

Batch 22.9 introduced a persistent agricultural
interval ledger and guarded economic runner.

Batch 22.10 does not change either format.

The in-memory coordinator is not a substitute
for durable interval execution tracking.

Before STRICT_EXECUTION is enabled in the
live runtime, every agricultural operation
must be coordinated with durable state.

A callback failure can occur after historical
time has advanced.

The current system does not roll back
CampaignCalendar, NPC lifecycle or society
state as one atomic transaction.

Cross-domain checkpointing is outstanding.

### 10. Performance

Passive observation is constant-time for
each received clock snapshot.

The coordinator does not scan NPCs,
households, holdings or inventories.

Fast-forward publishes one day notification
per simulated campaign day.

No additional per-household daily work
is enabled by this batch.

### 11. Future integrations

NPC lifecycle and population:
Population changes must affect subsequent
economic demand and labor.

Holdings and agriculture:
Cultivated area and harvest settings must
come from authoritative holding data.

Household organizations:
Consumption must use real household
membership and existing inventory ownership.

NpcNeedManager:
Food availability and consumption must
influence hunger without duplicate
consumption accounting.

NpcActionProcessor:
Physical work must affect the shared
economic system without double-counting.

SimulationFastForwardService:
The same economic day operation must run
in accelerated progression.

NpcSimulation:
Final authoritative phase ordering must
be established before economic mutation
is enabled during normal gameplay.

### 12. Acceptance criteria

- Existing tests remain passing
- Existing campaign calendar is unchanged
- Existing simulation clock is unchanged
- Fast-forward supports day observers
- Observers execute deterministically
- Duplicate registration is rejected
- Observer failures propagate
- Live economy runtime observes campaign days
- Startup does not fabricate a day advance
- Duplicate day observations are ignored
- Strict mode rejects missing days
- Failed strict operations are blocked
- No automatic harvests are activated

### 13. Next development stage

Batch 22.11 should establish the final
campaign-day execution ordering and
controlled end-to-end integration tests.

The next implementation should verify:
- A daily economic operation through live time
- The equivalent operation through fast-forward
- Identical agricultural inventory outcomes
- Restart and interval-ledger safety
- No double-counting of persistent NPCs
- No implicit population or farmland creation

Autonomous world-wide agricultural progression
remains disabled until those checks and scenario
readiness requirements are satisfied.
