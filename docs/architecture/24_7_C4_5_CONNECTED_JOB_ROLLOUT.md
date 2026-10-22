# 24.7 — C4.5 Connected Job Distribution, Winterfell NPCs and Generic Construction

## Relationship to the authoritative roadmap

Follows `KNOWN_WORLD_MASTER_ROADMAP.md`, `23_0_CONNECTED_SIMULATION_ROADMAP.md` and `23_1_CONNECTED_WORLD_IMPLEMENTATION_PLAN.md`. C4.5 is a *physical-worldgen/data/NPC import pilot*, not final B4 identity creation, D jobs/lifecycle, F institutions or dynamic C5 construction.

## Population and job authority

`settlement_job_distributions_c4_4_v2.json` contains **248 site plans**, 198 nonzero population; `occupation_building_catalog_c4_4_v2.json` preserves **206 rows / 204 unique jobs**. These counts are estimated, not sourced headcounts or actual existing jobs. The seven regional economic sectors and total worker counts remain conserved. Do not create 259 new independent populations for political holdings.

Winterfell retains 5000 persistent persons / 1136 real households; the C4.5 fresh-save import derives generated adults' primary occupation from the C4.4 matrix while retaining authored named character employment. Keep job count reconciled with *actual generated adults*, not the arbitrary `population × 50%` planning assumption; shortages/surpluses must be surfaced in the report.

## Buildings and sign authority

Generic SMALL/MEDIUM worldgen: for each C1 site with `GENERAL_COMPACT_SETTLEMENT`, `C4.5` calculates `houses = C1.householdBuildingDemand`, then `workplaceCount(kind) = C4.4.buildingDemand[kind].workplacesRequested` for eligible, safe modules. These counts are NOT capped by the old 48/180 test limits. The planner emits stable site/lot IDs, streets, farmland, and chunk-local indices; the game's C2 terrain survey remains the physical authority.

C4.5 signs are generic building names, not invented household or dynasty labels; instance-relative castle workrooms are a separate concept. C3 and Winterfell V3 painters both delegate exterior signs to the same `KnownWorldStructureSigns` helper, using Minecraft 26.3 `SignTextSlot.FRONT`.

World activation is **optional** and remains *new-chunk-only*. Winterfell V3 is excluded from C2; additional V3 specialty facilities are appended to its own index rather than overwriting castle geometry.

## Remaining gates before worldwide live simulation

1. B4 population/household/identity authority for 197 inhabited sites beyond Winterfell; NPC creation events need a site ID. No duplicate cohorts as extra people.
2. Current-period job staffing ledger with per-building reserved worker slots, per-person jobs and persistence; unemployment, wage rules, promotions, family care and migration.
3. Actual land/soil/crops and food productivity, farms consuming land and resources; 64×64 visual field plot does not determine yield.
4. MAJOR and SPECIAL physical archetypes, shoreline/slope/volcanic/ruin exceptions and castle interiors.
5. Dynamic construction projects on existing terrain with resources, labor and faction authority. Existing C2 chunk creation never revisits loaded or player-edited chunks.
6. Build/test/performance: 26.3 Gradle, sign NBT saves, save-reload, long-run worker conservation, fresh terrain safety, geographic distances, full-site memory/CPU.

Do not claim full worldwide NPC activation, zero housing backlog from terrain surveys, or autonomous construction until these gates are satisfied.
