@echo off
setlocal
cd /d "%~dp0"
echo ========================================================================
echo C4.7  REBUILT ARCHITECTURE ^& INTERIORS - SAME REAL C3 GALLERY
echo ========================================================================
echo This installs a new C3 detail layer; it does NOT regenerate settlements.
echo Requires C4.6.1 to have already built successfully.
echo Uses NEW test worlds to see the new chunk geometry.
echo All previews, source snapshots and Gradle build are automatic.
echo.
set /p CONFIRM=Type NEW_WORLD to confirm future-chunk changes: 
if /I not "%CONFIRM%"=="NEW_WORLD" (
  echo No changes made.
  pause
  exit /b 1
)
if not exist "gradlew.bat" (
  echo ERROR: Extract this ZIP into D:\GameOfThronesMod2 next to gradlew.bat
  pause
  exit /b 1
)
py tools\geodata\c4_7_run_install.py NEW_WORLD
set CODE=%ERRORLEVEL%
echo.
if "%CODE%"=="0" (echo COMPLETE: C4.7 installed and built) else (echo FAILED: review C4_7_INSTALL_AND_BUILD.log for exact cause)
echo This window stays open. Exit code %CODE%.
pause
exit /b %CODE%
