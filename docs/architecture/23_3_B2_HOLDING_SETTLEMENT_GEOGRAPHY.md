
# Known World — B2.1 Holding and Settlement Geography

Status: Implementation supplied; build validation pending
Scenario: AGOT 298 AC
First reconciliation target: Winterfell V3

## Purpose

Unify the geographic identities used by settlements, holdings,
population, NPC affiliation and physical world generation.

Do not equate a holding with a generated town.

## Current sources of authority

Geographic identity:
WorldReferenceCatalog

Geographic map coordinates:
FeatureLocationResolver and regional map calibration

Political holdings:
LandedHoldingManager

Simulated settlement identity and center:
SettlementManager

Scenario starting settlements:
data/knownworld/scenarios/agot_298_ac/settlements.json

Physical Winterfell V3 generation:
WinterfellWorldgenV3 and its generated structure plan

Individual NPC identities:
NpcSimulation and AuthoredIdRegistry

## Audited data

The 298 AC landed holding file currently has 259 records.

Types:
- CASTLE: 171
- CITY: 17
- TOWN: 6
- FORTRESS: 2
- ESTATE: 17
- TERRITORY: 34
- REGION: 12

These classifications are political holding types,
not the future MAJOR/MEDIUM/SMALL physical build tiers.

At present, only Winterfell has an explicitly bootstrapped
scenario settlement in settlements.json.

The full set of possible sites therefore needs a separate
placement and settlement-type audit before world generation.

## Winterfell inconsistency

Previous scenario settlement:
- Dimension: minecraft:overworld
- X: 3.5
- Y: 106
- Z: 3.5

Winterfell V3 physical worldgen origin:
- X: -4050173
- Z: -1976496

Current B2.1 representative settlement point:
- X: -4050155.5
- Y: 106, provisional
- Z: -1976441.5

This point corresponds to the existing V3 Great Keep entrance.

It is a practical representative location, not a claim that
all of Winterfell has a radius of sixteen blocks.

The vertical coordinate still requires terrain/building
entry validation before it becomes a walkable path target.

## Migration behavior

New Known World saves:
- Load the revised settlement center from scenario data.
- Receive the existing stable Winterfell settlement authored ID.

Existing Known World saves:
- Load their saved settlement identities normally.
- Check whether Winterfell still has the old X/Z
  placeholder at (3.5, 3.5).
- If so, update the existing settlement center.
- Preserve SettlementId, name, type and all references.
- Save the normal NPC simulation state.
- Do not move any existing NPC or physical villager.

Other center coordinates:
- Remain unchanged, including manually corrected sites.
- Must never be silently moved to an inferred marker.

This is a narrow, intentionally safe migration, not a
general settlement relocation mechanism.

## What B2.1 actually implements

1. Correct physical scenario position for Winterfell.
2. Guarded legacy-center migration on server startup.
3. Read-only linkage through existing canonical managers.
4. Map placement / unmapped holding classification.
5. Holding audit commands.
6. Regression tests for identity and scenario location.

No new NPC population is created by B2.1.
No new holding is constructed.
No existing structure is regenerated.
No name is invented for an unresolved place.

## Commands

/kwplaces status

Show holding coverage and simulated settlement count.

/kwplaces winterfell

Show the persistent settlement ID, current center, scenario
target and whether the legacy center was migrated.

/kwplaces holding winterfell_domain

Show political type, current owner/holder links, geographic
reference, source provenance and map placement where available.

/kwplaces audit

Summarize unmapped possible physical sites and show the
first ten entries requiring future placement research.

## B2.2 — Complete coordinate and settlement identity audit

Next work:
- Reconcile known atlas markers and user-confirmed overrides.
- Preserve uncertain and disputed placements separately.
- Match existing research dossiers to canonical reference IDs.
- Produce an exportable audit of every holding and location.
- Distinguish unknown settlement names from unknown positions.
- Support more than one settlement per political holding.
- Connect physical and abstract NPC positions to residence.
- Explicitly establish site position and territory separately.

## B3 — Manual holding-to-generation classification

Do not derive construction scale directly from HoldingType.

Define the physical generator tier separately:
- MAJOR
- MEDIUM
- SMALL
- NO_SETTLEMENT
- UNKNOWN

Use existing research to recommend tiers.
Allow explicit manual overrides.
Do not fabricate sites for REGION or TERRITORY entries.
Some political domains contain multiple settlements.

## B4 — Population identity and residence

Every resident must have one authoritative identity.

Residence must resolve to the canonical settlement identity.

A rendered villager is only a physical projection of that
individual and must not create another simulated person.

Population aggregation must not create invisible duplicate
headcounts.

## Continuing technical debts

A4:
Witness and rumor propagation still require live end-to-end
validation against true abstract NPC positions.

A5:
Personal player relationships have not yet been verified
across every save/reload and conversation state.

B1:
The Minecraft-day bridge works at startup but still needs
full coordination with SimulationClock, routine scheduling,
economy day observers, and fast-forward.

Worldgen:
Steep terrain, navigation, building utility, density and
existing chunk migrations remain future construction work.

## Release gate

Required for B2.1:
- Compile project and run automated tests.
- Read /kwplaces status without exceptions.
- Confirm Winterfell is no longer at the old origin.
- Confirm its settlement ID does not change after reload.
- Confirm no NPC identity/count changes from this migration.

These tests can be deferred during iterative development,
but a deferred test is not marked as passed.
