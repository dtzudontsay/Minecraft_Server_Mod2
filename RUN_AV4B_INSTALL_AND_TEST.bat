@echo off
setlocal
cd /d "%~dp0"
py -3 tools\av4b\av4b_install.py --install
exit /b %errorlevel%
