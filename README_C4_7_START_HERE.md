# C4.7.1 — Minecraft 26.3 Architectural Catalogue Compiler Fix

The preceding C4.7 installer failed to compile because `Blocks.ORANGE_STAINED_GLASS` is not an exposed field in your Minecraft 26.3 mappings. It rolled back correctly. This full package replaces the problematic line in the **installer source template** with safe `Blocks.GLOWSTONE.defaultBlockState()` above the enclosed forge, preserving warm light. The static test now rejects the obsolete field, instead of auto-stubbing it and missing the error.

**Use exactly this installer:** `RUN_C4_7_1_ARCHITECTURE_KEEP_OPEN.bat`. Extract the entire zip directly into `D:\GameOfThronesMod2`, allowing replacement of the `tools/geodata/...` C4.7 files. Do not run the older `RUN_C4_7_ARCHITECTURE_AND_GALLERY_KEEP_OPEN.bat` or C4.6 installers. At the confirmation prompt type `NEW_WORLD`.

The output log is `C4_7_1_INSTALL_AND_BUILD.log`, in the mod root. All existing NPCs, 128 visible NPC limit, compact Winterfell layout, signs, world saves, and 61 gallery templates remain unchanged. If the Gradle build fails, the installer restores the original architecture Java file and removes the new detail class. **A Gradle BUILD SUCCESSFUL is required before launching the game.**

## Original C4.7 design details

# C4.7 — Full 61-Blueprint Architectural Overhaul

## What this package changes

This is a **source-only upgrade** for Minecraft/Fabric 26.3, starting from the **successfully installed C4.6.1** package. It retains the connected C3 architecture program used by Winterfell's newer specialized workplaces, the C2 small/medium settlements, and the existing superflat preview command.

- `KnownWorldC3Architecture.java`: guarded insertion of a deterministic detail layer, profile-driven masonry/wood/brick walls, multiple gable/roof orientations, and a fix to avoid free-flowing water as a public-bath workstation.
- `KnownWorldC47ArchitecturalDetails.java` (new): enhanced exterior profiles, purposeful furniture, usable access, supported lanterns on all floors, and type-specific decoration. Works via the **existing C3 block sink** for both chunk generation and gallery world painting.
- `c4_7_review_catalog.csv`: all 61 actual C3 programs, original footprints/floor counts, intended C4.7 distinctive elements and notes for reviewing.

**Unchanged:** Winterfell's castle walls/keep, roads/ring layout, household coordinates, 5,000 NPC identities, 1,136 households, NPC occupation distribution, 128 visible NPC budget, settlement plots, farms, worldgen site authorization, C4.6 gallery command and C4.6 sign system. The older legacy V3 Winterfell homes and old workrooms still use their previous renderer. The new C3 venues in Winterfell and C2/C3 buildings in other settlements get the upgrade on newly generated chunks.

### Specific requests implemented

| Building(s) | New visible elements |
|---|---|
| Temple | Monumental stone exterior, deep-slate roof, stone altar/sculpture, pews and candles |
| Shipyard | Large timber shed with opening at south end, timber keel and unfinished hull ribs; does **not** spawn a real boat |
| Smithy / armory / brickworks | Brick exteriors, enclosed glass-faced kiln/forge, anvil, water cauldron, brick stacks, target for armory; no exposed lava |
| Stable | Fenced stalls, hay and water cauldrons; **no animals spawned** |
| Tavern / inn | Bar/counter, stools/benches, tables and storage; upstairs furnishing for inn |
| Public bath | Below-floor contained water basin with masonry rim and safe entrance step; no free-floating source station |
| Mine / canal works | Five-block-deep masonry-lined inspection shaft with a supported ladder; **not a dynamically expanding mine** |
| Justice hall | Stone façade, lectern/judge's seat and courtroom seating |
| Market | Separate vendor stalls/counters and goods displays |
| Mill | Stationary timber mill-wheel shape on outside-facing wall, grain storage; no mechanical rotation |
| Fighting pit | Exposed roof/open-air center, dirt arena and stepped spectator seating |
| Forest camp | Horizontal timber-log stacks; not an identical military tent |
| Dock / ferry landing | Loading platform, fences and storage **within the footprint**; does not alter unapproved waterfront terrain |
| Courtyard | Stone sculpture and benches |
| Chandlery | Candle-topped tables with workshop storage |
| Cart depot | Small wagon/cart form and fenced stock bay |
| Brewery | Brewing tanks, cauldrons, barrels and raised workstations |
| Brothel | Interior partitions and actual beds; no NPCs generated here |
| Butchery | Fenced holding bay, hay and smoker; no livestock spawned |
| Barracks | More training targets and rack-like equipment; no armor-stand entities in world generation |
| Bank / Administration | Stone monumental profiles, cash counter / vault / council seating |
| Arcane study | Enchanting table, shelves and brewing area |
| All others | Distinctive material/trim profiles where appropriate, basic trade furnishment, supported lanterns **on each existing floor** |

