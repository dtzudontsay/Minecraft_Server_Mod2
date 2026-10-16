
# Known World — Batch 22.11

## Executable Campaign-Day Agriculture

Status: Controlled end-to-end execution
Automatic world activation: Disabled

### 1. Objective

Connect the agricultural domain mechanics from
Batches 22.6 through 22.9 with the shared campaign-day
scheduling contract established in Batch 22.10.

Use the same agricultural operation for normal-style
tick advancement and accelerated-style day advancement.

### 2. Existing systems reused

SimulationClock:
Authoritative simulation tick

CampaignCalendar:
Authoritative historical campaign day

SimulationTimeContract:
Immutable time snapshot

CampaignAgricultureDayCoordinator:
Strict campaign-day dispatch and retry protection

CampaignDayObserverHub:
Shared notification contract

OrganizationManager and NpcAffiliationManager:
Household identity and membership

NpcRegistry:
Persistent individual population

SettlementFoodScenarioAdapter:
Household and inventory resolution

SettlementAgricultureRunner:
Guarded agricultural execution

AgriculturalIntervalLedger:
Durable interval progress

EconomyAuthorityService:
Verified economy snapshot commits

No replacement population or household registry.

### 3. Agricultural enrollments

AgriculturalDayEnrollment provides:

- Existing HoldingId
- Existing farm InventoryId
- Existing SettlementId
- Harvest interval length
- Agricultural yield parameters
- Protected farm reserve
- Food requirement per person per day

The enrollment is explicitly provided by the caller.

It is not yet automatically generated from
landed-holding or geographic scenario data.

### 4. Daily operation

CampaignAgricultureDayOperation implements
CampaignAgricultureDayCoordinator.DayOperation.

The operation requires exactly one campaign-day window.

Example:

[29, 30)

A daily operation may include a harvest only when
its holding's configured harvest boundary is crossed.

### 5. Execution sequence

First, preflight all enrolled farms.

The preflight verifies:

- Snapshot-primary economy authority
- Readable and consistent interval ledger
- Valid farm and settlement identities
- Existing household inventories
- Existing living household population
- No unassigned individual residents
- Acceptable journal transaction budget
- Correct campaign-day progression

Only after all preflight checks succeed does execution
begin.

Each farm then uses SettlementAgricultureRunner:

1. Verify references and household state
2. Commit existing economic baseline
3. Persist PREPARED interval marker
4. Execute agricultural production
5. Transfer grain to households
6. Consume food
7. Commit and verify economy snapshot
8. Persist COMPLETED interval marker

### 6. Duplicate consumption protection

A single settlement may have only one enrolled farm
in this version.

This is a temporary restriction.

SettlementFoodCycle currently distributes food to
all represented households in the settlement.

Executing it twice for different farms would cause
household food demand to be charged twice.

A future multi-farm coordinator must:

- Combine local production from multiple farms
- Respect separate inventory ownership
- Apply reserves and taxation rules
- Calculate household demand once per interval
- Distribute available food through explicit transfers

The restriction must not be removed until that
model is implemented and tested.

### 7. Deterministic ordering

Enrolled farms execute in ascending HoldingId order.

Households retain the deterministic ordering already
implemented by SettlementFoodCycle.

Identical initial data and time sequences should
produce identical agricultural inventory outcomes.

### 8. Failure and recovery

The strict campaign-day coordinator blocks
further execution after a failed operation.

The agricultural interval ledger blocks replay
of prepared intervals.

If a later farm fails after an earlier farm committed,
there is no cross-holding rollback.

The calling strict coordinator must remain blocked
until the campaign state is reconciled.

A successful in-memory coordinator observation
is not proof of durable cross-domain consistency.

### 9. Live and accelerated testing

Tests exercise two time advancement patterns.

Live-style:
Advance SimulationClock one tick at a time and
advance CampaignCalendar through tick boundaries.

Accelerated-style:
Advance SimulationClock by the campaign-day tick
duration and advance CampaignCalendar one day.

Both invoke the same strict agricultural operation.

Tests compare:
- Persisted economic snapshots
- Agricultural interval progression
- Farm grain balances
- Harvest and consumption results

This verifies agricultural boundary equivalence
for controlled fixtures.

It does not yet verify complete equivalence
between the full live NPC simulation and
SimulationFastForwardService, including lifecycle
and social side effects.

### 10. Existing-world safety

No changes to EconomyRuntime.

No new server lifecycle registration.

No automatic farming in existing worlds.

No automatic provisioning of households.

No automatically initialized interval ledger.

No implicit population generation.

No scenario-wide activation.

The existing MONITOR_ONLY runtime mode is retained.

### 11. Outstanding integration requirements

The full CK-style economy still requires:

- Authoritative demographic cohorts
- Household economic identity rules
- Holding-to-settlement production mapping
- Multi-farm settlement allocation
- Permanent farmland and labor capacity data
- Climate and seasonal agricultural modifiers
- Integration with NPC hunger and work
- Land ownership, rents and taxation
- Consistent domain execution ordering
- Cross-domain checkpoint and recovery protocol
- Large-scale population performance benchmarks
- Verified live-versus-fast-forward equivalence

### 12. Acceptance

- A real agricultural day produces grain
- Household consumption occurs exactly once
- The next non-harvest day does not repeat production
- Both controlled time advancement styles produce
  identical economic snapshots
- Existing household identity is reused
- Farm execution ordering is deterministic
- Preflight rejects unresolved settlement residents
- Preflight rejects invalid ledgers
- Previously committed intervals cannot be replayed
- Multiple farms cannot double-charge one settlement
- Multiple independent settlements can execute
- No live-world agricultural activity is enabled

### 13. Next batch

Batch 22.12 should establish the first proper
multi-farm settlement production and distribution
model.

It should eliminate the temporary one-farm restriction
by calculating household food demand once per settlement
and coordinating contributions from multiple farm
inventories.

Political ownership and the existing holding system
must remain authoritative.

No automatic global activation until scenario
population and agricultural data are ready.
