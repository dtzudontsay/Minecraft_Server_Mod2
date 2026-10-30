@echo off
cd /d "%~dp0"
py -3 "QOL_STAGE\tools\qol\install_qol.py" --repo "%CD%" --check-only
if errorlevel 1 exit /b 1
exit /b 0
