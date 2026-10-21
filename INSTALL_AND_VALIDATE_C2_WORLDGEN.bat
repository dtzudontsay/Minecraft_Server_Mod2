@echo off
setlocal
cd /d "%~dp0"
echo [C2] Derive strict worldgen pilots from already generated B2/B3 and C1 resources...
py tools\geodata\build_c2_physical_pilots.py || goto :fail
echo [C2] Install guarded generator hooks + merge Fabric entrypoint with backups...
py tools\geodata\install_c2_worldgen_hooks.py || goto :fail
echo [C2] Verify generated data remains deterministic...
py tools\geodata\build_c2_physical_pilots.py --check || goto :fail
echo [C2] Test pilot planner and safe installation...
py tools\geodata\test_c2_physical_pilots.py || goto :fail
echo [C2] Build Fabric mod...
call gradlew.bat clean build || goto :fail
echo.
echo C2 BUILD PASSED. Start a NEW KnownWorld test world or visit unexplored pilot chunks.
echo In-game: /kwc2 sites , /kwc2 status
exit /b 0
:fail
echo.
echo C2 FAILED: stop and send the complete error output. Backups are under tools\geodata\output\c2_physical_pilots\backups.
exit /b 1
