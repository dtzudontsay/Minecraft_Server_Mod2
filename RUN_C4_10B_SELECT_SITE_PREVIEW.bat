@echo off
setlocal
cd /d "%~dp0"
echo === C4.10B: explicit site selection; FRESH NEW CHUNKS ONLY ===
echo NEVER choose site_nw_029 (Winterfell), site_nw_031 (Ramsgate), site_sw_023 (Acorn Hall).
echo Eligible example IDs from YOUR installed C4.9 data (not terrain-approved):
py -3 tools\geodata\c4_10b_preview_compiler.py --list
if errorlevel 1 goto fail
 echo Pick one GENERAL_COMPACT_SETTLEMENT Small, Medium, or Major site ID from above.
set /p SITE=Enter one physical site ID (e.g. site_sw_046 is TOO LARGE for this preview): 
if "%SITE%"=="" goto fail
echo First, inspect the plan without activating worldgen:
py -3 tools\geodata\c4_10b_preview_compiler.py --site "%SITE%"
if errorlevel 1 goto fail
echo.
echo READ CAREFULLY: Activate this site only for NEW, NEVER-BEFORE-GENERATED chunks.
set /p CONFIRM=Type ACTIVATE to write the preview manifest: 
if not "%CONFIRM%"=="ACTIVATE" (echo Not activated. & exit /b 0)
py -3 tools\geodata\c4_10b_preview_compiler.py --site "%SITE%" --activate
if errorlevel 1 goto fail
echo MANIFEST ACTIVATED. Run a Gradle build, then test in a BACKUP/NEW Minecraft world.
echo If terrain fails, see your logs\latest.log for C4.10B site and lot failures.
exit /b 0
:fail
echo Site selection failed. NO fresh site activation performed.
pause
exit /b 1
