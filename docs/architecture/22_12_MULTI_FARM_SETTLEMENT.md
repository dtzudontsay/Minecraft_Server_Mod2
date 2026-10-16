
# Known World — Batch 22.12

## Multi-Farm Settlement Agriculture

Status: Controlled integrated execution
Automatic campaign activation: Disabled

### 1. Purpose

Extend the agricultural economy from one farm
per settlement to multiple independently owned
farms supplying the same household population.

This batch does not introduce a new resource
ownership system, household registry, settlement
registry, or historical calendar.

### 2. Existing systems reused

HoldingId:
Existing landed holding identity

SettlementId:
Existing settlement identity

OrganizationId:
Existing household identity

NpcAffiliationManager:
Household membership and residence

NpcRegistry:
Living persistent individuals

InventoryManager:
Authoritative resource balances and transfers

AgriculturalYieldModel:
Farm production calculations

HouseholdFoodDemandModel:
Population-based food requirements

SettlementFoodScenarioAdapter:
Authoritative household and farm validation

CampaignAgricultureDayCoordinator:
Campaign-day execution contract

EconomyAuthorityService:
Verified checkpoint commits

AgriculturalIntervalLedger:
Persistent interval replay protection

### 3. Independent farm ownership

Every farm uses its own holding-owned inventory.

Agricultural production is recorded against
that farm's actual inventory.

Grain is not silently moved into a new
central settlement inventory.

All household allocations are explicit
farm-to-household inventory transfers.

A later tax, rent, or market system may
change which owner receives or distributes
particular resources.

### 4. Agricultural production

Each enrolled holding specifies:

- HoldingId
- Farm InventoryId
- Harvest period
- Yield model inputs
- Protected grain reserve

Harvest timing uses AgriculturalInterval.

Farm production occurs only when its
configured harvest boundary is crossed.

Farms may use different harvest periods
and production settings.

### 5. Household demand

Household population comes from the existing
social adapter.

The model calculates household food demand
once per settlement interval.

This prevents double consumption when
several farms contribute to one settlement.

Existing household grain is counted before
new food is allocated.

### 6. Distribution policy

The initial policy is proportional allocation
according to unmet household food demand.

The algorithm uses largest-remainder rounding.

Households are ordered by InventoryId.

Farms are ordered by HoldingId.

Available farm grain is contributed in
ascending holding order.

This donor-priority rule is a provisional
deterministic policy, not a canonical
medieval land-tenure rule.

Future policies can incorporate:

- Food markets
- Prices and household purchasing power
- Land tenure and rents
- Feudal taxation
- Noble household privileges
- Seed reserves
- Military requisitioning
- Emergency rationing

### 7. Accounting

One multi-farm operation may record:

- One production per harvesting farm
- Multiple individual farm-to-household transfers
- One consumption per household with food

No operation creates a transfer for zero grain.

All authoritative changes use InventoryManager.

Farm balances and household balances are
checked against the precomputed plan.

### 8. Preflight guarantees

Before resource mutation, the model checks:

- Existing farm inventories
- Correct holding ownership
- Correct settlement locations
- Duplicate inventory identities
- Harvest arithmetic
- Reserve restrictions
- Household demand calculations
- Allocation arithmetic
- Journal capacity
- Expected remaining resource balances

Unexpected runtime failures are not covered
by a general rollback transaction.

### 9. Persistent runner

MultiFarmSettlementAgricultureRunner manages
one settlement's group of enrolled holdings.

It uses the following sequence:

1. Validate snapshot authority.
2. Check existing interval progress.
3. Resolve household membership.
4. Verify population consistency.
5. Prepare a full settlement economic plan.
6. Commit the existing economy baseline.
7. Mark all farm intervals PREPARED.
8. Execute all settlement resource mutations.
9. Verify the economy snapshot.
10. Mark all farm intervals COMPLETED.

The entire resource mutation stage is one
coordinated InventoryManager operation.

### 10. Persistence limitations

All farm and household inventory balances
are captured in one verified economic snapshot.

However, the per-holding interval ledger
markers are stored separately.

A crash may interrupt interval preparation
or completion between ledger writes.

Automatic replay is prohibited in such cases.

The implementation does not provide:

- Cross-file atomic transactions
- Automatic rollback
- Distributed transaction locking
- Guaranteed power-loss durability
- Automatic recovery of ambiguous intervals

The runner fails closed when it detects
unsafe interval or checkpoint state.

### 11. Existing single-farm compatibility

AgriculturalFoodCycle remains unchanged.

SettlementFoodCycle remains unchanged.

SettlementAgricultureRunner remains unchanged.

CampaignAgricultureDayOperation remains unchanged.

The older Batch 22.11 dispatcher still prevents
multiple farms in one settlement.

The new multi-farm runner is an explicit
additional execution path.

A future batch should consolidate dispatch
once multi-farm durability and multi-settlement
coordination have been validated.

### 12. Existing-world safety

No automatic enrollment.

No resource seeding.

No new household creation.

No automatic harvests.

No changes to EconomyRuntime.

No changes to existing save formats.

No automatically initialized ledgers.

New World (47) remains in its previously
selected economy authority mode.

The new runner must be explicitly constructed
and invoked by a controlled caller.

### 13. Scaling considerations

The settlement operation processes existing
households rather than creating new entities.

Allocation and transfer planning are
deterministically ordered.

Very large populations still require cohort
aggregation and efficient inventory indexing.

A future benchmark must measure:

- Farms per settlement
- Households per settlement
- Transactions per day
- Checkpoint file sizes
- Fast-forward cost
- Memory usage
- Household and holding lookup costs

### 14. Future integration

The full CK-style economy still requires:

- Authoritative non-individual population cohorts
- Farm labor participation from NPC work systems
- Cultivated land and land-use data
- Geographic and climatic productivity
- Multi-settlement agricultural scheduling
- Markets, wages, rents, taxation and prices
- Food shortage consequences for NPC needs
- Food transport between settlements
- Cross-domain persistence and recovery
- Central day-phase execution ordering
- Performance benchmarking at continental scale

These are planned integration requirements,
not reasons to duplicate existing systems.

### 15. Acceptance criteria

- Multiple farms can produce within one settlement
- Each farm retains independent ownership
- Household food need is calculated once
- Households can receive grain from several farms
- Supply never exceeds available grain
- Protected farm reserves remain unavailable
- Allocation is deterministic
- Existing household stores are respected
- Accounting transactions name actual owners
- Journal-capacity failures reject before mutation
- A complete settlement cycle survives a snapshot
- Per-holding interval progress is persistent
- Completed intervals cannot be replayed
- Unresolved residents block execution
- Existing tests remain passing

### 16. Next batch

Batch 22.13 should unify the single-farm and
multi-farm campaign-day dispatch mechanisms.

It should group all enrolled holdings by settlement,
calculate food demand once per settlement, and
coordinate independent settlement operations through
the existing campaign-day scheduling contract.

The integration must preserve the established
holding, organization, NPC, settlement and economy
identities.

Autonomous world-wide economic progression should
remain disabled until scenario readiness and
cross-domain persistence requirements are met.
