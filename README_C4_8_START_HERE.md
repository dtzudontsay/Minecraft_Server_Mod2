# C4.8 — Worldwide census reference and atlas tier labels

**Scope:** Reference exports + read-only atlas UI labels. **Not** a worldwide construction/NPC activation batch.

This batch reuses the actual C1 settlement preset and C4.4 occupation data already installed. It does not recalculate or change any population figure, job allocation, plot, game save, biome or chunk. The 248 × 204 source matrix is included in the ZIP for immediate reference; the script joins it with the exact current C1 site/anchor records to produce updated CSV and JSON files locally.

## Run

1. Back up your mod directory as usual. Extract this ZIP directly over `D:\GameOfThronesMod2` (alongside `gradlew.bat`).
2. Execute **only** `RUN_C4_8_CENSUS_AND_ATLAS_KEEP_OPEN.bat` from the repo root. No older installers are required.
3. It validates 248 physical sites / 253 markers / 204 unique jobs / 3,760,684 people / 1,880,341 workers / all per-site conservation constraints. It creates CSV+JSON references in `docs/reference/c4_8_worldwide`.
4. It adds a small read-only atlas resource for the 253 markers and patches the existing atlas tooltip to show `[SMALL]`, `[MEDIUM]`, `[MAJOR]`, `[SPECIAL]` or `[RUIN]` next to a marker's name.
5. It runs **`gradlew.bat build`**, retaining full Gradle output in `C4_8_SITE_REFERENCE_AND_ATLAS_BUILD.log`. If building fails, the Java atlas edits are restored; exported CSV/JSON data remain available.
6. Only after **BUILD SUCCESSFUL** start `gradlew.bat runClient` and open your existing Known World Atlas. Hover a red marker to see its classification. This is an atlas label only; no generated buildings/NPCs are affected.

If you want **only the data** without changing or compiling Java, run:

```
py -3 tools\geodata\c4_8_worldwide_reference.py
```

This standalone command generates the CSV/JSON reference exports and the optional atlas tag resource, but does **not** activate the GUI display or modify Java source.

## Newly generated reference files

- `docs/reference/c4_8_worldwide/C4_8_SITE_POPULATION_HOUSING_248.csv`: every site, tier, original/merged markers, X/Z, original C1 people/workers and **estimated** households and housing demand.
- `docs/reference/c4_8_worldwide/C4_8_ALL_248_SITES_204_JOBS.csv`: same site IDs, exact C4.4 workers per job, **including all zeros**.
- `docs/reference/c4_8_worldwide/C4_8_WORKPLACE_DEMAND_248.csv`: shared venue types, planned worker counts, requested buildings and capacity ceilings. **Demand, not constructed buildings.**
- `docs/reference/c4_8_worldwide/C4_8_WORLDWIDE_SITE_REGISTRY.json`: compact machine-readable site IDs, tier, X/Z, all 204 jobs and building demands, aligned to later housing/worldgen/NPC phases.
- `docs/reference/c4_8_worldwide/C4_8_MAJOR_SPECIAL_REVIEW.csv`: exception-planning list, **not** a runtime build-failure list.
- `docs/reference/c4_8_worldwide/C4_8_AUDIT_SUMMARY.json`: exact source hashes and conservation checks.
- `docs/reference/c4_8_worldwide/C4_8_README.md`: data provenance and limitations.
- `src/main/resources/assets/knownworld/maps/atlas_site_tiers_c48.json`: map marker classifications for the atlas UI.

## Understanding the status

There are **253** original atlas marker observations but only **248** distinct physical sites because some markers refer to the same site. Do not double-count them. The 248 physical sites comprise **41 Small, 95 Medium, 47 Major and 65 Special**; 198 have positive model populations; 50 have none.

The **3,760,684** people and **1,880,341** workers are **sums of all individual site plans**. These are estimates in a mod scenario, not canon census records, and not runtime NPCs. C4.4 has 206 workbook rows, including 2 repeated job names and **204 unique profession IDs**; many rare jobs intentionally have zero default workers in specific regions. Seven regional macro-economic profiles were adjusted as provisional corrections. Existing named characters or special organizational roles need a future reconciliation against this model, to avoid duplicates.

A C1 Major settlement plan uses roughly 4.7 people per household and **four household units per residential building**. Thus Winterfell's generic C1 model requests **266 residential buildings**, but its dedicated V3 plan binds 5,000 people to **1,136 occupied homes**. These are different density models, not contradictory headcounts; final global household generation should explicitly settle on the housing units and households before construction.

## Design contract for later worldwide construction (not implemented by this package)

1. Preserve the accepted marker coordinate as the **canonical map pin**, even if a buildable town center shifts to the closest sufficient dry land footprint. Persist the X/Z shift and reason; prioritize distance, coastline orientation and area/slope suitability, not the very nearest one-block beach pixel.
2. Prefer terracing and switchback access in steep mountains, keeping structures within approved graded lots; never flatten a whole mountain indiscriminately. Log impossible terrain and excessive shift/slope in an explicit failure list with site name, marker IDs and reason.
3. Generate deterministic **household plans and workplace slot IDs** from the immutable per-site population and profession plan before construction. This is not the same as creating millions of individual NPC state machines before physical generation.
4. Build districts from the same plan with chunk-indexed streaming and physical build acknowledgements. **Do not count skipped plots as usable capacity.** Assign actual NPC identities and workplaces later by resolving preserved household/workplace IDs; store backlog if capacity is insufficient.
5. Small and Medium generic geometry is already supported on eligible sites; **Major streaming and exceptional site layouts are not fully implemented**. Major source types may require an authored fortress/seat, but do not generically place a Winterfell castle in all cities.
6. Perform per-site reconciliations for population, households, occupied units, actual workplace capacity, reserved rights/ownership, unemployed workers and exceptional visitors/garrisons. Do not silently fabricate or delete residents to match a building count.

## First rollout target

After this read-only atlas/data batch, the next real generation batch should implement dry-land relocation + mountain-safe surveying and scale the generic district planner through the 38 Major sites that are currently `GENERAL_COMPACT_SETTLEMENT`. The other nine Major sites need coastal, canal, pyramid, fortress or other authored geometry where appropriate. Report every skipped/failed placement; do not claim universal construction until the generated world's building-capacity audits pass.

**Installer safety note:** New helper Java source is kept as a `.txt` template in `tools/geodata`, not copied to active `src/main/java` until all data and compatibility checks pass. No Java class is created merely by extracting the ZIP. If the subsequent build fails, the installer restores the prior atlas source and removes any newly created helper class. It does not overwrite an unexpected preexisting helper.
