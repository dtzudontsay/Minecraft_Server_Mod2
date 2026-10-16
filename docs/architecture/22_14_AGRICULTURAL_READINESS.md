
# Known World — Batch 22.14

## Agricultural Readiness and Cross-System Consistency

Status: Read-only integration audit
Autonomous agricultural activation: Disabled

### 1. Objective

Determine whether existing campaign data is
sufficiently consistent for a controlled
agricultural execution.

Do not construct another authoritative calendar,
population registry, holding registry or economy.

### 2. Existing systems reused

SimulationTimeContract:
Authoritative time observation

CampaignCalendar:
Historical campaign-day progression

LandedHolding:
Political and economic holding identity

HoldingStatus:
Current holding status

HoldingType:
Distinction between aggregate political domains
and individually productive holdings

CampaignSettlementAgricultureDispatcher:
Unified settlement agricultural preflight

SettlementFoodScenarioAdapter:
Existing NPC household identities and membership

InventoryManager:
Authoritative resource inventories

EconomyAuthorityService:
Economic snapshot authority and checkpointing

AgriculturalIntervalLedger:
Durable agricultural interval progression

### 3. Campaign day alignment

CampaignDayBoundaryCheck verifies:

- A valid one-day interval
- The requested interval end equals the current
  authoritative campaign day
- The campaign tick remainder is zero

It never equates routine Minecraft days with
historical campaign days.

It never advances either clock.

### 4. Holding readiness

Each enrolled holding must exist in the
authoritative landed-holding registry.

The recommended production integration supplies:

LandedHoldingManager::find

Holdings must currently have ACTIVE status.

REGION and TERRITORY holdings are rejected as
individual productive farms.

This prevents agricultural production from
being counted once at an aggregate territorial
level and again in individual subordinate estates.

Other holding types remain eligible for
further validation.

### 5. Geographic evidence

LandedHolding.worldLocationId is an existing
geographic identity.

AgriculturalDayEnrollment.settlement is an
existing SettlementId.

These are not interchangeable identifiers.

The audit requires explicit evidence that the
holding is correctly associated with the chosen
production settlement.

The audit does not infer that relationship
from matching names, arbitrary distances,
political ownership, or populationWeight.

A permanent authoritative geographic association
must be implemented before scenario-wide activation.

### 6. Population completeness

The existing household adapter currently counts
represented living individual NPCs.

That does not establish a complete settlement
population.

Population coverage must also account for
abstract inhabitants or demographic cohorts
where they exist.

The audit therefore requires independent evidence
that the settlement's consumption population is
complete and correctly reconciled.

A represented NPC must not also be counted in a
separate population cohort.

### 7. Agricultural production calibration

The existing yield model supports land area,
labor capacity, fertility, weather and season.

It does not prove that the input values for
a particular holding are scenario-authentic.

The audit requires evidence that agricultural
production inputs have been calibrated.

No automatic production values are generated
from political taxBase or populationWeight.

### 8. Existing economic preflight

The audit invokes the read-only inspect method
of CampaignSettlementAgricultureDispatcher.

This verifies the existing economic operation,
including:

- Inventory identities and ownership
- Settlement locations
- Household membership
- Living represented population
- Food demand
- Harvest arithmetic
- Farm reserves
- Journal capacity
- Snapshot-primary authority
- Durable interval progression

Errors are returned as readiness findings.

No ledger is initialized automatically.

No economic balances are changed.

### 9. Readiness levels

Controlled readiness means:

- All controlled-execution requirements pass
- Existing dispatcher preflight succeeds
- No controlled blockers remain

It does not activate any system.

Autonomous readiness additionally requires
cross-domain save consistency.

The current economy snapshots and interval ledger
are not one atomic transaction with the NPC,
political and campaign-calendar persistence files.

Therefore autonomous readiness remains blocked.

### 10. Important limitations

ScenarioEvidence is an interface for independently
established facts.

Its unverified implementation fails closed.

A caller returning true asserts that evidence
has been established; the boolean alone is not
independent proof that the world data is correct.

This batch does not implement the geographic
mapping or demographic cohorts themselves.

No automatic runtime activation occurs.

### 11. Existing-world safety

No EconomyRuntime modifications.

No new save formats.

No automatic resource production.

No automatic household population generation.

No automatic holding enrollment.

No change to existing campaign progression.

New World (47) remains unaffected.

### 12. Tests

Tests cover:

- Correct historical campaign-day alignment
- Wrong historical day rejection
- Partial day rejection
- Invalid daily interval rejection
- Routine/campaign time-scale separation
- Existing holding identity checks
- Inactive holding rejection
- Aggregate holding rejection
- Missing geographic evidence
- Missing population evidence
- Missing production calibration
- Missing interval ledger
- Read-only economic snapshot behavior
- Explicit controlled versus autonomous readiness

### 13. Next development stage

Batch 22.15 should begin supplying real evidence
for these readiness requirements through the
existing scenario and world systems.

Priorities:

1. Verified holding-to-settlement geographic links
2. Agricultural land and production capacity sources
3. Complete settlement demographic coverage
4. Separation of persistent NPCs from population cohorts
5. Performance evaluation of the complete population
6. Cross-domain checkpoint and recovery design

Continue using existing authoritative identity
and persistence systems rather than creating
parallel versions of them.
