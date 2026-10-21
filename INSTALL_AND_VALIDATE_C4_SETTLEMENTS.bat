@echo off
cd /d "%~dp0"
echo [C4] Running a single visible, logged PowerShell pipeline...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0RUN_C4_CHECK.ps1"
set "RC=%ERRORLEVEL%"
echo.
if not "%RC%"=="0" echo C4 FAILED, see C4_INSTALL_LOG.txt
if "%RC%"=="0" echo C4 SUCCESS
if /i not "%1"=="--no-pause" pause
exit /b %RC%
