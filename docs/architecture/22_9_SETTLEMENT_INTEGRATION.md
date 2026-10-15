
# Known World - Batch 22.9

## Existing Household Integration and Agricultural Interval Safety

Status: Explicit integration path
Automatic campaign scheduling: Disabled

### Objective

Connect the agricultural economy to the established
Known World social identity and economy systems.

Prevent accidental repeated harvests after restarts.

### Authoritative identities

Individual NPC:
NpcId, NpcRegistry

Residence:
NpcAffiliationManager

Household:
OrganizationId with OrganizationType.HOUSEHOLD

Farm:
HoldingId

Farm inventory owner:
EconomicOwner.holding

Household inventory owner:
EconomicOwner.organization

Economic inventory location:
SettlementId

No replacement household or population registry
is introduced in this batch.

### Household population

Only living persistent NPCs with a matching
residence and household affiliation are counted.

An organization must exist and have HOUSEHOLD type.

If a household has an explicit seat settlement,
it must match the represented residence.

Every represented household must possess exactly
one existing inventory in that settlement.

Known residents without household assignments
block the integrated runner.

Cohort population is not implemented here.

The existence of household organizations does
not by itself establish total regional population.

### Holding and settlement relationships

The farm inventory must be owned by the
specified existing holding.

It must be located in the selected settlement.

The holding must exist in the authoritative
holding registry.

This does not prove that the settlement is
geographically within the holding's territory.

A canonical holding-to-settlement geographic
mapping is still required before global activation.

Population weights and tax-base weights must not
be interpreted as actual population or farmland.

### Interval ledger

File:
agriculture_intervals_v1.tsv

The ledger contains one progress record per holding.

Records store:
- Last completed interval end
- Optional prepared interval end

Each record is validated on loading.

The file uses:
- Versioned header
- Deterministic row ordering
- SHA-256 checksum
- Atomic file replacement
- Readback verification

The ledger requires explicit initialization.

A missing or corrupted ledger blocks interval
execution rather than resetting progress.

### Interval execution

Only explicitly activated snapshot-primary
economies can execute guarded agricultural cycles.

The sequence is:

1. Resolve real household membership.
2. Reject unresolved residents.
3. Validate economy references.
4. Commit the pre-interval economy baseline.
5. Durably mark interval PREPARED.
6. Execute agriculture and food consumption.
7. Commit and verify economic snapshot.
8. Durably mark interval COMPLETED.

Previously completed intervals cannot be repeated.

An interrupted prepared interval cannot be
automatically replayed.

### Critical recovery limitation

The agricultural ledger and economic snapshot
remain separate physical files.

They do not form a single atomic world save.

A crash can leave:
- PREPARED with old inventory balances
- PREPARED with newly committed balances
- A damaged checkpoint requiring manual recovery

In all these cases automatic retry is refused.

There is no automatic rollback or automatic
discard of prepared interval records.

Full cross-domain atomic checkpointing and
manual recovery tooling remain future work.

### Performance

The ledger stores bounded per-holding progress
rather than every historical interval.

The household adapter scans current affiliations
and inventories.

This is suitable for a small integration benchmark.

Larger campaigns will need indexed household
residence and inventory ownership lookups.

### Existing systems to connect later

SimulationClock and CampaignCalendar:
Authoritative agricultural scheduling

SimulationFastForwardService:
Equivalent accelerated economic progression

NpcNeedManager:
Hunger and deprivation consequences

NpcActionProcessor and NpcRoutineService:
Labor and productive WORK actions

LandedHoldingManager:
Agricultural estate relationships

World geography and climate rasters:
Environmental agricultural inputs

Future population cohorts:
Non-individual population demand and labor

Political ownership:
Rents, taxes and resource obligations

### Existing-world safety

EconomyRuntime is unchanged.

The integrated runner is not invoked automatically.

No household is created automatically.

No inventory is provisioned automatically.

No food is generated automatically.

No agricultural interval ledger is initialized
merely because Minecraft loads a save.

New World (47) remains unchanged.

### Acceptance

- Existing households retain their OrganizationId.
- Existing NPC affiliations determine population.
- Missing household inventories fail closed.
- Unassigned residents are reported.
- Farm inventory ownership is validated.
- Ledger persists across restarts.
- Repeated intervals cannot execute twice.
- Interrupted intervals cannot automatically retry.
- Verified economy commits preserve balances.
- Existing tests remain passing.

### Next batch

Batch 22.10 should establish a shared campaign-day
execution contract for agriculture.

It must reuse the existing CampaignCalendar,
SimulationSchedule and SimulationPhase architecture.

Normal gameplay and accelerated simulation must call
equivalent domain operations.

No global autonomous production should be enabled
until the remaining population and geographic
integration requirements are satisfied.
