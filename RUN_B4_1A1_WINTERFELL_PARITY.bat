@echo off
setlocal
cd /d "%~dp0"
echo =============================================================
echo B4.1A.1: WORLDWIDE HOUSEHOLDS / NAMES / MOVEMENT / PROFILES
echo =============================================================
echo READS existing B4.0 people and C4.11 construction tiles.
echo Does NOT regenerate original people, modify Minecraft saves,
echo overwrite Winterfell V3, add worldwide LLM, or write GitHub.
echo The previous B4.1A registration and C4.11 placement hook stay active.
echo =============================================================
py -3 -m unittest discover -s src\test\python -p "test_b4_1a1_*.py" -v
if errorlevel 1 goto :error
echo.
echo === Verify B4.1A registration + exact census ===
py -3 tools\geodata\b4_1a1_install.py
if errorlevel 1 goto :error
echo.
echo === Build ALL eligible worldwide residents into 512-block tiles ===
py -3 tools\geodata\b4_1a1_household_index.py
if errorlevel 1 goto :error
echo.
echo B4.1A.1 RESOURCE GENERATION COMPLETE.
echo Run .\gradlew.bat clean build and runClient.
echo In game: /kwactors status , /kwactors nearby , right-click visible actors.
echo K-key worldwide conversation is B4.1B; do NOT assume it works yet.
pause
exit /b 0
:error
echo B4.1A.1 FAILED. See the first Python error above.
echo If compilation is attempted without complete resources, NPC projection may be disabled.
pause
exit /b 1
