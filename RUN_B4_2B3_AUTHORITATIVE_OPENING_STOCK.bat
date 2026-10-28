@echo off
cd /d "%~dp0"
echo ================================================================
echo B4.2B.3 - OPENING PUBLIC RESERVES AND NATIVE LEDGER BRIDGE
echo Guarded source + world plan - NO AUTOMATIC GRAIN OR COIN CREDIT
echo Does NOT alter Minecraft save files; no activated starvation.
echo ================================================================
py -3 tools\geodata\b4_2b3_guarded_install.py --install
if errorlevel 1 (
 echo B4.2B.3 FAILED. Inspect rollback. Previous sources restored if possible.
 pause
 exit /b 1
)
echo B4.2B.3 SOURCE/RESOURCE BUILD COMPLETE. Read README for in-game audit.
