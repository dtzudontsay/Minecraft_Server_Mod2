# C4.2 — Physical street grading and safe entrances

## Current scope

This is an incremental C4 construction improvement, **not** another blueprint stage.
The approved C4.1 construction manifest and its 2 pilots remain unchanged.
Ramsgate: X=-3454784, Z=-1589008. Acorn Hall: X=-4115723, Z=-284673.

C2 chooses site, safe plot, terrain elevation, and writes only to the new chunk.
C3 builds the 43 architecture types. C4.2 supplies a bounded 3D road painter.
It replaces C2's outdated assumption that every door is linked to the original
`pilot.z` axis with a per-chunk set of surveyed, approved actual entrances.

## Specific improvements

- Approved door fronts are indexed per decorated chunk, not rediscovered for
  every road block across every settlement building.
- Geographic terrain samples smooth tiny height spikes; bounded cut/fill is
  at most four blocks (no arbitrarily tall walls of stone).
- Road tiles taper towards the nearest surveyed entrance within five blocks.
- Road columns gain eight blocks of headroom and remove leftover logs and lava.
- Shore tiles are skipped; sharp cliffs above an 8-block neighbor spread are
  skipped rather than bulldozed. Roads still may have gaps on harsh terrain.
- Plot survey results are cached for consistent construction and better speed.
- Winterfell, NPC simulation, individual signs, and existing chunks are untouched.

## Deployment

Extract package into repository root and run:

    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\RUN_C4_2.ps1

The script writes C4_2_BUILD_LOG.txt and runs tests and Gradle. Do not rerun C1,
C2 or C3 installation scripts. Test with a NEW world or ungenerated chunks.

## Not covered

Terrain-aware *routing* around cliffs, retaining walls, bridges, city gates,
unique keeps/forts, family name signs and populated workplaces are separate
future work. They must not be claimed complete merely because this builds.
