# C4.13 — Construction worldgen diagnosis + marker-accurate teleport

## Findings from the code

1. Atlas right-click currently converts the **cursor pixel**, not the nearest pin's exact source coordinate. One image pixel is about 6,009 blocks at source scale; on screen that is often several hundred blocks even at maximum zoom. The atlas marker for Winterfell itself converts accurately to (-4050173, -1976496); the arbitrary cursor may not.
2. `WinterfellWorldgenV3` is NOT replaced by C4.11 and remains enabled at NW-029. If it and the original C2 Ramsgate / Acorn Hall construction are missing on NEW chunks, this cannot be explained by C4.11's exceptional-site terrain rules alone.
3. `KnownWorldChunkGenerator` only runs if the world uses `knownworld:known_world` world preset. A normal vanilla world can still open the map and use the atlas teleport packet, but no Known World construction decorators execute.
4. On a save whose chunks were generated before C4.11 / Winterfell activation, these decorators do not automatically retrofit those chunks.
5. C4.11's worldwide generated resources include *194 construction-attempt sites*, not all 248. 54 have protection or nonbuildable statuses. 23,786 lots were deferred for template policy before terrain checks. Numbers on planning output are **not evidence Minecraft painted blocks**.

## Install

Extract this ZIP into D:\GameOfThronesMod2 (the same directory as build.gradle); overwrite when prompted.
Run `RUN_C4_13_DIAGNOSTICS_AND_MAP_SNAP.bat` then run a normal Gradle `clean build` and `runClient`.

The script only patches these two existing files:
- `src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java` (register the diagnostic command)
- `src/main/java/dev/dtzudontsay/knownworld/client/atlas/KnownWorldAtlasScreen.java` (snap near marker)

It also adds `src/main/java/dev/dtzudontsay/knownworld/debug/KnownWorldC413ConstructionDiagnostics.java`. Both original Java files receive time-stamped backups. It never touches Minecraft saves.

## Test in Minecraft

**Use a NEW world with the world preset `Known World`** when creating it.

Type `/kwconstruction status` (shows the ACTUAL overworld generator class and C4.11 index runtime status); then `/kwconstruction anchors`; then `/kwinterfell3 status`. The expected generator is `dev.dtzudontsay.knownworld.world.generation.KnownWorldChunkGenerator`, not a vanilla `NoiseBasedChunkGenerator`.

The three older physical generation sites are:
- Winterfell `NW-029`: `X=-4050173 Z=-1976496`
- Ramsgate `NW-031`: `X=-3454784 Z=-1589008`
- Acorn Hall `SW-023`: `X=-4115723 Z=-284673`

Use the atlas right-click ON the corresponding pin (within 9 SCREEN pixels). The popup X/Z must match that marker exactly. Clicking away from markers continues to teleport to arbitrary map coordinates as before.

On a truly new world, load the exact Winterfell marker and run `/kwinterfell3 status` AGAIN; `chunks generated this session` should increase if the Known World worldgen hook executed. If the active generator is correct but the chunk counter remains zero, the next fix is the hook/runtime lifecycle, not terrain relocation.

If `/kwconstruction status` says C4.11 index missing, check the generated resource in `build/resources/main/data/knownworld/scenarios/agot_298_ac/universal_construction_c4_11_global_v1.json` and the tiles in the adjacent `c4_11_global_tiles` folder. Also look in `run/logs/latest.log` for `C4.11 SITE APPROVED`, `C4.11 SITE FAILURE`, `C4.11 LOT FAILURE`, and `Winterfell V3 chunk worldgen ready`.

## Boundaries

This diagnoses the missing structures and fixes the map-coordinate test. It does **not** rebuild old chunks, change C4.11 world demand, or claim that all 194 sites rendered successfully. Mountain/shore failures are still possible but cannot explain Winterfell disappearing from a fresh correct-preset world.
