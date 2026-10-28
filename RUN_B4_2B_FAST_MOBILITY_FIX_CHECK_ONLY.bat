@echo off
setlocal
cd /d "%~dp0"
py -3 tools\geodata\b4_2b_fast_mobility_hotfix.py --check
exit /b %errorlevel%
