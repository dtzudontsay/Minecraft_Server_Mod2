# Step C1–C3 — Universal Settlement Presets (blueprints and capacity plans)

**Scenario:** AGOT 298 AC. **Not** a completed physical settlement generator or living economy.

This batch deliberately separates **data generation**, **physical worldgen** and **simulation decisions**. It is possible for a plan to exist for all 253 mapped observations without a single new structure being placed in a Minecraft world. All coordinates are inherited from B2/B3 as *accepted geographic planning anchors*. Y levels, terrain surveys and build footprints are NOT approved. `noWorldgenActivation=true` is a hard invariant.

## Data authority and identity

- Input: `world_sites_b2_b3_v1.json` built in B2/B3: 253 *marker observations*, 259 political holdings, <=253 unique physical sites. No new map coordinate invented.
- Output: `settlement_presets_c1_v1.json`, one planning profile per **unique physical site** and 259 holding cross-references. Records carry source marker IDs, canonical geographic references and existing political house/holder IDs. Do not create new holdings.
- Five Forts: five independent monumental sites; **not** five invented lords. Greywater Watch: mobile seat, no generic fixed castle. Stygai/Yeen/Valyria: ruined/no normal resident census. Red Keep: nested in King's Landing, no second city population.
- One family's sign must come from **actual household membership** of individual persisted characters. Dynastic ownership of the city does not imply every residential sign says "Baratheon" or "Stark".
- No invisible cohorts, duplicate residents, or new employment assignments; planning numbers are scenario estimates tagged INVENTED/CONTEXT as appropriate.

## A — Tree and lava safety

The *same constraint applies to every future archetype.* A real chunk decorator must reserve the occupied footprint **plus two neighbor source chunks** before natural feature decoration. It then sanitizes existing planned plot-space trees, leaves, surface lava and foliage. Never globally clear natural features for the whole region. Only edit new chunks, never player-modified existing chunks. The C1 JSON encodes this policy, but enforcement on non-Winterfell sites is **pending** C's actual generic chunk decorator. Winterfell currently implements its own worldgen reservation.

## B — Residential signs

C1 emits a `householdSigns` contract requiring a real persistent household display name. The *dwelling* gets a family sign when a household is assigned in B4. Generic home lots have `UNASSIGNED_HOME` internally; no false family names will be put on signs. A sign text renderer must use block-entity state and localize/persist without forcing chunks to load during worldgen.

## C/D — Jobs, buildings and staffing capacity

The `c1_building_grammars.json` library describes 43 planning modules across 14 regional material/style palettes, including three house geometry variants and temporary faction camps. These are **geometry contracts**, not Minecraft block placements. More than thirty workplace types span farm, garden, pasture, fishery, forest camp, mine, smithy, carpentry, masonry, weaving, markets, warehouses, docks, kitchens, bakeries, mills, taverns, inns, stable, barracks, armory, administration, temple, healer, school, baths, keep, fortification, monument, and camps. The **same** existing 21-role worker distribution from B2.3 is allocated to these workplaces; cooks split into kitchens/bakeries/mills/taverns, guards support armories, etc. This **reassigns** worker demand rather than multiplying it.

Examples of *provisional maximum staff* per building: FARM 12, BAKERY 6, SMITHY 6, INN 8, BARRACKS 55, ARMORY 8, ADMINISTRATION 18. Farms have field annexes in an **outer agricultural belt**; a farm is not one building per farmer. Individual workers later choose workplaces using occupancy slots, shifts and availability. C1 computes **demands**, not scheduled workers.

Brothels are policy-dependent: `permitBrothel=false` by default. They should be placed only when culturally, legally and scenario-appropriately allowed, and should not be assumed ubiquitous. The existing occupation catalog already included `courtesan` and `prostitute`; no NPC activity is created.

52 additional **contextual** occupations are appended to the existing seven in `occupations_extensions.json`. This expands reference vocabulary but does not assign them to characters or mutate the current 21-role workforce percentages. Assignments, qualifications and skills remain B4/D.

## E — When NPC lifecycles start

Do **not** run continent-wide daily routines yet. B4 must establish authority for each individual NPC, household, home location, workplace and current world-clock phase, reconciling existing Winterfell NPC persistence. D can then handle shifts, crop cycles, birth/death, marriage, apprenticeships, job changes, food consumption, rest and travel. The existing simulation's population cohorts must not become a second phantom population.

## F — Dynamic growth and faction construction

