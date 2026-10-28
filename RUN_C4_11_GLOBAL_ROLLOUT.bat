@echo off
setlocal
cd /d "%~dp0"
echo ===============================================================
echo C4.11: ENABLE GLOBAL SETTLEMENT CONSTRUCTION, NOT A PREVIEW
 echo Source: exact C4.9 248-site census + C4.10A universal layouts.
echo Constructs in NEW CHUNKS ONLY. Existing saves do not retrofit.
echo Winterfell + existing C2 sites preserved, not duplicated.
echo NO NPCs ARE SPAWNED by this construction batch.
echo ===============================================================
py -3 -m unittest discover -s src\test\python -p test_c4_11_global.py -v
if errorlevel 1 goto fail
echo.
echo === Audit full 248-site scope and demand, no worldgen yet ===
py -3 tools\geodata\c4_11_global_rollout.py
if errorlevel 1 goto fail
echo.
echo === Install worldwide Minecraft generation hooks ===
py -3 tools\geodata\c4_11_install_hooks.py --install
if errorlevel 1 goto fail
echo.
echo === Generate and ACTIVATE ALL eligible worldwide tile resources ===
py -3 tools\geodata\c4_11_global_rollout.py --install
if errorlevel 1 goto fail
echo.
echo C4.11 GLOBAL CONSTRUCTION RESOURCE IS ACTIVE.
echo Rebuild the mod with your usual Gradle command and open a fresh world.
echo Log markers: C4.11 SITE APPROVED, SITE FAILURE, LOT FAILURE.
echo Check tools\geodata\output\c4_11_global\C4_11_WORLDWIDE_CONSTRUCTION_AUDIT.json
exit /b 0
:fail
echo C4.11 stopped. Inspect errors above. Do not use a partial install.
pause
exit /b 1
