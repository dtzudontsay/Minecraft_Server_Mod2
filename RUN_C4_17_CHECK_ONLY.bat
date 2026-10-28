@echo off
setlocal
cd /d "%~dp0"
py -3 tools\geodata\c4_17_guarded_install.py --check
exit /b %errorlevel%
