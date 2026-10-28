@echo off
setlocal
cd /d "%~dp0"
echo =============================================================
echo C4.17 SAFE B41 LINE-ENDING COMPATIBILITY REPAIR
 echo Live project Java and saved NPC data are NOT modified.
echo =============================================================
py -3 -m unittest discover -s src\test\patches -p test_c4_17_line_endings_repair.py -v
if errorlevel 1 goto failed
py -3 tools\geodata\c4_17_fix_local_line_endings.py --check
if errorlevel 1 goto failed
py -3 tools\geodata\c4_17_fix_local_line_endings.py
if errorlevel 1 goto failed
echo.
echo Verifying original C4.17 compatibility status...
call RUN_C4_17_CHECK_ONLY.bat
if errorlevel 1 goto failed
echo.
echo SAFE REPAIR SUCCESS. Next run:
echo   .\RUN_C4_17_DENSE_TOWNS_AND_PHYSICAL_DAILY_LIFE.bat
exit /b 0
:failed
echo C4.17 SAFE STOP. Nothing has been forced into live Java.
exit /b 1
