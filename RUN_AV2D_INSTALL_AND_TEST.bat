@echo off
setlocal
cd /d "%~dp0"
py -3 "%~dp0tools\av2d\av2d_guarded_install.py" --install
if errorlevel 1 exit /b 1
endlocal
