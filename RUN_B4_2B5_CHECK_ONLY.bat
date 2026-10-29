@echo off
setlocal
cd /d "%~dp0"
py -3 "B4_2B5_STAGE\scripts\b4_2b5_guarded_install.py" --check-only
if errorlevel 1 (
  echo B4.2B.5 CHECK FAILED. No source changed.
  exit /b 1
)
echo B4.2B.5 CHECK OK.
endlocal
