@echo off
setlocal
cd /d "%~dp0"
echo =============================================================
echo B4.0 POPULATION COMMAND DIAGNOSTIC HOTFIX (no regeneration)
echo =============================================================
echo Checking generated manifest + all populated site resource headers...
py -3 tools\geodata\b4_0_validate_runtime.py
if errorlevel 1 goto :failed
echo.
echo Checking diagnostic hotfix files...
py -3 -m unittest discover -s src\test\python -p test_b4_0_command_diagnostics.py -v
if errorlevel 1 goto :failed
echo.
echo B4.0 diagnostic hotfix checks OK.
echo Now run Gradle clean build and restart Minecraft.
echo In game: /kwpopulation diagnose
pause
exit /b 0
:failed
echo B4.0 diagnostics detected invalid or missing population resources.
echo Copy the first ERROR shown above; do NOT regenerate blindly.
pause
exit /b 1
