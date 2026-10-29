@echo off
setlocal
cd /d "%~dp0"
py -3 "%~dp0tools\av2b\av2b_guarded_install.py" --check-only
exit /b %errorlevel%
