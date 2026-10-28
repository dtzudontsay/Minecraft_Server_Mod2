# C4.9 — Uncapped worldwide settlement-demand reservations and terrain-anchor preflight

**Scenario:** 298 AC. **Status:** new files delivered for user installation; local Python source tests passed; not a Minecraft/worldgen rollout. **GitHub:** assistant read-only, user installs/pushes. This does NOT supersede the project master roadmap.

## What is authoritative

* Physical site IDs, markers, X/Z anchors, original tiers, population and residential demand: existing C4.8 CSV (derived from C1).
* Existing 204 occupation counts and per-building requested workplaces: C4.8 (derived from C4.4).
* Winterfell's 5,000 people, 1,136 household IDs and 1,136 residential buildings: existing Winterfell V2/V3 case study; NEVER regenerate their identity records.
* 259 political holdings are **not** 259 independent physical populations; use the existing 248 unique physical site IDs.
* Land, height and relief: the established authored geography and world generator. The new policy is a *candidate*, not terrain approval.
* **These numbers are scenario modeling estimates, not historical canon headcounts.** Never label the forecasts as actual people or physically built structures.

## Frozen baseline audit, 2026-10-22

* 248 sites, 253 marker observations, 198 inhabited sites, 50 zero-population sites.
* 3,760,684 planned residents, 1,880,341 workers, 204 distinct occupations.
* C4.8 estimated 799,271 household units, 221,541 residential buildings and 150,963 workplaces. Replacing only Winterfell's generic 1,064 households and 266 residential buildings with its authored 1,136/1,136 yields 799,343 household units and 222,411 residential buildings (planning arithmetic only).
* 116 source workplace demand rows of type `KEEP` contain 661 modeled workers but *zero* separate `KEEP` workplace buildings. They must be assigned to the central-complex capacity, not discarded and not duplicated in separately generated generic keeps.
* Example: King's Landing 500,000 people, 250,000 workers, 106,383 households, 26,596 residential buildings (4 units per building), 19,475 workplace buildings, and 1,416 SMITHY workplaces for 8,492 smithy workers. The occupation mix may need demographic review, but construction must not secretly cap it.
* 47 Major sites: 38 existing `GENERAL_COMPACT_SETTLEMENT`; 9 specialized (White Harbor, Braavos, Meereen, Dragonstone, Casterly Rock, Oldtown, Selhorys, Valysar, Volon Therys). The 9 are not failed builds.

## Three shared central-place archetypes

| Tier | Core requirement | Walls and streets | Capacity |
|---|---|---|---|
| SMALL | One manor/civic hall | palisade and gates, homes nearby and outside | uncapped by tier |
| MEDIUM | One minor keep | stone wall, gate, connected core and outer wards | uncapped by tier |
| MAJOR | One main castle complex | inner/outer walls, castle work rooms and housing/workplace rings | uncapped by tier |
| SPECIAL | one site-policy-specific core when inhabited and fixed | special dry/wet/monumental/ruin/moving rules | never implicitly converted into generic castle |

Existing C3 template `KEEP` is a 49×41, 3-floor generic structure; `GREAT_HALL` and `ADMINISTRATION` are also available. The MAJOR `MAIN_CASTLE`, SMALL `MANOR_HALL` and complete curtain-wall archetypes are **logical required core complexes**, not falsely claimed finished C3 renderers. Winterfell keeps its authored V3 keep/walls and existing identities; its geometry is the model for a new reusable major layout, not subject to demolition. A central castle counts exactly once. Its staff slots consume the existing site job budget, never mint new jobs.

## Household forecasts versus actual families

This batch conserves each site's original C1/C4.4 population/worker counts. One household reservation is created for each estimated household outside Winterfell; sizes vary deterministically from 1 to 10, sum **exactly** to the planned population and are stable on rerun. These are *not* yet genealogical family memberships, owners, births, deaths, ages, or named NPCs. Distinguish `householdReservationId` from existing `NpcId`, from future authoritative HouseholdId and from House/Dynasty IDs. Before enabling NPC generation, B4 must bind/migrate these reservations to existing authoritative identity managers, preserve named characters, and ensure individual + cohort count = settlement population.

Winterfell additionally retains its existing C4.6 inventory of 1,913 planned building plots: 1,136 residential plus 777 other lots. New household **and** workplace reservation files exclude Winterfell entirely; its authored physical lots require reconciliation against the C4.8 demand before the next worldgen batch, not recreation.

No housing counts are capped to pilot numbers. A housing **unit** can share a physical building (urban 4-unit major and 2-unit medium estimates). When future NPC simulation changes population or labor, the ledger is a baseline for *diff-based* demand, not a permanent immutable population. Counts and records must be versioned before any real saved world activation.

## Workers, workplace slots and ownership

Each occupied workplace type has `workersTarget`, exact number of buildings requested, and physical staff capacity. A second reservation ledger emits stable workplace IDs and worker-slot counts. Reserved worker slots are not NPC identities. One institution may contain multiple logical work functions. All 116 `KEEP` categories with 661 workers are accounted for by `mainComplex.reservedKeepWorkerSlots` rather than inventing extra KEEP buildings.

In the NPC integration batch, derive actual occupation allocations from **the same 204-job distribution**, assign specific `NpcId` to a single primary job and a single residence, then attach workers to the available building's reserved positions. Record owners separately (households, persons, organizations, titles, state or communal institutions) with legal authority. Unemployment, fractional labor, job changes, vacancies and special roles must be explicit. Staff/job shortages must report backlog; never conjure workers to fill a building.

