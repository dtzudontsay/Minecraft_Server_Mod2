@echo off
setlocal
cd /d "%~dp0"
py -3 "%~dp0tools\av2c\av2c_guarded_install.py" --check-only
if errorlevel 1 exit /b 1
endlocal
