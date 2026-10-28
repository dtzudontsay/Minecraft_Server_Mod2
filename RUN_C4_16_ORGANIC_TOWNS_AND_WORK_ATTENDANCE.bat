@echo off
setlocal
cd /d "%~dp0"
echo ==============================================================
echo C4.16 / B4.2A — ORGANIC SETTLEMENTS AND WORK ATTENDANCE
echo Staged source + FULL BACKUP of current source and resource data
echo Does not edit saves, B4.0 identities, or Winterfell V3.
echo ==============================================================
echo PRECHECK
py -3 tools\geodata\c4_16_stage_apply.py --check
if errorlevel 1 goto :fail
echo.
echo [1/6] Run regression tests before updating sources
py -3 -m unittest discover -s src\test\python -p "test_c4_16_*.py" -v
if errorlevel 1 goto :fail
echo.
echo [2/6] BACK UP source and ALL planned generated resources, install guarded source
py -3 tools\geodata\c4_16_stage_apply.py
if errorlevel 1 goto :fail
echo.
echo [3/6] Preview layout (no Minecraft world modification)
py -3 tools\geodata\c4_10_shared_layout.py --summary-only
if errorlevel 1 goto :fail
echo.
echo [4/6] Rebuild all 248 global settlement tiles and index
py -3 tools\geodata\c4_11_global_rollout.py --install
if errorlevel 1 goto :fail
echo.
echo [5/6] Reselect provisional job locations, reproject unchanged people
py -3 tools\geodata\c4_15_commute_assignment.py
if errorlevel 1 goto :fail
py -3 tools\geodata\b4_1a1_household_index.py
if errorlevel 1 goto :fail
echo.
echo [6/6] Rebuild daily-life planned workplaces
py -3 tools\geodata\b4_2_workplace_index.py
if errorlevel 1 goto :fail
echo.
echo BUILD FROM YOUR PROJECT: .\gradlew.bat clean build
 echo GENERATION CHANGES REQUIRE FRESH CHUNKS / A NEW TEST WORLD.
 echo Existing Known World saves are NOT changed by this batch.
exit /b 0
:fail
echo C4.16 STOPPED. The last source/resource snapshot is under
 echo tools\geodata\output\c4_16_safe_snapshots
 echo Do NOT assume partial regenerated resources are complete.
 pause
exit /b 1
