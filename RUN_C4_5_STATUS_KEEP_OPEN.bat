@echo off
setlocal EnableExtensions
cd /d "%~dp0"
echo Checking the installed C4.5 files WITHOUT modifying them...
echo.
set "LOG=%~dp0C4_5_INSTALL_AUDIT.log"
where py >nul 2>&1
if not errorlevel 1 (
    py -3 tools\geodata\check_c4_5_install.py > "%LOG%" 2>&1
) else (
    python tools\geodata\check_c4_5_install.py > "%LOG%" 2>&1
)
set "RC=%ERRORLEVEL%"
type "%LOG%"
echo.
echo Audit finished with EXIT CODE %RC%.
echo Saved log: %LOG%
echo This did NOT rerun the C4.5 installation and did NOT edit saves.
pause
exit /b %RC%