**Honest visual limitations:** These are substantial *procedural/block-level improvements*, not handcrafted high-resolution schematic assets. Footprint width/depth does not grow: expanding special buildings into adjacent plots needs a later collision-aware replan of the settlement. Medieval furnishings use Minecraft blocks rather than custom models. No moving mill wheels or actual cuttable ships. Candle blocks are decorative, while lanterns provide lighting.

## Installation (single script, no old installers)

1. **Back up any saved worlds you care about**, and close Minecraft.
2. Unzip all C4.7 files into `D:\GameOfThronesMod2`, preserving directories. They should sit alongside the original `gradlew.bat` and `tools\geodata`.
3. In PowerShell:

```powershell
cd D:\GameOfThronesMod2
.\RUN_C4_7_ARCHITECTURE_AND_GALLERY_KEEP_OPEN.bat
```

4. Type `NEW_WORLD` and press Enter.
5. Wait for the script to report `BUILD SUCCESSFUL` / `COMPLETE`. It verifies the expected **C4.6** source anchors, tests 61 programs, snapshots `KnownWorldC3Architecture.java`, installs the new renderer and runs `gradlew.bat build`. If any stage fails it automatically restores the old C3 source and removes the new class, without touching saves. The full report is `C4_7_INSTALL_AND_BUILD.log`.
6. Do **not** rerun C4.3–C4.6 setup batches or the old sign-repair batch.

**Not compiled here against your local Minecraft 26.3 dependencies.** The package's Java stub compilation proves only that the new helper's Java syntax and basic geometry execute. Gradle and the in-game review are necessary.

## Superflat gallery test — all 61 real blueprints

Start with a **new Superflat Creative Overworld**, with cheats/commands enabled. Stand at a clear empty area, then run:

```mcfunction
/kwgallery build
```

The existing C4.6.1 command queues every actual template and its numbered sign, in a **7-column gallery with 78-block spacing**, building gradually rather than all at once.

```mcfunction
/kwgallery status
/kwgallery cancel
```

**Do not repeatedly use `/kwgallery build` in the same footprint.** Make a fresh superflat world, or stand at least 750 blocks from an old gallery before building another. The gallery deliberately cannot be run in your Known World custom terrain.

The numerical labels depend on the command's sorted catalogue; use the **template ID on each sign** rather than old gallery numbers. The CSV records all exact names and physical sizes.

## Winterfell worldgen test

Launch:

```powershell
.\gradlew.bat runClient
```

- Create a **brand-new Known World** (older chunks cannot be redrawn by a code update).
- For stability, temporarily use render distance 6–8, simulation distance 4–6 and Distant Horizons distant generation disabled.
- Teleport in Creative/Spectator:

```mcfunction
/tp @s -4050173 200 -1976496
/kwinterfell3 status
```

Inspect the newly projected C3 specialist venues. Original Winterfell V3 houses, keep, curtain walls and wards **remain unchanged**, because they use an older generator. Signs are supplied by the existing bounded C4.6 code and are **not modified** in this batch.

## Known risks and follow-up

- No real Minecraft build was possible here. If Gradle fails, the installer rolls source back and logs the exact error. Send `C4_7_INSTALL_AND_BUILD.log`.
- A small subset of C3 programs requires explicit shore, mine or policy authorization in *real* settlements. The gallery intentionally bypasses only those placement conditions to show the model in a safe superflat environment.
- For Winterfell, only C3-identified workplaces use the new C3 detailer; a future batch may port original V3 workplaces to the new renderer.
- Gallery writing uses C4.6's existing live sink and per-tick queue. Sign handling stays in the C4.6 bounded system, not in C4.7.
- Visual complexity now increases slightly on world generation. It is still strictly clipped to each generated chunk and lot footprint.

## Reviewing and providing feedback

Open `tools\geodata\c4_7_review_catalog.csv` with Excel or a text editor. Mark any type by **ID** (e.g. `TEMPLE`), not only its former #number. Send a screenshot or a list of the changes you want, especially any clipping, incomplete water basin, unreachable rooms, floating lamps or odd roof intersections.