Site plans expose stable, append-only phases and five future authorization actions: `ADD_HOUSING`, `ADD_FARM`, `ADD_WORKPLACE`, `ADD_WALL`, `ADD_TEMPORARY_CAMP`. Every lot needs a stable site+phase+lot identity and a construction ledger; worldgen must be deterministic in unexplored chunks, and a separate, server-thread construction executor is required for already-generated chunks. Faction-owned camps will need lifetime, owner, cleanup policy and safety checks. No AI/faction decision is implemented here; this is the groundwork for F/G.

## G — Compact settlements and Winterfell

Current Winterfell V3 reaches **about 4,500 Minecraft blocks** from its anchor. This is deliberately *not* silently relocated because it would conflict with generated chunks and individual NPC home coordinates. New major preset default keeps a linked core within **860m max radius**, medium **440m**, small **210m**, special **320m**. Residential blocks should cluster around a traversable civic center; outer farms connect to main roads, not distant unrelated satellite towns. Existing Winterfell needs a separate migration/rebuild strategy and an actual scan of its home/worldgen source before moving any building.

Large city budgets (for example 500,000 in King's Landing) can far exceed first-phase plots. Demand backlogs are explicit; they do not pretend several hundred thousand people fit into 2,500 cottages. Multi-story buildings, high-density district tiles, phasing and performance budgets need future implementation.

## H — Population-based scaling

`peopleTarget`, provisional labor-participation, 21 worker role estimates, household average and first-phase housing/workplace capacity are computed for **every** physical site. If housing demand exceeds plot limits, backlog stays explicit. Occupation distributions are of EMPLOYED WORKERS, not all residents. Actual B4 resident census takes precedence once individuals exist. Never scale in-world physical structures directly from an invented research number without reconciliation.

## I — Mountain towns

All presets carry local slope caps, water exclusion, stepped terraces, retaining wall and stair/switchback road requirements. For actual mountains use the same final terrain sampling as `KnownWorldChunkGenerator`; do not assume elevation raster height equals the fully generated terrain. Cliff-side villages require retaining structures, bridges and valid access routes. C1 has **no heightmap/Y survey** and therefore does not claim to have solved mountain worldgen yet.

## J — Additional occupations

The existing 136 occupations and seven extension occupations were preserved, with 52 new PLAUSIBLE reference roles (road warden, mason's related trades, stable master, canal keeper, bath attendant, granary keeper, forester, surveyor, etc.). Assigning NPCs to occupation-specific buildings, skills, pay, shift clocks, dependencies and business ownership is B4/D. Reference IDs don't create NPCs by themselves.

## K — Governance

Already present: `LandedHoldingManager`, dynasty managers and hierarchy, `TitleManager`, claims, `SuccessionService`, `SuccessionRuleManager`, `NonDynasticSociety`, reference culture/religion/roles, authored `landed_holdings.json`, regional influences and zone polygons. C1 *references* political IDs already assigned in B2/B3. The existence of title/succession code **does not mean** every site has a correct 298 AC individual officeholder, institution, law, effective tax collection or decision system. F will unify capital sites, de jure ownership, de facto holder, governance law and construction decisions.

## What this package validates

- No marker lost, no marker included twice; 259 political records preserved.
- Distinct site identities for overlapping observations; no phantom extra populations.
- Staff-per-building capacities and job shares sum exactly to the worker demand.
- Ruins and legendary sites never receive a default normal-town construction template.
- Housing and workplace deficits remain visible.
- Compatible model data can be inspected via `/kwpresets`; no actual world changes.

## Next physical implementation (C worldgen)

1. Local terrain survey using **final generated** terrain, land mask and 16x16 neighbor areas; approve a specific core and road entry point independently from map marker.
2. Chunk-local structure decorator with robust forest/lava reservation, terrain-aware foundation and step access. Keep natural tree generation outside settlement lots. Save-safe first generation only.
3. Template generation by archetype, not cosmetic palette alone; build working wells, beds, stairs, doors, lantern supports, job blocks, storage/production facilities, signs when real families are assigned.
4. Prototype a **small** site other than Winterfell and test slopes, roads, docks and world reload; then **medium**, then **major**. Monumental Five Forts and canal/pyramid cities need individually designed additions, not scaled-up basic keeps.
5. Introduce deterministic phased construction and individual-NPC projection after B4 confirms identities, not before.

All places may be browsed in the generated offline HTML. This package is intended as **concrete, testable prerequisites** rather than pretending city worldgen is already complete.
