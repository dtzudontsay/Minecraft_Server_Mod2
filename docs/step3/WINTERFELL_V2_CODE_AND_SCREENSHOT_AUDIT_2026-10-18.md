# Winterfell V2 — Real-terrain playtest audit, 18 October 2026

**Scope.** Read-only audit of current `dtzudontsay/Minecraft_Server_Mod2` `master`, files `src/main/java/dev/dtzudontsay/knownworld/simulation/casestudy/WinterfellV2.java`, `WinterfellCaseStudyInteraction.java`, `world/generation/KnownWorldChunkGenerator.java`, `simulation/NpcActivationManager.java`, `simulation/time/CampaignCalendar.java`, and `tools/geodata/build_winterfell_v2.py`; user test screenshots in `2026-10-18_19.34.43.zip`. This is an implementation plan, **not** a patch or a tested Minecraft build.

## Observed facts from in-game `/kwinterfell2 status`

- Atlas coordinate **NW-029 X=-4,050,173 Z=-1,976,496**.
- Imported **5,000/5,000** persistent NPC identities.
- Built **96/1,514** planned residence/workplace plots (~6.3%).
- Processed **2,832/77,213** road positions (~3.7%); 16 blocked/wet; 2 building plots marked unsuitable.
- Full physical actors **128/128**, reduced physical actors **128/128**.
- Controller reported **3 ms**: this is not total server MSPT and cannot be treated as a whole-world performance benchmark.
- Screenshots show partially generated roads/walls, raised entrances, square house/keep templates, raw natural landscape around/in plot areas, and Robert Baratheon rendered as baby with adult biographical character card.

## Root causes pinned to current sources

| Area | Existing implementation | Effect | Fix |
|---|---|---|---|
| Delay before first structure | `WinterfellV2.tick` lines 433–448 imports 75 NPCs/controller second; `buildNearby` runs only after all 5,000 imported | ~67 seconds before any construction; flying out/back conceals the wait | Split independent population/import, planning, construction stages; prebuild on new chunk generation |
| Slow visible buildings | `buildNearby` lines 654–687 builds max 1 plot and 150 road coordinates per second within `BUILDER_RADIUS=120`, `isLoaded` | Pieces appear within visual distance, slow to completion; still 1,418 buildings pending in screenshot | Chunk-keyed precomputed immutable structure commands; execute before client chunk delivery; optional pregeneration; no player-radius-driven building |
| Road stubs | Python `build_winterfell_v2.py` `plan()` emits connected graph over 21 sites; only nearby points get runtime blocks | Disconnected/stubby roads and long partial dirt cuts visible during development; 77,213 positions are not all built | Build per-chunk before visibility; distinguish local streets from highways and terminate at real intersections and gates |
| Width and elevation | Runtime lines 669–684 changes only one block at `(x,z)` to gravel at current `WORLD_SURFACE` | 1-block-wide uneven roads | Path centerlines, 2–3 block surface widths, grade limiting, stairs/slabs, drainage and retaining walls, pedestrian-access graph check |
| Inaccessible doors | `buildHome` lines 690–759 floors at `ground+1`, same as bottom door half, while road height uses unrelated local terrain | Interior floor obstructs doorway; external drop can exceed one block | Solid floor support at appropriate elevation, door bottom *above* floor, outside landing with grade ≤1 and verified traversable path |
| Misleading height measurements | `safeHeight` repeatedly calls `WORLD_SURFACE` during construction; roofs, water, foliage and prior placement alter subsequent readings | Floating foundations, incorrect local roads, unpredictable cut/fill | Cache natural height and fluid maps before any terrain modification; never sample edited world as if original |
| Forest/fluid clearance | 9 samples check water and relief; no footprint clearance, water/lava replacement plan, exception masks | Trees and lakes remain inside buildings/streets or collide with roofs; hazards persist | Per-site land-use masks, conservation rules, vegetation removal, lava replacement, fill/drain where allowed, protect godswood and rivers |
| Castle wall geometry | `buildCastlePass` lines 764–803 writes x-fixed columns and `z+k` thickness on all edge directions | East/west walls only one block wide, straight featureless bands | Orientation-aware solid cores, 5–9 block thick defensive walkways, towers, crenellations, planned gatehouses and navigable doors |
| Hollow keep | `buildCastlePass` lines 804–827 writes an approximately 70×43 footprint with mostly empty 22-block-tall walls and a flat dark deepslate roof | Giant dark cuboid, little architecture or internal use | NBT/Jigsaw/custom structure templates for major keep; actual floors, roofs, halls, stairs, courtyards, private quarters, servant wings |
| Stark's home | Python `slot_homes()` assigns 76 `CASTLE_QUARTER` units in rows; named nine occupy `castle_home_0001` | Principal noble family appears to live in generic bunkhouse | Castle complex is one architectural compound; housing units are room references inside it, not 76 identical freestanding homes |
| Interaction | `WinterfellV2.inspect` lines 633–651 and legacy `WinterfellCaseStudyInteraction` both register `UseEntityCallback`; inspector silently exits for missing plan, mapping, simulation or authored ID | Some tagged NPC right-clicks produce no output; precise intermittent cause not yet established | Unify handler, server-side resolve via persistent NPC key, visible diagnostics for mismatch, verify tag lifecycle and entity UUID mapping |
| Visitor age | Visitors in `WinterfellV2.loadPlan` are initialized with `birthYear=298`; `projectNearby` line 597 does `setBaby(298 - person.birthYear < 15)` | Robert (actual birth 262) and other visitors render as babies | Resolve `NpcState.identity.birthYear` first; use existing `CampaignCalendar.year()`, baby if age<16; synchronize on age change |
| Reduced NPCs | `projectNearby` lines 556–617 uses `villager.setNoAi(!detailed)` | Reduced 128 are inert statues, not low-frequency AI workers | Occupation-aware custom NPC projection, schedulers, bounded pathfinding and separate client rendering from simulation |
| Scaling all-world | `projectNearby` scans all 5,000 residents and sorts eligible each controller second; `buildNearby` scans all plots + roads | Multiply by thousands of settlements becomes prohibitive | Spatial indexes by cell/chunk; priority queues for construction tasks; activation bounded by nearby chunks and requested budgets |
| Save consistency | V2 uses its own `knownworld_winterfell_v2_test.json`, simulator uses its own authoritative NPC persistence | Two save stores need crash reconciliation; restart must not create or lose people | Versioned transaction/journal per settlement, imported IDs reconciled with `AuthoredIdRegistry`, construct state committed per chunk and idempotently |
| Village appearance | `buildHome` chooses only a few stone/wood palettes with rectangular box volumes; vanilla village Jigsaw pieces are *not* used | Row houses look nearly identical despite occupation categories | Begin with vanilla Jigsaw/NBT building pools or improve small regional template library; role-specific interior and façade variants |

