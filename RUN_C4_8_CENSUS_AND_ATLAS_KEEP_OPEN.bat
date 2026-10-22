@echo off
setlocal
cd /d "%~dp0"
echo ===============================================================
echo C4.8 WORLDWIDE SITE REFERENCE + ATLAS SETTLEMENT TAGS
 echo ===============================================================
echo No old batch needed. No saves, population counts or world generation changed.
echo Reads the exact C1 and C4.4 resources already in this repository.
echo.
if not exist "gradlew.bat" (echo ERROR: extract alongside gradlew.bat & pause & exit /b 1)
where py >nul 2>&1
if not errorlevel 1 (py -3 tools\geodata\c4_8_run_reference_and_atlas.py) else (python tools\geodata\c4_8_run_reference_and_atlas.py)
if errorlevel 1 goto failed
echo.
echo PASS. Marker tooltip now includes settlement class in the atlas.
echo Reference files in docs\reference\c4_8_worldwide
pause
exit /b 0
:failed
echo.
echo BUILD OR DATA AUDIT FAILED. Java atlas changes, if any, rolled back.
echo Full Gradle output: C4_8_SITE_REFERENCE_AND_ATLAS_BUILD.log
pause
exit /b 1
