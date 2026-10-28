@echo off
cd /d "%~dp0"
py -3 tools\geodata\b4_2b4_guarded_install.py --check
if errorlevel 1 (
 echo B4.2B.4 SAFE STOP. Do not run installer.
 pause
 exit /b 1
)
echo READY existing / NEW additions = installation is compatible.
