@echo off
setlocal
cd /d "%~dp0"
py -3 tools\geodata\b4_3a_guarded_install.py --check
if errorlevel 1 (
 echo B4.3A compatibility check FAILED. Do not force or copy stage files.
 pause
 exit /b 1
)
