@echo off
setlocal
cd /d "%~dp0"
echo ========================================================================
echo C4.7.1  ARCHITECTURE COMPILATION FIX ^& FULL CATALOGUE INSTALL
 echo ========================================================================
echo Requires successful C4.6.1 installation. Do not run old installers.
echo No existing world, settlement plan, or NPC save is edited.
echo The previous failed C4.7 run already rolled back safely.
echo.
set /p CONFIRM=Type NEW_WORLD to confirm future-chunk source changes: 
if /I not "%CONFIRM%"=="NEW_WORLD" (
  echo STOP: Confirmation did not match; nothing installed.
  pause
  exit /b 1
)
if not exist "gradlew.bat" (
  echo ERROR: Extract full ZIP into D:\GameOfThronesMod2 beside gradlew.bat
  pause
  exit /b 1
)
if not exist "tools\geodata\templates\KnownWorldC47ArchitecturalDetails.java.txt" (
  echo ERROR: Extract the full ZIP WITH subdirectories; template missing.
  pause
  exit /b 1
)
py tools\geodata\c4_7_run_install.py NEW_WORLD
set CODE=%ERRORLEVEL%
echo.
if "%CODE%"=="0" (
  echo SUCCESS: C4.7.1 all stages completed. See C4_7_1_INSTALL_AND_BUILD.log
) else (
  echo FAILED: See C4_7_1_INSTALL_AND_BUILD.log. Source rollback was attempted.
)
echo This window will stay open. Exit code %CODE%.
pause
exit /b %CODE%
