@echo off
setlocal
cd /d "%~dp0"
echo ============================================================
echo B4.2B.1 POSITION-BASED NPC DISCOVERY AND ECONOMIC READINESS
echo Guarded source, population-derived mobility index, rollback
echo Does NOT edit saves, B4.0 identities or EconomyRuntime stocks
echo ============================================================
py -3 tools\geodata\b4_2b_guarded_install.py --install
if errorlevel 1 (
  echo.
  echo B4.2B STOPPED. Existing files are protected; inspect the error above.
  echo Snapshots: tools\geodata\output\b4_2b_safe_snapshots
  pause
  exit /b 1
)
echo B4.2B SOURCE AND GRADLE BUILD SUCCESS.
endlocal
