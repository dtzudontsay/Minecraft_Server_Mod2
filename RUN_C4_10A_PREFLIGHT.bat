@echo off
setlocal
cd /d "%~dp0"
echo === Known World C4.10A: planning tests ===
py -3 -m unittest discover -s src/test/python -p test_c4_10_shared_layout.py -v
if errorlevel 1 goto error
if not exist "tools\geodata\output\c4_9_settlement_ledgers\C4_9_SITE_CONSTRUCTION_AND_POPULATION_248.json" (
  echo ERROR: C4.9 site ledger missing. Run RUN_C4_9_PLAN_READONLY.bat first.
  goto error
)
echo === Generate worldwide *planning* summaries only ===
py -3 tools\geodata\c4_10_shared_layout.py --summary-only
if errorlevel 1 goto error
echo.
echo Planning results are in tools\geodata\output\c4_10_shared_layout
 echo NO MINECRAFT BLOCKS OR NPCS CHANGED. Complete.
pause
exit /b 0
:error
echo C4.10A PREFLIGHT FAILED: inspect output above. No world generated.
pause
exit /b 1
