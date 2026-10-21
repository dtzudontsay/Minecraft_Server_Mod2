@echo off
setlocal
cd /d "%~dp0"
py -3 tools\geodata\audit_c4_3_jobs_and_settlements.py --check
if errorlevel 1 exit /b %errorlevel%
py -3 tools\geodata\audit_c4_3_jobs_and_settlements.py
if errorlevel 1 exit /b %errorlevel%
echo.
echo C4.3 REVIEW ONLY. No Minecraft world, Java code, or scenario source changed.
echo Inspect tools\geodata\output\c4_3_readonly_audit\
endlocal
