@echo off
cd /d "%~dp0"
py -3 tools\geodata\b4_2b3_guarded_install.py --rollback latest
if errorlevel 1 (echo ROLLBACK FAILED - preserve snapshots. & pause & exit /b 1)
echo Source/resource rollback restored. Minecraft saves and grants are NOT rolled back.
