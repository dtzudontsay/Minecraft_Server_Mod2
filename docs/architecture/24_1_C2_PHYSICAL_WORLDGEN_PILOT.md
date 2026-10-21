# Known World — C2 physical settlement worldgen, controlled pilot

Scenario AGOT 298 AC. This batch deliberately **does not activate all 253 sites**.

## Input authority and safeguards

- B2/B3: 253 atlas observations, distinct site IDs, source X/Z. All retained, none rewritten.
- C1: 259 political holdings, work demand, size classifications, palettes, archetype restriction.
- This batch: **only two** compatible `GENERAL_COMPACT_SETTLEMENT` sites (one SMALL and one MEDIUM) chosen deterministically using `c2_pilot_selection.json`. Winterfell `NW-029` is excluded.
- C1 estimates are *planning inputs*, not a live roster or confirmed census. No NPCs or jobs are created/assigned. No household surname is fabricated.
- Farmland and structures exist as chunk blocks only. Agricultural production, inventory and labor are still future integration.

## C2 block execution

- On each newly generated `ChunkAccess`, in `KnownWorldChunkGenerator.applyBiomeDecoration`, suppress biome decoration with a source-chunk margin around indexed pilot lots and roads, then invoke C2 chunk-local physical decorator after the existing Winterfell one.
- `createStructures` suppresses vanilla feature starts near pilot building areas.
- The decorator reads an immutable, build-time generated chunk index. It writes ONLY the current chunk; does not load or set blocks in neighboring chunks or server-level coordinates.
- Topography uses the same projected elevation sampling as the Known World generator. A dense footprint and entrance survey refuses water, large height deviations and extreme elevations; unsafe lots are **skipped**, not terraformed into mountains or lakes. A failed lot must later be manually re-sited in C3. Roads avoid abrupt slopes and deep fills. No roads are forced across real water.
- Residential and workplace structures: floor, foundation, walls, roof, windows, accessible 2-block doors, beds, light posts, basic workstation, outdoor road approaches. Basic palette selected by C1 region.
- Farming sample: farm building plus irrigated field, subject to valid land survey. People remain simulated only when B4/D establishes actual identities.
- Flower/tree/lava hazard removal applies to the newly generated occupied chunk and a feature-spawn margin, never to existing player edits.
- `road` and `buildLot` depend only on immutable inputs, so loading chunks in different orders is deterministic.

## What this phase does NOT yet do

- It does not physically generate MAJOR cities, special monuments, 43 complete C1 grammatical buildings, urban districts or full population capacity. These are C3/C4 tasks.
- It does not provide populated family-name signs: per-household names need real B4 persistent household IDs. Placeholder surnames would incorrectly claim every tenant belongs to the ruling dynasty.
- It does not ensure all buildings pass final in-game pathfinding: manually inspect sample terrain, door elevation, full entrances, crops, tree/lava intrusion and aesthetic results, then refine the physical templates.
- It does not perform dynamic growth, construction-lifetime persistence for built-out live changes, faction camps, jobs, marriage/birth/death or governance. C6, B4/D and F own those.
- It does not edit existing chunks or retroactively migrate Winterfell's sprawling V3 settlement. Those require separately reviewed migration tools.

## Acceptance test

1. `py tools/geodata/build_c2_physical_pilots.py` prints selected site markers / anchors.
2. `py tools/geodata/test_c2_physical_pilots.py` reports passed; `--check` reproduces generated JSON exactly.
3. `gradlew.bat clean build` passes on the actual Minecraft 26.3 Fabric mapping set.
4. Create a **new test world** or reach never-before-generated pilot chunk coordinates. `/kwc2 sites` provides exact anchor X/Z. Use a high safe altitude initially. Observe visible houses/roads/special workplaces, correct orientation, no submerged plots.
5. `/kwc2 status` returns nonzero decorated chunks only after newly generated pilot chunks are visited. Survey-safe lot count may be less than planned if topography is unsuitable.
6. Restart world: compare visible construction and check no duplicate entities; no resident NPCs should be created by this batch.

Backups: `tools/geodata/output/c2_physical_pilots/backups/`. Uninstall physical hooks through Git (user-controlled, no automatic world revert), or restore backed-up generator/configuration **before** creating a production world with these sites.
