@echo off
setlocal
cd /d "%~dp0"
set "LOG=C4_5_3_CALLBACK_FIX_BUILD.log"
> "%LOG%" echo C4.5.3 Fabric chunk-load callback fix, repository: %CD%
echo.
echo C4.5.3 - Correct Fabric chunk-load event callback
 echo One Java source line only; original backed up; no NPC/building regeneration.
echo.
echo [1/3] Read-only compatibility check...
py -3 "tools\geodata\c4_5_3_fix_fabric_chunk_callback.py" --check >> "%LOG%" 2>&1
if errorlevel 1 goto :failed
echo [2/3] Apply minimal source correction...
py -3 "tools\geodata\c4_5_3_fix_fabric_chunk_callback.py" --install >> "%LOG%" 2>&1
if errorlevel 1 goto :failed
echo [3/3] Compile and test with Gradle...
call gradlew.bat build >> "%LOG%" 2>&1
if errorlevel 1 goto :failed
echo.
echo BUILD SUCCESSFUL. You can now run gradlew.bat runClient and create a fresh test world.
goto :done
:failed
echo.
echo FAILED. Showing end of %LOG%:
powershell -NoProfile -Command "Get-Content -LiteralPath '%LOG%' -Tail 60"
echo.
echo Full log: %CD%\%LOG%
:done
echo.
pause
endlocal
