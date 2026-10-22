@echo off
setlocal
cd /d "%~dp0"
echo ==== C4.5.4 STABLE WORLDGEN ISOLATION ==== 
echo Disables experimental generated sign processing and restores NPC visual budgets to 128.
echo Keeps all 5000 simulated Winterfell residents and all C4.5 buildings.
echo.
if not exist "gradlew.bat" (echo ERROR: Extract ZIP into repository root alongside gradlew.bat.&pause&exit /b 1)
set LOG=C4_5_4_SAFE_PROBE_BUILD.log
(echo ==== C4.5.4 ==== &echo Repository %CD%&echo Mode INSTALL) > "%LOG%"
py tools\geodata\c4_5_4_safe_worldgen_probe.py --check >> "%LOG%" 2>&1
if errorlevel 1 goto error
py tools\geodata\c4_5_4_safe_worldgen_probe.py --install >> "%LOG%" 2>&1
if errorlevel 1 goto error
call gradlew.bat build >> "%LOG%" 2>&1
if errorlevel 1 goto error
findstr /C:"BUILD SUCCESSFUL" "%LOG%"
if errorlevel 1 goto error
echo.
echo PASS: Safe probe installed and Gradle build successful.
echo Create a BRAND NEW world; render distance 6-8 and DH distant generation disabled.
echo Do not reuse already-generated or crashed chunks for this isolation test.
echo Log: %CD%\%LOG%
pause
exit /b 0
:error
echo FAILED. Last 65 log lines:
powershell -NoProfile -Command "Get-Content -LiteralPath '%CD%\%LOG%' -Tail 65"
echo.
echo Log: %CD%\%LOG%
pause
exit /b 1
