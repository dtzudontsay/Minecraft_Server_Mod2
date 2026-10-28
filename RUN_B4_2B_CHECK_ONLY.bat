@echo off
setlocal
cd /d "%~dp0"
py -3 tools\geodata\b4_2b_guarded_install.py --check
if errorlevel 1 (
  echo B4.2B CHECK FAILED. No source or resource was changed.
  exit /b 1
)
echo B4.2B check complete. READY old sources and NEW new files/resources are expected.
endlocal
