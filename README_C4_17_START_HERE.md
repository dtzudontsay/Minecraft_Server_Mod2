# C4.17 / B4.2C — Dense Settlements & Visible Daily-Life Refinement

## One-batch, guarded installation

This is an **incremental update for a project that has successfully installed C4.16/B4.2A (including its local-compatibility and regression-test fixes)**. It does not regenerate B4.0's 3,760,684 person records, change any person/household/occupation ID, or touch any Minecraft save.

1. **Make an external copy of your `run/saves` directory anyway.** The installer does not touch it, but worldgen changes are inherently incompatible with mixed old/new generated chunks.
2. Extract this archive **into** `D:\GameOfThronesMod2`, retaining the `C4_17_STAGE` directory as a directory. Do **not** overwrite the live Java files manually.
3. In PowerShell, run `./RUN_C4_17_CHECK_ONLY.bat`. All four existing source files should show `READY`, the new physical controller `NEW`. If anything says `UNKNOWN_LOCAL_EDITS`, stop and supply the exact modified file(s). Do not forcibly overwrite.
4. Run `./RUN_C4_17_DENSE_TOWNS_AND_PHYSICAL_DAILY_LIFE.bat`. This runs tests; snapshots managed sources **and all existing derived construction/commute/projection/daily-worksite resources**; installs sources; regenerates C4.10, C4.11, commute, B4.1A.1 projection, B4.2 worksite resources; and runs `gradlew.bat clean build`.
5. **If any step fails, the installer automatically restores its snapshot** and prints an error. Don't run older installation BAT files to recover. Keep the full console output.
6. Test physical architecture in a **fresh Known World save**. Old chunks cannot be retroactively regenerated safely.
7. For rollbacks later, use `py -3 tools/geodata/c4_17_guarded_install.py --rollback latest` or provide the exact timestamp ID from the installer. If you have installed a newer batch after C4.17, do not roll back blindly over the newer batch.

## Changes included

### C4.17 compact settlements

- Default street-cell pitch moves from 31 to 25 blocks (~35% less nominal plot area). C4.11 Java painter uses the same pitch; source lots and renderer therefore remain coherent.
- All three existing residential **construction** templates (`HOUSE_SMALL`, `HOUSE_MEDIUM`, `HOUSE_URBAN`) can occur in Small, Medium, and Major settlements, in varied, deterministic proportions. This is a *proxy* housing distribution; it is **not** claimed to reflect an authoritative person's household wealth or noble rank yet.
- Same-type homes have variable footprint and setback sizes, without duplicating household IDs, workplace counts or staffing slots.
- Preserves C4.16's grooves/groves, regional vegetation, curved lanes, belt connector spines and castle improvements. Major's first urban ring is moved to 8 cells to respect the larger castle curtain at reduced street pitch.
- Buildings remain block/cardinal aligned and streets are still based on a shared deterministic street-node system. This is denser and more irregular, but **not a complete self-organizing medieval city-planning engine**.

### B4.2C visible physical routines

- A new shared navigation owner stops issueing contradictory work/home path commands. Retarget cooldown is 160 ticks. Movement request speed is 0.48 (previously 0.85), but Minecraft's own AI goals remain a possible source of movement variation.
- Home/household positions are per-person, not one doorstep target. No hard home-range `snapTo` teleport; the controller moves actors on paths, or reports failure.
- Work destinations are per-person; enclosed workplaces use their southern entrance and attempt an interior point when nearby; open-air farms, gardens, pasture and forest camps can target their plot interior.
- In leisure phases, some villagers stay home; others select bounded nearby taverns/inns/markets/schools/courtyards as physical destinations. Some periodically approach another *currently visible* resident. **Those movements do not create persistent friendship or witnessed-event records.**
- Long trips can route via cardinal C4.10 wall openings, including the Major inner curtain, using waypoints. This is not a verified full road network: physical pathfinding can still fail and no chunk is force-loaded.
- On physical projection, a work-phase resident can be placed near an inferred workplace and leisure-phase resident near a social destination **when the relevant home-index tile is within the player scan area**. This is deterministic phase-based placement, **not real offscreen per-step travel history**; people whose homes are far outside that scanned area are not globally discoverable at every worksite yet.
- Once observed near a player, an avatar is kept while nearby even if it moves beyond its original home radius, rather than being culled solely because of home distance.
- Exactly 128 worldwide projected actors remain the budget. Winterfell V3 and its authoritative NPC registry, B4.1B K-chat and privacy records remain separate.

## Key remaining limitations

This does **not** yet guarantee 100% workplace attendance: terrain/pathfinding, missing physical lots, deferred commuting assignments, and other vanilla goals may interfere. It does not implement authoritative farming/forge economic transactions, permanent individual world-space travel, school enrollment or lifelong persistent friendships. B4.2's offscreen daily schedule and skill estimates remain provisional, not evidence that a specific task occurred.

## In-game diagnostics and evaluation

After `./gradlew.bat runClient` in a fresh test world:

- `/kwdaily status`
- `/kwdaily attendance`
- `/kwdaily nearby`
- `/kwactors nearby`

Watch an NPC at home and at work, particularly outside and inside castle walls, around farms, and near taverns. Compare initial speed and rapid heading reversals. When NPCs fail to reach a specific workplace, the worksite's existence and actual door path should be checked before assuming unwillingness.

## Source paths modified

1. `tools/geodata/c4_10_shared_layout.py`
2. `src/main/java/dev/dtzudontsay/knownworld/world/generation/settlement/KnownWorldC411GlobalWorldgen.java`
3. `src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java`
4. `src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42DailyLife.java`
5. **New** `src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42PhysicalRoutine.java`

Also included are a guarded installer, BAT entry points, Python tests, and this README. Build verifies compilation in your actual Fabric environment; separate isolated Java syntax compilation is not a substitute.
