
# Known World — Batch 22.13

## Unified Agricultural Campaign Dispatcher

Status: Controlled execution
Automatic campaign activation: Disabled

### 1. Objective

Establish one recommended agricultural campaign-day
entry point for both single-farm and multi-farm
settlements.

Continue using the authoritative Known World
population, household, holding, settlement, time,
and economic accounting systems.

### 2. Existing systems reused

SimulationClock:
Authoritative simulation ticks

CampaignCalendar:
Authoritative historical days

CampaignDayObserverHub:
Campaign-day notifications

CampaignAgricultureDayCoordinator:
Strict day-boundary dispatch

HoldingId:
Existing holding identity

SettlementId:
Existing settlement identity

NpcRegistry and NpcAffiliationManager:
Living individual household membership

OrganizationManager:
Persistent household organizations

SettlementFoodScenarioAdapter:
Authoritative household and farm resolution

MultiFarmSettlementFoodCycle:
Production, allocation and consumption

MultiFarmSettlementAgricultureRunner:
Guarded settlement execution

InventoryManager:
Authoritative resource balances

EconomyAuthorityService:
Verified economy snapshots

AgriculturalIntervalLedger:
Durable replay protection

### 3. Unified execution

CampaignSettlementAgricultureDispatcher is the
recommended entry point for new agricultural
campaign-day execution.

The dispatcher groups enrollments by SettlementId.

Each group has one or more holding-owned farms.

Every group uses the same
MultiFarmSettlementAgricultureRunner.

There is no separate single-farm calculation path
inside the new dispatcher.

### 4. Deterministic ordering

Settlements are processed by ascending SettlementId.

Within each settlement, holdings are processed by
ascending HoldingId.

Households are processed by ascending InventoryId.

Supply distribution uses the existing deterministic
multi-farm allocation policy.

Input enrollment order cannot change the intended
economic outcomes.

### 5. Consumption accounting

Every settlement calculates household demand once
per campaign-day interval.

A settlement with multiple farms does not repeat
its household food requirements for each farm.

Farm-to-household resource transfers remain
individually journaled.

Each household with food consumption generates
one consumption operation for the settlement day.

### 6. Global preflight

The dispatcher inspects every enrolled settlement
before executing the first one.

Preflight checks include:

- Snapshot-primary economy authority
- Valid existing holding and settlement identities
- Farm inventory ownership and location
- Household membership and population
- Missing or conflicting household inputs
- Unassigned living residents
- Harvest arithmetic
- Farm reserve restrictions
- Household demand and food allocations
- Economic journal capacity
- Agricultural interval continuity
- Previously prepared or completed intervals

If any preflight fails, no settlement resource
mutation for that dispatch is started.

### 7. Execution

After successful global preflight, settlement
runners execute in ascending settlement order.

Each runner:

1. Creates a verified pre-interval checkpoint.
2. Marks its holdings PREPARED.
3. Executes one settlement food cycle.
4. Commits and verifies the economy snapshot.
5. Marks its holdings COMPLETED.

The dispatcher returns an ordered result for all
successfully executed settlements.

### 8. Failure and recovery

The dispatcher blocks after an execution failure.

An interrupted holding interval cannot be
automatically replayed.

The interval ledger remains authoritative for
durable agricultural progression.

Cross-settlement atomicity is NOT implemented.

A failure during a later settlement may occur
after an earlier settlement successfully committed.

No automatic global rollback is attempted.

The complete world save must eventually coordinate
calendar, NPC state, holding state, economy state,
and interval progress in one recovery protocol.

### 9. Compatibility

The existing Batch 22.11
CampaignAgricultureDayOperation remains available.

The existing Batch 22.9
SettlementAgricultureRunner remains available.

Older tests and callers do not need immediate
migration.

New campaign agriculture code should use
CampaignSettlementAgricultureDispatcher.

The old APIs are compatibility paths, not
additional recommended production schedulers.

Do not register both old and new dispatchers for
the same holding and campaign interval.

### 10. Existing-world protection

EconomyRuntime remains MONITOR_ONLY for agriculture.

No automatic holding enrollment.

No automatic inventory provisioning.

No automatic household creation.

No new grain production on world startup.

No change to the economy snapshot format.

No change to the agricultural interval ledger format.

Existing campaign saves remain unchanged unless
an explicitly configured dispatcher is executed.

### 11. Outstanding CK-style integration

Population:
Persistent individuals and abstract population
cohorts must be reconciled without double-counting.

Labor:
Farm production must use actual labor availability
and existing NPC work/occupation systems.

Land:
Cultivation and agricultural capacity must derive
from actual holdings, geography and land use.

Climate:
Seasonal and regional conditions must feed into
the agricultural yield model.

Needs:
Food shortages must affect NPC health and needs
through existing authoritative systems.

Politics:
Holding ownership must affect taxation, rents,
grain requisitioning and government obligations.

Markets:
Households and farms require trading, prices,
transport and purchasing-power decisions.

Persistence:
Cross-domain atomic checkpointing and recovery
are required before autonomous continent-scale use.

Performance:
The dispatcher needs large-scale benchmarking with
many settlements, farms and household cohorts.

### 12. Acceptance criteria

- One farm per settlement works
- Multiple farms per settlement work
- Multiple settlements advance in one campaign day
- Household food demand is calculated once per settlement
- Farm ownership is preserved
- Global preflight rejects known unsafe inputs
- Deterministic execution ordering is preserved
- Verified economic snapshots remain authoritative
- Durable interval ledgers prevent replay
- Live-style and accelerated-style day advancement
  produce identical economic outcomes in controlled tests
- Existing suites remain passing
- No automatic campaign-wide agriculture is enabled

### 13. Next stage

Batch 22.14 should focus on execution readiness and
cross-domain progression consistency.

Before automatic activation, the system must resolve
population coverage, settlement-to-holding geography,
durable calendar/economy synchronization, and
performance requirements.

The existing broader CK-style roadmap remains active.
