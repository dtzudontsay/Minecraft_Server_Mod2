@echo off
setlocal
cd /d "%~dp0"
where py >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  py -3 tools\av1\av1_guarded_install.py --check-only
) else (
  python tools\av1\av1_guarded_install.py --check-only
)
exit /b %ERRORLEVEL%
