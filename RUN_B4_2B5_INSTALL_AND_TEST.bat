@echo off
setlocal
cd /d "%~dp0"
py -3 "B4_2B5_STAGE\scripts\b4_2b5_guarded_install.py" --install
if errorlevel 1 (
  echo B4.2B.5 INSTALL OR BUILD FAILED. Managed sources restored automatically when needed.
  exit /b 1
)
echo B4.2B.5 INSTALL AND BUILD COMPLETE.
endlocal
