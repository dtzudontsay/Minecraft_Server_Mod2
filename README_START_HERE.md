# Known World C4.3 — All 206 User Jobs, 248-Site Planning, Two-Pilot Stress Preview

**State:** Implemented *planning/data bridge*, provisional generic C3 architecture extensions, and an *opt-in* two-pilot fresh-world construction stress test. NOT a completed worldwide/NPC-lifecycle integration. GitHub remains read-only from the assistant; only you may apply these local files and push.

## Install into local project

1. Back up your repository branch and any Minecraft saves.
2. Extract this ZIP into the **root** of `Minecraft_Server_Mod2`, keeping the directory structure.
3. The workbook `Jobs_C4_3_Buildings_and_Locations.xlsx` is also included. It preserves every original job line and your annotations. Column C is the building, D the eligible places, E the count; further columns show rarity/policy/new-template status. The workbook itself does not alter game data.
4. Execute `RUN_C4_3_REVIEW.bat` to generate full 248-site per-job demand CSVs and a Winterfell ID-preserving occupation overlay without changing active source resources.
5. Open generated `tools/geodata/output/c4_3_jobs/ALL_JOBS_AT_ALL_LOCATIONS.csv` and `BUILDING_CONSTRUCTION_DEMAND.csv`. Those are estimated *jobs and workplace demand*; there is not yet an active B4 world population employment roster.
6. If you want to test dense villages in a **fresh new Minecraft world**, run `RUN_C4_3_NEW_WORLD_STRESS_INSTALL.bat` and type `STRESS`; it stages generic templates, applies tightly matched C3/C2 Java guard changes, creates uncapped original 2-pilot C4 data and runs `gradlew.bat build`. Existing chunks, other sites and Winterfell V3 remain untouched.

## Four distinct deliverables in the ZIP

- **Spreadsheet** — 206 rows retained; map to 48 suggested workplace/location concepts (including nonphysical/keep interior), 198 inhabited site names eligible. Presence in a list is not proof a worker already exists at that site.
- **Job catalog and model** — deterministic reallocation of **all 248 physical site presets** among the user jobs, conserving each site's old C1 worker total and seven sector subtotals. Reserves non-existing/exceptional roles for scenario authoring and excludes repeat Salt Worker / Slaver source rows from double employment. Output is proposed plan only.
- **Building programmes** — 18 generic extra C3 template proposals; Java manifest count and footprint guards updated only with explicit opt-in. Still need design review for bespoke cultural interiors and Castle workroom coordinates.
- **Full-demand stress pilots** — Ramsgate/Acorn Hall home caps removed and farm belts enlarged to **24 fields of 31×31 per farmhouse**. Shops spread throughout a connected grid and separate rural workplaces join the farm belt. Special, aquatic, geology-gated and policy-gated construction remains explicitly blocked.

## Do not misread this package

- Does **not** turn population estimates into persistent individuals at every site.
- Does **not** change the current NPC routine system or original 21 named character profiles. The Winterfell overlay is proposed, not imported by the runtime.
- Does **not** lift the two-pilot Java safety restriction or enable worldwide settlement generation. MAJOR and special region architecture are still future integration work.
- Does **not** add live construction, automatic growth, economically productive jobs, castle room navigation or dynamic worker assignment; the planning contract specifies the required integration points.
- Does **not** guarantee every field is terrain-safe. C2 may skip lots after geographic checks. Enlarged layouts may generate thousands of chunks and require CPU/memory profiling.

## Paths and order

Required new files go to their ZIP-contained relative locations. C3 and C4 baseline resource files remain unchanged until the explicit stress-install command. The two Java source files are patched by `patch_c4_3_java_guards.py` with exact-match assertions; `.bak` backups avoid duplicate Java compilation. To undo, use git restore for modified tracked resources and Java files, after saving any wanted test outputs.

Full roadmap handoff: `docs/architecture/24_5_C4_3_ALL_JOBS_REGIONAL_POPULATION_CONSTRUCTION.md`.
