C4.5.1 STAGE 3 COMPATIBILITY HOTFIX
===================================

This ZIP contains a one-file replacement:
  tools\geodata\c4_5_patch_java.py

WHAT HAPPENED
The original exact-text patcher expected 29 leading spaces in two lines of
WinterfellWorldgenV3.java, whereas the repository source uses 28. This causes
Stage 3 (Read-only Java patch compatibility) to stop with exit code 1.

FIX / INSTALLATION
1. Do not delete your Minecraft saves or reset the repository.
2. Extract this ZIP into D:\GameOfThronesMod2 (the same folder as gradlew.bat).
   Allow it to REPLACE tools\geodata\c4_5_patch_java.py.
3. Re-run RUN_C4_5_1_KEEP_OPEN.bat, choose PILOTS and enter NEW_WORLD.
4. It is fine that Stage 1 ran previously: its script checks whether the test
   has already been updated and does not apply the change twice.
5. Continue only if ALL TEN stages complete and Gradle prints BUILD SUCCESSFUL.
   A fresh Minecraft world is required to inspect new worldgen buildings.

IF THE INSTALLER STOPS AGAIN
Open D:\GameOfThronesMod2\C4_5_1_INSTALL.log and send the lines
surrounding the last 'STOPPED IN STAGE...' / Traceback / ValueError / ERROR.
The installer appends to the log, so read from the final C4.5.1 run heading.

STATUS / SCOPE
This repair only fixes source-code matching in the installer. It does not
modify Minecraft world saves, add buildings by itself, or establish that the
later Gradle build and in-game test will pass.

PATCH CHECKED
Python syntax compiled. The patch functions were tested on representative
unmodified code snippets for C2/C3/Winterfell V3/NPC and checked for
idempotence. Not run against your local D: drive; that happens during install.
