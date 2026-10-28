@echo off
setlocal
cd /d "%~dp0"
echo =============================================================
echo B4.1A: WORLDWIDE BOUNDED NPC PHYSICAL PROJECTION + INSPECTION
echo Existing B4.0 3,760,684 identities remain unchanged.
echo Winterfell V3 untouched. Max 128 additional nearby projections.
echo This batch DOES NOT add worldwide LLM or offscreen daily routines.
echo =============================================================
py -3 -m unittest discover -s src\test\python -p test_b4_1a_*.py -v
if errorlevel 1 goto :failed
py -3 tools\geodata\b4_1a_install.py --check
if errorlevel 1 goto :failed
echo.
echo === Compile projection candidates from installed B4.0 + C4.11 ===
py -3 tools\geodata\b4_1a_projection_index.py
if errorlevel 1 goto :failed
echo.
echo === Patch Java registration and read-only placement accessor ===
py -3 tools\geodata\b4_1a_install.py
if errorlevel 1 goto :failed
echo.
echo B4.1A resources and hooks installed. Gradle CLEAN BUILD then runClient.
echo Use /kwactors status, /kwactors nearby, right-click B4.1A villagers.
pause
exit /b 0
:failed
echo.
echo B4.1A INSTALL FAILED. Do not assume that Minecraft activation is enabled.
pause
exit /b 1
