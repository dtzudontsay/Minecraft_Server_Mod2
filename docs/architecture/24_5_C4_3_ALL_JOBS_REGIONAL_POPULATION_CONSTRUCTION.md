# C4.3 — All-Jobs Population, Workplace and Full-Demand Pilot Construction

Scenario: ASOIAF, 298 AC. **Technical staging package, not finished global implementation.**
Source of truth: user `Jobs.xlsx` (206 occupied occupation rows, 204 unique occupation identities after resolving two exact repeats). The original spreadsheet annotations and row positions are preserved in `Jobs_C4_3_Buildings_and_Locations.xlsx`.

## Core observations

- C1 has 248 physical site presets; 198 have nonzero estimated populations and 50 have no modeled permanent population. Five additional atlas marker observations refer to already deduplicated physical sites. The political holding catalogue contains 259 entries, not 259 physical towns.
- **C1 estimates are not demographic censuses.** The existing C1 workbook model contains 21 detail worker roles and seven sector percentages, mostly MOD_ESTIMATE. No global B4 per-person employment roster exists.
- The new C4.3 planner preserves every site's original C1 planned workforce and seven-sector worker count, while distributing workers among 204 occupation IDs according to role rarity, eligibility and place-specific policy. It never manufactures additional residents.
- Existing published slavery geography, institutional/cultural restrictions, exceptional positions and criminal activities require explicit exceptions. Slavers are never automatically assigned in Westeros or Braavos; unique scenario jobs never appear as random one-per-village employment.
- 18 additional physical building designs are proposed, and room-level workspaces such as keep offices can use stable parent building IDs with local coordinates. **The generic new templates are not hand-detailed culturally unique buildings.**
- Initial construction and future growth need to use the same occupancy/workplace demand engine; a worldgen preview does not constitute a persistent construction transaction.

## What's physically implemented by these files

An **opt-in**, full-demand regenerated chunk-index JSON can replace the first-phase two-site `settlement_construction_c4_v1.json` for *Ramsgate and Acorn Hall only*; it retains the canonical `pilotCount == 2`, C2 pilot marker IDs and Winterfell exclusion. Unlike C4.1, it plans ALL baseline residential buildings instead of fixed caps of 48 and 180, intersperses shops among streets, incorporates existing C3 civic templates and the additional safe generic buildings, and expands the farm belt to 24 plot tiles of 31×31 blocks **per farmhouse**. Road/building overlap is statically rejected.

Actual Minecraft terrain suitability is still evaluated at runtime; unsafe plots are skipped, sometimes leaving housing/fields missing. C4.2 entrance grade remains a prerequisite. The pilot may generate thousands of chunks and be expensive. No new existing-world chunks or saves are edited; changing the JSON will not retrofit a previously visited town.

The **existing Winterfell V3** 5,000 residents, 1,136 households and bespoke walls/keep remain authoritative and untouched. A separate report script can propose reclassifications for generated Winterfell residents, preserving all NPC IDs, named characters, household references and legacy work positions. It does not apply these proposals to the runtime yet.

## What is *not* complete

1. An actual B4 persistent individual roster and job-to-workplace occupancy manager across all 248 sites.
2. Rich distinct interiors or coordinate-resolved workroom paths within keeps, for dynamic NPC routines.
3. All 18 new templates being tested in Minecraft across every regional palette and environmental condition.
4. Shore-dependent docks and shipyards, mine geology surveys, specialist fortifications, special cities (e.g. Braavos and Meereen), and mobile Dothraki communities.
5. Staffing every building; productive WORK actions, actual farms/yield capacities, inventories, property taxes or changes through simulated lifecycles.
6. Persistent, resource-consuming construction/demolition in already-generated chunks.
7. Full worldwide generic layout generation and MAJOR architectural streaming. Global activation is intentionally not enabled.

## Local execution (repo root; backups recommended)

**Review-only, do first:**

```powershell
py tools/geodata/build_c4_3_job_distribution.py
py tools/geodata/extend_c4_3_architecture.py
py tools/geodata/build_c4_3_winterfell_job_overlay.py
py tools/geodata/test_c4_3_job_distribution.py
```

Outputs appear in `tools/geodata/output/c4_3_jobs/`: all-job population distribution for **all 248 sites**, `ALL_JOBS_AT_ALL_LOCATIONS.csv`, `BUILDING_CONSTRUCTION_DEMAND.csv`, resource blueprint JSON and optional preserved-roster Winterfell occupation overlay. **These output files contain proposals, not already employed NPCs.**

**To opt into a NEW WORLD stress test for ONLY Ramsgate and Acorn Hall:**

```powershell
py tools/geodata/extend_c4_3_architecture.py --apply-resource
py tools/geodata/patch_c4_3_java_guards.py
py tools/geodata/build_c4_3_full_pilot_stress.py
# Inspect tools/geodata/output/c4_3_full_pilots/C4_3_FULL_PILOT_REPORT.json
py tools/geodata/build_c4_3_full_pilot_stress.py --install-resource
.\gradlew.bat build
```

Do not run older C3/C4 builders immediately afterwards: they regenerate the previous 43-template / 48-180-home resources. Consider integrating C4.3 as the standard builder only after the new workflow passes tests.

The tools make `.bak` / `.json` backups when patching. Java patches are narrowly guarded to refuse unrecognized source versions. Do not use a pre-existing Minecraft save for the stress test; use a fresh world or genuinely ungenerated chunks, and preserve the old `level` directory.

## Acceptance gate before activating more than two sites

- All 248 site workforce totals conserved; all role/macro totals conserved; no duplicate Salt Worker/Slaver employees from repeated source rows.
- Name/location/provenance of all 253 map marker observations and 259 political holdings unaltered.
- No ghost NPCs: 5,000 Winterfell roster identities preserved, each existing household assigned once; new estimated populations are not automatically instantiated.
- Spatial integrity: no road/plot collision, all required worker sectors represented, functioning doors and stairs, shore/mining plots either approved or deferred.
- In-game test on both pilots in a fresh save, plus terrain tests in steep/snowy/coastal contexts.
- Confirm field annexes survive terrain surveys and are large enough to illustrate realistic farmland; reconnect with authoritative agricultural land area rather than using decorative parcels as acreage.
- Save/reload and chunk regeneration consistency with the existing C4.2 Java pipeline.
- Performance benchmark of the significantly enlarged chunk indices and physical lots before global expansion.

## References and provenance

- `docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md`
- `docs/architecture/23_1_CONNECTED_WORLD_IMPLEMENTATION_PLAN.md`
- `docs/architecture/24_C1_SETTLEMENT_PRESETS_AND_CONSTRUCTION_BOUNDARIES.md`
- `docs/architecture/24_3_C4_CONNECTED_SETTLEMENT_LAYOUTS.md`
- `docs/architecture/24_4_C4_2_TERRAIN_STREETS.md`
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/settlement_presets_c1_v1.json`
- https://awoiaf.westeros.org/index.php/Slaves
- https://awoiaf.westeros.org/index.php/Dosh_khaleen
- https://awoiaf.westeros.org/index.php/Bloodriders

All noncanon allocations, building types, capacities and fields in this batch are MOD_ESTIMATE. The workbook is for review; it does not silently change authored canonical character profiles or assign a profession simply because a town has an eligible job slot.
