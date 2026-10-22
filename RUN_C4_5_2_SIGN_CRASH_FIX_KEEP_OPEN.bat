@echo off
setlocal
cd /d "%~dp0"
set LOG=C4_5_2_SIGN_CRASH_FIX.log
> "%LOG%" echo C4.5.2 - deferred sign text hotfix, repository: %CD%
echo.
echo C4.5.2 fixes the known NullPointerException in SignBlockEntity.setText during worldgen.
echo It modifies only KnownWorldStructureSigns.java and KnownWorld.java (with backups).
echo It does NOT rerun C4.5.1, regenerate worldgen data or edit saved worlds.
echo.
py -3 "tools\geodata\c4_5_2_fix_sign_crash.py" >> "%LOG%" 2>&1
if errorlevel 1 goto :error
echo Source preflight OK. Installing safe sign handler...
py -3 "tools\geodata\c4_5_2_fix_sign_crash.py" --install >> "%LOG%" 2>&1
if errorlevel 1 goto :error
echo Checking Gradle build and tests...
call gradlew.bat build >> "%LOG%" 2>&1
if errorlevel 1 goto :error
echo.
echo SUCCESS: Gradle build and tests succeeded.
echo Review the log: %CD%\%LOG%
echo Create a NEW test world; the crashed world may contain incomplete chunks.
goto :done
:error
echo.
echo FAILED - do not enter the damaged test world.
echo Open the log %CD%\%LOG% and send the first error / exception section.
:done
pause
endlocal
