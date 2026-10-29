@echo off
setlocal
cd /d "%~dp0"
py -3 tools\av2\av2_guarded_install.py --check-only
if errorlevel 1 exit /b 1
exit /b 0
