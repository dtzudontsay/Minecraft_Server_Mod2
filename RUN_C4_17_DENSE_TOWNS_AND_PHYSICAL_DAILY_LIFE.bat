@echo off
setlocal
cd /d "%~dp0"
echo ============================================================
echo C4.17 / B4.2C  SAFE ROLLBACK BUILD + PHYSICAL ROUTINE
 echo Preserves B4.0 people, save games, Winterfell and dialogue.
echo ============================================================
py -3 tools\geodata\c4_17_guarded_install.py --install
if errorlevel 1 (
  echo.
  echo FAILED / ROLLED BACK. Check first error above.
  echo Snapshots: tools\geodata\output\c4_17_safe_snapshots
  pause
  exit /b 1
)
echo.
echo SUCCESS. Build passed. Use a fresh Known World test save.
exit /b 0
