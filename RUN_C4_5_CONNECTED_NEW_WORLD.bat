@echo off
setlocal EnableExtensions
cd /d "%~dp0"
if not exist "gradlew.bat" (
  echo ERROR: Extract the ZIP into the repository root, next to gradlew.bat.
  exit /b 1
)
if not exist "src\main\resources\data\knownworld\scenarios\agot_298_ac\settlement_presets_c1_v1.json" (
  echo ERROR: Missing current C1 source population presets.
  exit /b 1
)
echo.
echo ================================================================
echo Known World C4.5 - JOBS + FRESH-WORLD SETTLEMENT CONSTRUCTION
echo ================================================================
echo PILOTS: Ramsgate + Acorn Hall; Winterfell NPC jobs + new workplaces.
echo WORLD : All standard SMALL/MEDIUM site layouts; Winterfell as above.
echo.
echo Both options preserve 5000 Winterfell identities and do NOT modify saves.
echo Existing chunks do not regenerate. Use a NEW creative test world.
echo WARNING: WORLD makes a MUCH larger chunk index and can require high RAM.
echo.
set /p SCOPE=Type PILOTS or WORLD to choose, or anything else to cancel: 
if /I "%SCOPE%"=="WORLD" (
  set "C45_SCOPE=all-small-medium"
) else if /I "%SCOPE%"=="PILOTS" (
  set "C45_SCOPE=pilots"
) else (
  echo Cancelled with no install.
  exit /b 0
)
set /p CONFIRM=Type NEW_WORLD to confirm fresh-world resources and backups: 
if not "%CONFIRM%"=="NEW_WORLD" (
  echo Cancelled with no install.
  exit /b 0
)
set "KNOWNWORLD_C45_CONFIRM=NEW_WORLD"
echo.
echo Preflight: structural and job-conservation tests...
py -3 tools\geodata\test_c4_5_planner.py
if errorlevel 1 goto fail
echo.
echo Preflight: Java patch anchors matched to repo...
py -3 tools\geodata\c4_5_patch_java.py --npc-visual-stress
if errorlevel 1 goto fail
echo.
echo Generating Winterfell NPC occupational diversity and %C45_SCOPE% settlement plans...
py -3 tools\geodata\c4_5_connected_rollout.py --scope %C45_SCOPE% --winterfell --install
if errorlevel 1 goto fail
echo.
echo Rebuilding Winterfell V3 index with diverse specialty workplaces...
py -3 tools\geodata\c4_5_winterfell_venues.py --install
if errorlevel 1 goto fail
echo.
echo Installing source guards, exterior building signs and visual NPC stress budgets...
py -3 tools\geodata\c4_5_patch_java.py --npc-visual-stress --install
if errorlevel 1 goto fail
echo.
echo Compiling and testing Minecraft 26.3 mod (gradlew build includes compile)...
call gradlew.bat build
if errorlevel 1 goto fail

echo.
echo BUILD SUCCESSFUL. Start Minecraft with gradlew.bat runClient.
echo Create a NEW WORLD - old generated chunks are NEVER rewritten.
echo For scope WORLD, inspect /kworldgen-c2 status if your mod supports it.
exit /b 0
:fail
echo.
echo C4.5 FAILED. Do not launch Minecraft with an uncompiled installation.
echo Source files with changes have *.before_c4_5.bak backups.
echo Errors above or gradle stack trace identify which part needs correction.
exit /b 1
