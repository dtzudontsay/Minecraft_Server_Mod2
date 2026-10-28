@echo off
setlocal
cd /d "%~dp0"
echo Restore ORIGINAL B4.2B mobility-index generator only.
echo This will refuse to roll back if an installed mobility index exists.
py -3 tools\geodata\b4_2b_fast_mobility_hotfix.py --rollback latest
if errorlevel 1 (
  echo Safeguard stopped rollback. See error above.
  pause
  exit /b 1
)
echo Hotfix rollback completed.
endlocal
