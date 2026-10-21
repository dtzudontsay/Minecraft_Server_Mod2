# Known World / C3 Architectural execution layer

**Status:** new-chunk block-construction renderer. No automatic worldwide deployment.

## Architecture

- B2/B3: source-of-truth for atlas site identity, map anchors, political links, and proposed population.
- C1: metadata for **43 building modules**; working geometries and staffing limits.
- C2: **two selected physical pilot settlements**, their safe terrain surveys, chunk/plot indexes, protection from vanilla trees/lava and existing-chunk mutation.
- **C3 (this batch):** architectural program catalog and deterministic per-block construction for C2's approved lots. Source: `c1_building_grammars.json` -> `architecture_templates_c3_v1.json` -> `KnownWorldC3Architecture.buildLot()`.
- C4: scale layout and build footprint across more sites, bespoke keeps, city walls, Ghiscari pyramids, Yi Ti's Five Forts, inhabited/uninhabited ruins, hill settlements.
- C5: worldwide rollout after verified terrain and persistence tests.
- C6: new plot/phase construction ledger, demolition, faction camps; requires explicit authorization.
- B4/D: real people, families, housing assignments, work shifts, food production, life cycle, household signs.

## All 43 executable IDs

`FARM`, `GARDEN`, `PASTURE`, `FISHERY`, `FOREST_CAMP`, `SMITHY`, `CARPENTRY`, `MASONRY`, `WEAVERY`, `MINE`, `MARKET`, `CART_DEPOT`, `DOCK`, `KITCHEN`, `SERVICE_HALL`, `INN`, `BARRACKS`, `ADMINISTRATION`, `TEMPLE`, `BAKERY`, `ARMORY`, `MILL`, `TAVERN`, `STABLE`, `BROTHEL`, `HEALER`, `WELL`, `GRANARY`, `WAREHOUSE`, `GUARD_TOWER`, `GREAT_HALL`, `KEEP`, `PUBLIC_BATH`, `SCHOOL`, `COURTYARD`, `RITUAL_GROUND`, `MONUMENT`, `RUIN`, `CAMP`, `HOUSE_SMALL`, `HOUSE_URBAN`, `HOUSE_MEDIUM`, `TEMPORARY_CAMP`.

Aliases for the current C2 pilot: `HOUSE` -> `HOUSE_SMALL`/`HOUSE_MEDIUM` depending on pilot tier; `FIELD` stays under its C2 field painter. A catalog entry makes the type constructible when C4 plans a suitable lot; it **does not automatically place it**. Some roles share a physical house shell but differ in dimensions, number of levels, equipment and requirements.

## Construction invariants

1. New-chunk decoration only; chunk-local clipped writes (`ChunkAccess`). Existing explored chunks never re-generated.
2. Inherits C2 whole-lot terrain grade, land/water and entrance-access survey; out-of-policy building types cannot be constructed in the pilot.
3. House and workshop doors contain lower+upper halves and remain unobstructed.
4. Multi-level buildings include upstairs decks with ladder shafts and adjacent supported ladders. Lanterns rest on fences and don't float.
5. Open forms (pastures, wells, monuments, ruins) are not generated as normal populated homes.
6. Real Minecraft workstation blocks furnish interiors; this **does not** imply staffed NPCs or simulated production.
7. Family surnames are not generated from guessed houses. Wall-sign text requires B4 persisted household/person links.
8. The 43 module records have role-specific *physical block renderings*, not 43 lore-accurate custom monuments. Five Forts and other scenario-specific architecture require bespoke later C4 work.
9. `BROTHEL`, `TEMPORARY_CAMP`, marine docks, and mountain-resource mining retain explicit policy/terrain constraints.
10. C2's existing worldgen is the only module with authorization to execute. C3 only replaces how approved lots are painted.

## Caveat about changing the renderer

The C3 installer upgrades the renderer used for **newly generated** C2 pilot chunks. It does not edit old buildings. A new test world is strongly recommended; existing pilot chunks may remain C2 until regenerated in a separate test world. Do not reset a valuable save to see C3 changes.

## Acceptance tests

- `py tools/geodata/build_c3_architectural_templates.py --check`
- `py tools/geodata/test_c3_architectural_templates.py`
- `gradlew.bat clean build`
- `/kwc3 status`, `/kwc3 template BAKERY`, `/kwc2 sites`
- In a new world, inspect the actual **two** C2 pilot villages. Verify doors, furnishing, farm rows, roofs, lanterns, upstairs ladder/landing, chunk boundaries, absence of trees/lava and road accessibility. Send screenshots of failures.

**Not completed by C3:** physical nationwide rollout, one building for every target resident, real ownership signs, full medieval occupation workflows, farmer-managed crop harvests, castle walls and unique Five Forts, dynamically building new lots, governance decisions or family lifecycle simulation.