## Coast/mountain rule (coded policy, not yet connected to active Minecraft worldgen)

`KnownWorldC49TerrainPolicy.find(...)` is a pure Java site-placement policy accepting the existing world land + authored terrain height samplers. It:

1. Tries the original map X/Z first, then scans candidate build anchors outward, preferring the closest **sufficiently large connected dry** core and street reserve, not the nearest single dry block. Original map anchor is never overwritten.
2. Computes an inland expansion direction from the connected land survey. A shore anchor is provisional until all actual lots, docks, roads and walls pass surveys.
3. Allows bounded local relief and classifies terrace, retaining-wall and switchback-road requirements; it does *not* flatten authored mountains. Steep central-keep candidates are rejected in favor of another nearby site.
4. If candidates fail, returns site ID, name, all marker IDs, original coordinates, attempted coordinates, tier, population, deferred home/job counts and a reason. That record must later be persisted in a per-site failure register and returned by the construction command rather than thrown away.
5. Never reduces `homes` or `workplaces` as a workaround. The anchor search radius is a **location-search bound**, not a cap on city footprint. Further districts must grow through indexed, spatially paged worldgen as required.

**Important implementation gate:** The policy class is not automatically invoked by the current C2 generator. The current `KnownWorldUniversalWorldgenC2` loads a two-pilot manifest and decorates existing approved lots. It requires a future worldgen integration: one-time preflight, persistence of selected anchor, recomputed lot/chunk indices, actual terrace/retaining-wall/switchback renderers and safe new-world activation. It would be inaccurate to claim the present batch already relocates real settlements or builds the walls.

## Files / run

* `tools/geodata/c4_9_settlement_ledgers.py` reads the **existing** C4.8 repository CSVs, never writes C4.8 sources.
* `src/main/java/.../KnownWorldC49TerrainPolicy.java` adds the pure preflight policy; unused until explicitly integrated.
* Tests under `src/test/python` and `src/test/java`.
* `RUN_C4_9_PLAN_READONLY.bat` runs the script and validations on Windows.
* Generated outputs in `tools/geodata/output/c4_9_settlement_ledgers/`: `C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json`, `C4_9_HOUSEHOLD_RESERVATIONS.jsonl` (excluding existing Winterfell IDs), `C4_9_WORKPLACE_RESERVATIONS.jsonl` (excluding existing Winterfell workplot IDs), `C4_9_TEMPLATE_POLICY_EXCEPTIONS.json`, `C4_9_SUMMARY.json`.

Run in repository root:

```bat
py -3 tools\geodata\c4_9_settlement_ledgers.py
py -3 -m unittest discover -s src/test/python -p test_c4_9_settlement_ledgers.py -v
gradlew.bat test
```

The existing source job matrix and per-workplace demands must not be replaced by these data exports. Large per-household JSONL and workplace JSONL should be loaded through pagination or indexed persistence, not eagerly into the active Minecraft chunk decoration thread. **No need to create millions of active Minecraft entities.**

## Roadmap from three examples to global rollout (new construction sub-batches)

1. **C4.9 (this package):** freeze population/household/workplace/core demand by site; no caps, authoritative Winterfell overrides, coast/mountain retry policy, failure record schema; no new Minecraft world blocks yet.
2. **C4.10 — physical universalization:** reusable Small/Medium/Major center and walls, switchbacks/terraces, indexed unbounded district paging, actual C2/C3 bridge, Winterfell V3 adapter preserving existing identifiers; render each new chunk only once. Add save-safe plot ID/lot state and automatic failure register. Enable only on fresh dedicated test worlds.
3. **C4.11 — broad physical activation:** preflight all 248 by type; activate eligible ordinary inhabited sites without extra manual per-site approval, handle exceptional port/canal/rock/monumental layouts, report unresolved anchors and every unmet house/job demand. Visual test coastal, mountain, huge Major, small and medium, across multiple chunks. Keep `SPECIAL` moving/ruined locations out of inappropriate city generation.
4. **B4/D integration:** authoritative household/person ledger and cohort materialization, jobs and ownership with existing IDs, population conservation and save/load; populate 198 inhabited sites with offscreen simulation, no phantom people. Only after that connect dynamic C6 construction triggered by births, migration, economic role changes and resources.

The four steps are an implementation sequence, not a promise that all can be safely accomplished in one batch. Physical construction can be batch-activated once preflight passes, but populating 3.76M people in a live simulation requires regional/cohort scalability, benchmarking and reproducibility.

## Remaining acceptance gates

- Java local 26.3 Gradle compile/test (the provided test class uses existing JUnit 5 project infrastructure).
- Full 248-site ledger generation from actual checked-in C4.8 CSVs on user's machine and matching invariant counts.
- Authoritative Winterfell ID preservation; never duplicate 1,136 authored household IDs.
- Coordinated placement of exact requested homes/workplaces, not just center-core approval.
- Worker-to-occupation and worker-to-building mapping, owners, and nonresident/nonnuclear housing handling.
- Fresh-world multi-chunk visual and performance benchmarks, failed-plot reporting and no overwrites of player structures.
- No assumptions of lore-specific actual geometry until special blueprints have been approved.
