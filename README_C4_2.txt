KNOWN WORLD C4.2 - PHYSICAL TERRAIN STREETS / DOOR APPROACHES

Prerequisite: the recent C4.1 run must have printed BUILD SUCCESSFUL.

1. Back up the test world, and close Minecraft.
2. Unzip into D:\GameOfThronesMod2 (keep directories intact).
3. Open PowerShell at that directory and run:
     powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\RUN_C4_2.ps1
4. If it fails, send C4_2_BUILD_LOG.txt.
5. If it succeeds, create a NEW Known World test world and inspect:
     /gamemode spectator
     /tp @s -3454784 250 -1589008   (Ramsgate)
     /tp @s -4115723 250 -284673    (Acorn Hall)
     /kwc2 sites

ONLY two C4-approved sites. No new sites or people, no household signs,
no changes to Winterfell, no writes to previously generated Minecraft chunks.

This is a street grade/entrance patch, not a fix for all mountain cliffs.
Physical Minecraft inspection is still required.
