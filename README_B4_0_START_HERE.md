# B4.0 — Worldwide initial population records and identity audit

This batch creates a **complete, deterministic first-pass people/household/employment baseline** from already-installed C4.9/C4.8/C4.4 sources. It does **not** pretend that these people already have active memories, conversations, production or physical entities. This is the necessary sharded identity layer on which the upcoming behavioral/LLM runtime can safely operate.

## Install

1. Extract this ZIP **into `D:\GameOfThronesMod2` (repository root)**, preserving paths.
2. Ensure you have already run the C4.9 generator **without `--summary-only`**; its household reservations must exist in `tools/geodata/output/c4_9_settlement_ledgers`.
3. Run `RUN_B4_0_GENERATE_WORLD_POPULATION.bat`. It runs the regression tests, creates one compressed population resource per inhabited site, validates headcount, installs complete resources and patches the existing `KnownWorld.java` with an idempotent command registration (with a `.before_b4_0.bak` file).
4. Run IntelliJ Gradle `clean`, `build` and then `runClient`.
5. In your **Known World** world, enter `/kwpopulation status` and `/kwpopulation site site_ne_001` to inspect the packaged worldwide first-generation records. You can inspect any site ID. Zero-population locations display their counts rather than fake residents.

To inspect the generator offline without publishing resources, run `py -3 tools\geodata\b4_0_world_population.py` without `--install`. To validate a single region in a small fixture, use `--site site_ne_001`; it deliberately cannot be installed into the game as a fake full census. The output folder is `tools/geodata/output/b4_0_world_population` and the installed resource is `src/main/resources/data/knownworld/scenarios/agot_298_ac/world_population_b4_v1`.

## Guarantees and boundaries

- Worldwide census: **248 site entries, 198 with people**, preserving exactly **3,760,684 people**, **799,343 households**, **1,880,341 modeled workers**. Each non-Winterfell generated resident gets a stable, deterministic `personId`; each worker receives exactly one of the source 204 occupations. 204 columns are validated site-by-site.
- Existing **Winterfell 5,000 residents / 1,136 households** are copied using their original case-study IDs, names, household and home IDs. Do not re-import them into `NpcRegistry`: Winterfell's V3 runtime already owns them. Its original jobs remain authoritative; the C4.8 2,500 modeled-worker figure is a target and is **not** forced onto authored Winterfell residents.
- Other sites retain existing household reservation IDs and C4.10 lot IDs. Multiple households can share an urban building; 204-job occupation-to-workplace assignment follows C4.4 building mappings and original C4.9 staffing limits. Missing/unsupported physical structures never cause deletion of an employee; missing bindings are counted under `unboundWorkplaceWorkers` in the site manifest.
- Site-by-site culture/religion/language influences come from B2/B3 region links and the existing region profile files. The included 21 regional name pools are **first-pass mod estimates**, not definitive source canon. People deviate from their household's main culture; religious variation uses the actual weighted profiles. Unknown contexts retain explicit `unknown_*` identifiers instead of creating false certainty.
- Generated members have plausible age structure and parental links within households. The dataset is a **starting model, not a fully historical/genealogical truth**. Couple and household models must be reconciled with existing genealogy and marriage authority before simulating births, inheritance or importing their parents into the live `GenealogyManager`.
- **No extra world save edits, no GitHub writes, no forced chunk loading, no 3.7 million Villager instances.** Shards are compressed initial base data in the mod resource; the eventual world-save identity delta/event overlay is part of B4.1. C4.11 terrain/physical construction remains separate.
- **Existing named characters outside Winterfell need an explicit ID-reconciliation migration** before these 3.76m people become the authoritative `NpcRegistry`. Do not add them wholesale to the existing cohort census: that would double-count. These baseline records intentionally have `identityProvenance: MOD_GENERATED_298AC_PROVISIONAL` and the manifest says `npcRegistryIntegrated: false`.

## Future batches to reach Winterfell parity worldwide

**B4.1** (critical): world-save canonical registry of compact identities, named-character reconciliation, bounded active-NPC projection for all settlements (max ~128 nearby), right-click inspection and generic K-key LLM conversation adapter with safe individual histories. Reuse A1-A5 / C4 Winterfell logic; never create a second mutable truth.

**B4.2**: daily scheduler applied to everyone using batched per-site time buckets; valid occupation/worksite productivity, skill learning, household consumption and offscreen event/experience summaries. Jobs in the current `NpcActionProcessor.processWork` are intentionally empty and must be connected before declaring real productivity.

**B4.3**: travel itineraries, real route clocks and interception, spatial event-witness validation for active and abstract people, rumor delivery, family event notifications, rulers/succession/loyalty, coalesced memories with no infinite conversation/experience files.

**B4.4+**: culture-sensitive workplace/trade/war/pathfinding/behavior refinement, partial deserialization, shard compaction and crash-safe migrations. Every generated baseline person must remain recoverable by stable ID.

## Validation still required

The eight Python tests use miniature 248-site structured fixtures and check exact jobs/households, deterministic names, age-gated employment, staffing IDs and installer idempotence. The full 3.76m population generation, exact B2 regional linking and full Fabric build **have not been run in this environment**; your local BAT is the first full integration test. Report any emitted `unboundWorkplaceWorkers` entries, missing regional profiles or mismatched named-character records in the resulting manifest instead of silently accepting these as completed gameplay.
