@echo off
setlocal
cd /d "%~dp0"
echo ========================================================================
echo C4.6 WINTERFELL RINGS + ALL C3 BLUEPRINT GALLERY + BOUNDED SIGNS
echo ========================================================================
echo IMPORTANT: No old batch files should be re-run. This INSTALLER alone
 echo verifies Python, installs changes with backups, then invokes Gradle build.
echo This changes FUTURE CHUNK GENERATION. Use brand NEW test worlds.
echo.
if not exist "gradlew.bat" (echo ERROR: Extract ZIP in repository root! & pause & exit /b 1)
set /p CONFIRM=Type NEW_WORLD to prepare fresh-world mod changes: 
if /I not "%CONFIRM%"=="NEW_WORLD" (echo Cancelled. & pause & exit /b 1)
set KNOWNWORLD_C46_CONFIRM=NEW_WORLD
set LOG=C4_6_INSTALL_AND_BUILD.log
echo C4.6 start %DATE% %TIME% > "%LOG%"
echo Running all stages and capturing full log to %LOG% ...
py tools\geodata\c4_6_install.py >> "%LOG%" 2>&1
if errorlevel 1 goto fail
findstr /C:"COMPLETE: Gradle passed" "%LOG%"
if errorlevel 1 goto fail
echo.
echo SUCCESS. C4.6 installed and compiled. Create NEW worlds.
echo Gallery: choose superflat world, operator/cheats on, then /kwgallery build
echo Full build log: %CD%\%LOG%
pause
exit /b 0
:fail
echo.
echo INSTALL FAILED. Changes to 5 original files rolled back if install began.
echo Last 85 log lines:
powershell -NoProfile -Command "Get-Content -LiteralPath '%CD%\%LOG%' -Tail 85"
echo.
echo Full log: %CD%\%LOG%
pause
exit /b 1
