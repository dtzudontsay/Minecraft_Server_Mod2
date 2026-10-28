@echo off
setlocal
cd /d "%~dp0"
echo === C4.10B: validate generator hooks then install ===
py -3 tools\geodata\c4_10b_hook_install.py --install
if errorlevel 1 goto fail
echo === C4.10B: Python regression tests ===
py -3 -m unittest discover -s src\test\python -p test_c4_10b_preview.py -v
if errorlevel 1 goto fail
echo.
echo Hooks ready; NO SITES ACTIVATED. Select site ID explicitly to make a preview manifest.
exit /b 0
:fail
echo C4.10B installer failed. Inspect error above. No worlds modified.
pause
exit /b 1
