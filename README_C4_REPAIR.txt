Known World C4 repair / full-file completion for master, October 21 2026.

Repository findings:
 - C1/C2/C3 resources exist in the pushed master branch.
 - tools/geodata/build_c4_settlement_layouts.py is missing from master.
 - src/main/resources/data/knownworld/scenarios/agot_298_ac/settlement_construction_c4_v1.json is not yet generated.
 - KnownWorldUniversalWorldgenC2.java currently uses settlement_construction_c2_v1.json.
 - C2 forwards FIELD into C3 even though C3 rejects FIELD. The guarded C4 installer fixes both points.
 - KnownWorldChunkGenerator already has the existing C2 hooks. No edits needed.

INSTALL (no C1/C2/C3 rerun):
 1. Extract this ZIP INTO the project root D:\GameOfThronesMod2 with paths preserved.
 2. In an OPEN PowerShell window:
      cd D:\GameOfThronesMod2
      powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\RUN_C4_CHECK.ps1
 3. Read results in the window and C4_INSTALL_LOG.txt. No more fast-closing batch window.

Alternative: double-click INSTALL_AND_VALIDATE_C4_SETTLEMENTS.bat; now PAUSES at end.

WHAT THE SCRIPT DOES:
 preflight -> 9 Python tests -> generate C4 manifest -> reproducibility check ->
 guarded one-time C2 patch (with timestamped backup) -> postflight -> Gradle build.
 If preflight fails, no Java files are patched.

If the Gradle build succeeds, create a NEW Known World TEST WORLD. Use
 /kwc2 status
 /kwc2 sites
 /kwc2 site site_nw_031
 /kwc2 site site_sw_023
 Then teleport in spectator mode to the reported anchors.

Ramsgate: X=-3454784 Z=-1589008
Acorn Hall: X=-4115723 Z=-284673
Y=250 is a viewing starting point only, not guaranteed above all terrain.

ONLY THESE TWO PILOTS are activated. Existing chunks are not retroactively rewritten.
 There are no new NPCs, household signage, real employment, full castle complexes,
 or general worldwide construction in this package. Preserve and back up test saves.

This repair was checked against the pushed repository filenames and runtime
 source signatures; the real Gradle build and first physical in-game observation
 must still run on your machine.
