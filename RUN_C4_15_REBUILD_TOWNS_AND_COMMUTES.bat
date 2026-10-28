@echo off
setlocal
cd /d "%~dp0"
echo =============================================================
echo C4.15 WORLDWIDE TOWN / CASTLE / STREET / COMMUTE REVISION
echo =============================================================
echo Preserves B4.0 all identities and counts; Winterfell V3 unchanged.
echo Rebuilds C4.11 terrain resources and B4.1A.1 NPC positions.
echo New physical layout appears ONLY in newly generated chunks.
echo Do NOT use an existing explored save to compare old/new streets.
echo =============================================================
echo === Back up and install staged code updates ===
py -3 tools\geodata\c4_15_stage_apply.py
if errorlevel 1 goto :error
echo.
echo === Run C4.15 unit and compatibility tests ===
py -3 -m unittest discover -s src\test\python -p "test_c4_15_*.py" -v
if errorlevel 1 goto :error
echo.
echo === PRECHECK exact hooks, code version, and B4.0 source ===
py -3 tools\geodata\c4_15_local_install.py
if errorlevel 1 goto :error
echo.
echo === Install ordinary-home stone and wood palette ===
py -3 tools\geodata\c4_15_local_install.py --apply
if errorlevel 1 goto :error
echo.
echo === READ ONLY audit complete worldwide demand ===
py -3 tools\geodata\c4_11_global_rollout.py
if errorlevel 1 goto :error
echo.
echo === Rebuild GLOBAL organic street / castle resources ===
py -3 tools\geodata\c4_11_global_rollout.py --install
if errorlevel 1 goto :error
echo.
echo === Reallocate workplace reservations near original homes ===
py -3 tools\geodata\c4_15_commute_assignment.py
if errorlevel 1 goto :error
echo.
echo === Reindex all nearby NPCs from existing B4.0 identities ===
py -3 tools\geodata\b4_1a1_household_index.py
if errorlevel 1 goto :error
echo.
echo C4.15 RESOURCES INSTALLED.
echo Rebuild Minecraft: .\gradlew.bat clean build
 echo Compare using a NEW Known World test world and check commute audit:
echo src\main\resources\data\knownworld\scenarios\agot_298_ac\world_commutes_c4_15_v1\audit.json
exit /b 0
:error
echo C4.15 stopped. Check the first error; do not assume partial resources are safe.
pause
exit /b 1
