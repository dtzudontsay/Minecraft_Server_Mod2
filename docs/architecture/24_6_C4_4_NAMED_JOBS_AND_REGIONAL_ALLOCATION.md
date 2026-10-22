# C4.4 — Corrected named workplaces and regional occupation redistribution

**Status:** Data/authoring handoff; no activation of NPCs, world generation, new structures or persistent job assignments.

This package corrects the user-submitted `Jobs.xlsx` occupation worksheet. All **206 original rows**, including annotations and repeated job names, remain in the workbook. The third column (C) names a concrete workplace in every occupied job row, not a `NEW BUILDING REQUIRED` placeholder. For castle jobs, the field names a specific interior or relative workspace such as a keep's administrative office, maester's study, or great hall. Criminal activities do not automatically demand public civic structures, but their plausible spaces are named.

## Spreadsheet worksheets

1. `Tabelle1` (original sheet): A and B preserve original categories and job strings; **C** is the actual named building/workplace; D enumerates sites with **positive planned** counts; E gives shared building ID; F/G give location and worker totals; H-K document rarity, policy, template state and cautions.
2. `248 Settlement Totals`: 248 sites, original/revised regional models, population, labor and seven sectors; **column P** verifies zero workforce discrepancy.
3. `248 Site Job Matrix`: all 248 sites × 204 unique jobs, **including zeros**. Locate a settlement in column B and read horizontally to see its exact planned worker counts. No missing professions or sites.
4. `READ ME - model`: explanations, special cases and source URLs.
5. For filterable long-form 24,000+ positive site/job entries, use the companion `C4_4_ALL_NONZERO_JOB_COUNTS.csv` within the ZIP. For all site-by-building planned counts and staffing caps, use the `buildingDemand` section of the companion JSON resource.

## Source counts and caution

- 253 map observations -> 248 distinct physical sites; 259 political holdings are separately modeled, some without a physical seat; do **not** add the observations and political domains to physical populations.
- The current C1 planning population of the 248 physical sites totals **3,760,684**, of whom **1,880,341** are provisionally counted as workers. These are *game scenario assumptions*, not canon population figures, not a census, and not runtime-saved individuals.
- 204 distinct job IDs are defined among the 206 source rows; the repeated `Salt Worker` and `Slaver` rows remain visible but do not duplicate employment.
- Some of the 204 distinct labels have no **default** allocations because they are scenario-authoring-only, hidden/exceptional or become zero after rarity/size filters; their workplaces are still named in column C.
- Existing C1 population estimates and labor planning totals are preserved. Provisional rarity caps prevent villages or large capitals from receiving multiple castellans or hundreds of executioners. The seven C1/B2.3 macro-sector splits are generally maintained. Seven *explicit* regional model corrections are introduced: Pyke (`SW-008`), Driftmark (`SW-035`), Sisterton (`NW-038`), Port of Ibben (`NE-002`) to island/port models; White Harbor (`NW-034`) and King's Landing (`SW-046`) to major-port models; Lhazosh (`SE-012`) to rural-south (rather than steppe) economic mix.
- Those regional corrections are invented/proposed authoring modifications and should be revisited before worldwide activation.
- Original C1 21-role worker targets were reconstructed using the exact corresponding B2.3 sector percentages and the repository's described largest-remainder rounding method; the *entire* 7.7 MB source C1 JSON was not available as a local input. Reconcile against the complete local C1 JSON before making this resource authoritative. Any differences should be small rounding differences, except the seven documented rebalances.

## No silent job/structure creation

- A zero planned number of `Spy`, `Ruler`, `Courtier`, `Dosh Khaleen`, secret arcane roles, `Unsullied Soldier` etc. does **not** remove those professions. They are designed as event- or organization-authorized identities, secondary roles, or historically authored profiles rather than random municipal occupations.
- Slaver professions are restricted to reviewed Essosi commercial contexts. In particular Braavos/Westeros are excluded from random slaver assignments.
- Specialized religious officials are locally constrained. The generic religious attendant workspace must resolve through the site's actual faith, e.g., sept, godswood, or temple, not a universal Faith of the Seven building.
- Shore workplaces, bridges, mines, canals, legal arrangements, exceptional fortresses, and Dothraki khalasars require their own land/institutional surveys; a positive job count is **not permission to build**.
- Buildings by job are a demand contract: `workplacesRequested = ceil(plannedWorkers / staffCapacity)`. A real building must be built, owned, saved, staffed, and made accessible before being counted as an occupied workplace.
- For castle rooms, store `buildingId + roomSlotId`, not authored global XYZ for each keep. Instance-relative room layouts and physical navigation still need implementation.

## Output artifacts

- `Jobs_C4_4_Named_Buildings_and_All_Settlement_Distributions.xlsx` — main workbook.
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/occupation_building_catalog_c4_4_v2.json` — names, IDs, rarity, venue plan for every original occupation row.
- `src/main/resources/data/knownworld/scenarios/agot_298_ac/settlement_job_distributions_c4_4_v2.json` — all per-site sector, profession and construction-demand estimates.
- `tools/geodata/output/c4_4_jobs/C4_4_ALL_248_SITES_JOB_COUNTS.csv` — dense 248 × 204 occupation matrix, **zero entries included**.
- `tools/geodata/output/c4_4_jobs/C4_4_ALL_NONZERO_JOB_COUNTS.csv` — long-format positive counts with named workplaces.

**None of these files is wired into the current runtime.** Do not assume simply extracting this package changes the generated towns; the C4.3 stress test and existing authoritative NPC records are unaffected. GitHub is read-only from the assistant side, and only the user may commit these files.

## Relevant original repository authorities

- [`tools/geodata/build_b2_3_world_profiles.py`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/tools/geodata/build_b2_3_world_profiles.py)
- [`tools/geodata/build_c1_settlement_presets.py`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/tools/geodata/build_c1_settlement_presets.py)
- [`docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/docs/architecture/KNOWN_WORLD_MASTER_ROADMAP.md)
- [`docs/architecture/23_1_CONNECTED_WORLD_IMPLEMENTATION_PLAN.md`](https://github.com/dtzudontsay/Minecraft_Server_Mod2/blob/master/docs/architecture/23_1_CONNECTED_WORLD_IMPLEMENTATION_PLAN.md)
- `docs/architecture/24_5_C4_3_ALL_JOBS_REGIONAL_POPULATION_CONSTRUCTION.md` (from the prior installer package)
