@echo off
setlocal
cd /d "%~dp0"
echo =======================================================
echo B4.0 WORLDWIDE INDIVIDUAL IDENTITY BASELINE, 298 AC
echo Build 3,760,684 sharded individual initial identities.
echo Reuse Winterfell V2; do NOT spawn 3.7 million entities.
echo Import into NpcRegistry / world events is a NEXT BATCH.
echo =======================================================
py -3 -m unittest discover -s src\test\python -p test_b4_0_world_population.py -v
if errorlevel 1 goto :failed
echo.
echo === Generate all 248 site shards and publish to src/main/resources ===
py -3 tools\geodata\b4_0_world_population.py --install
if errorlevel 1 goto :failed
echo.
echo === Register in-game /kwpopulation command ===
py -3 tools\geodata\b4_0_install_hook.py
if errorlevel 1 goto :failed
echo.
echo B4.0 baseline packaged. NEXT: clean build and runClient.
echo Command in game: /kwpopulation status ; /kwpopulation site site_ne_001
pause
exit /b 0
:failed
echo.
echo B4.0 failed. Do NOT assume the full world population was installed.
pause
exit /b 1
