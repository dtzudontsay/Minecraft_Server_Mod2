@echo off
setlocal
cd /d "%~dp0"
py -3 tools\av4_qol3\av4_qol3.py --check
exit /b %errorlevel%
