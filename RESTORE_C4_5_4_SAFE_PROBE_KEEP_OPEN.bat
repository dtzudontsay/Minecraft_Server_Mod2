@echo off
setlocal
cd /d "%~dp0"
echo Restoring C4.5.3 sign and prior C4.5.1 NPC projection budgets.
echo IMPORTANT: Original sign problem may return if you restore without a later proper fix.
py tools\geodata\c4_5_4_safe_worldgen_probe.py --restore
if errorlevel 1 (echo Restore failed. Files unchanged or partially restored. &pause&exit /b 1)
call gradlew.bat build
pause
