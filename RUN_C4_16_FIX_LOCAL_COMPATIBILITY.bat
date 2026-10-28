@echo off
setlocal
cd /d "%~dp0"
echo =============================================================
echo C4.16 SAFE COMPATIBILITY REPAIR -- NO LIVE JAVA MODIFICATIONS
echo =============================================================
py -3 tools\geodata\c4_16_resolve_local_compat.py --check
if errorlevel 1 goto :fail
py -3 tools\geodata\c4_16_resolve_local_compat.py
if errorlevel 1 goto :fail
echo.
echo Verifying guarded original C4.16 installer precheck...
py -3 tools\geodata\c4_16_stage_apply.py --check
if errorlevel 1 goto :fail
echo.
echo Compatibility repair complete. Run original batch next:
echo   .\RUN_C4_16_ORGANIC_TOWNS_AND_WORK_ATTENDANCE.bat
exit /b 0
:fail
echo SAFE STOP. No C4.16 resource generation occurred in this BAT.
pause
exit /b 1
