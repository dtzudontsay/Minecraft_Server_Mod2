@echo off
setlocal
cd /d "%~dp0"
echo Restoring B4.3A managed sources and projection manifest only.
py -3 tools\geodata\b4_3a_guarded_install.py --rollback latest
if errorlevel 1 pause