## Proposed world-scale architecture

1. **Global registry and immutable plans.** Seed+scenario+siteID+version define named and deterministic rural settlement anchors. Store authoritative land ownership, culture, population envelopes, families, road network and land-reserve; do not pregenerate the full 1:1 world as Minecraft blocks.
2. **Pre-terrain reservation and survey.** Determine local high-confidence ground elevation/slope, wetlands, relief and vegetation constraints from pristine chunk generation data. Use a reserved footprint mask (castle/market/roads/fields/woodland). Reject/relocate unsuitable local parcels before allocating NPC homes.
3. **Authoritative households and buildings.** Every live person has one stable ID and one primary home/household; buildings contain one or more units with occupancy capacity; every worker's `workplaceId` exists. Named visitors are not local residents. Reservations for future winter migrants are vacant without adding people.
4. **Chunk-keyed construction plans.** Generate deterministic block/template operations for each chunk; integrate into `KnownWorldChunkGenerator` and its structure/decorator stages with correct ordering so structures are part of the chunk before client dispatch. Handle cross-chunk templates using supported Minecraft structure systems/placement references; avoid recursively loading neighboring chunks from within a generator task. Existing chunks require explicit migration/pregeneration—do not silently overwrite player changes.
5. **Road graph + terrain profile.** Roads are graph edges between valid entry nodes, not incidental gravel strips. Apply smoothing using cached pristine terrain: ordinary roads width 3 and slopes <=1 per horizontal block, with stairs/bridges/fords as necessary. Validate pedestrian reachability from each home door to town spine and destination workplace.
6. **Major castle prefab and village tiers.** One architectural castle compound with distinct functional interiors; 10×/5×/1× villages as different *number of parcels/districts*, not stretched houses. Select vanilla-block structure pools by occupation, standing and local culture; ensure roads before houses, no collisions.
7. **Physical NPC projections.** `NpcRegistry` remains authoritative; proximate chunk-indexed person records map to one bound entity apiece. Distinct configurable full/reduced budgets (start 128+128). Preserve identity through unload/reload. Use the campaign calendar for age appearance (under 16 baby, 16+ adult). Avoid duplicate villagers or profiles on retry.
8. **Instrumentation and gates.** Before expansion: 5k unique people; household/home capacity invariant; exact individual-to-workplace assignments; 100% of road-accessible constructed doors pass reachability; save/reload twice yields unchanged IDs and constructions; `mspt` P50/P95 and FPS at 64/128/256/512 visible entities; compare with and without village entity AI; chunk-gen latencies and task backlog benchmarked separately.

## Recommended implementation order

**P0 — correctness and usability (one patch):** fix birth-year rendering, remove silent NPC inspector exits, ensure door/floor/threshold/porch, robust 3-wide roads with cached terrain heights, read-only site safety and pause on errors.

**P1 — construction redesign:** move building work out of player-proximity tick and into deterministic chunk creation/pregeneration; incorporate build reservation masks and idempotent per-chunk completion status; connected streets/gates and major castle template. This is the key architectural blocker for world-scale use.

**P2 — content and population:** create castle-room layout, 1×/5×/10× structure pools by occupation and standing; integrate residence/work IDs with live NPC movement/age/visits; scale from 5k to multiple named settlements.

**P3 — performance and world coverage:** regional settlement density and road graph, spatially indexed activation, global census invariants, load/flight benchmark, automatic generation from world seed and scenario without admin commands.

## Hard limits and feasibility

- No built-in 24-entity cap; 256 physical projections functioned in one observed test but overall server MSPT and pathfinding cost remain unknown.
- Fully pregenerating **every Minecraft chunk** across a roughly 12,000 km by 8,000 km 1:1 world is not feasible; seed-derived regional planning and on-demand chunk construction are feasible.
- Loading hundreds of thousands of **fully active physical villager entities** concurrently is not feasible. Persistent unique people can be far more numerous because inactive ones need not have spawned Minecraft entities; expensive behavioral simulation must be spatial/temporal-budgeted.
- Real-time roads and structures should be generated **before a chunk becomes visible**; current player-radius block placement is an expendable prototype.
- Population numbers are scenario calibrations, not canon censuses. 5,000 is a pilot estate cluster with 1,136 household units, not 5,000 castle interiors or a full known-world census.

**Safety:** build only in copies of the Known World. The current tester modifies an existing world with no undo, and old incomplete structures must not be blindly “upgraded” in place.
