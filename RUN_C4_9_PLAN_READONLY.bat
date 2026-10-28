@echo off
setlocal
cd /d "%~dp0"
echo === C4.9 demand / household / workplace baseline ===
py -3 tools\geodata\c4_9_settlement_ledgers.py
if errorlevel 1 goto :failed
py -3 -m unittest discover -s src/test/python -p test_c4_9_settlement_ledgers.py -v
if errorlevel 1 goto :failed
echo.
echo Planning data generation complete. No Minecraft worlds or GitHub files were edited by this script.
echo Check tools\geodata\output\c4_9_settlement_ledgers\C4_9_SUMMARY.json
pause
exit /b 0
:failed
echo.
echo C4.9 failed: examine messages above. No construction authorization was changed.
pause
exit /b 1
