@echo off
setlocal
cd /d "%~dp0"
echo === C4.13: precise atlas marker teleport + construction diagnostics ===
py -3 -m unittest discover -s src\test\python -p test_c4_13_worldgen_diagnostics.py -v
if errorlevel 1 goto error
py -3 tools\geodata\install_c4_13_worldgen_diagnostics.py
if errorlevel 1 goto error
echo.
echo SUCCESS. Now Gradle clean build, runClient, and in new test world:
echo /kwconstruction status
echo /kwconstruction anchors
echo /kwinterfell3 status
echo.
pause
exit /b 0
:error
echo C4.13 FAILED. Inspect message; world was not changed.
pause
exit /b 1
