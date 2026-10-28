@echo off
cd /d "%~dp0"
echo =========================================================
echo B4.2B.3 - COMMAND PERMISSION API COMPATIBILITY HOTFIX
echo Only staged Java; no changes to live code or saves
echo =========================================================
py -3 tools\geodata\b4_2b3_permission_fix.py --check
if errorlevel 1 goto error
py -3 -m unittest discover -s src\test\python -p "test_b4_2b3_*.py" -v
if errorlevel 1 goto error
py -3 tools\geodata\b4_2b3_permission_fix.py --apply
if errorlevel 1 goto error
py -3 tools\geodata\b4_2b3_guarded_install.py --check
if errorlevel 1 goto error
echo.
echo FIX APPLIED. Now rerun RUN_B4_2B3_AUTHORITATIVE_OPENING_STOCK.bat
exit /b 0
:error
echo.
echo SAFE STOP. Nothing should be forced; inspect the error above.
pause
exit /b 1
