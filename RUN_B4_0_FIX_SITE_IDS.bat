@echo off
setlocal
cd /d "%~dp0"
echo ==========================================================
echo B4.0: REPAIR VALID CANONICAL SITE IDs (PYTHON + JAVA)
echo No population regeneration. No save/worldgen edits.
echo ==========================================================
py -3 tools\geodata\b4_0_fix_site_id_validation.py
if errorlevel 1 goto :failed
echo.
echo [1/3] Run site-ID regression tests...
py -3 -m unittest discover -s src\test\python -p test_b4_0_site_id_repair.py -v
if errorlevel 1 goto :failed
echo.
echo [2/3] Read-only validation of existing B4.0 population shards...
py -3 tools\geodata\b4_0_validate_runtime.py
if errorlevel 1 goto :failed
echo.
echo [3/3] Re-run previous command-diagnostics tests (if installed)...
if not exist src\test\python\test_b4_0_command_diagnostics.py goto :done
py -3 -m unittest discover -s src\test\python -p test_b4_0_command_diagnostics.py -v
if errorlevel 1 goto :failed
:done
echo.
echo SITE-ID FIX INSTALLED. Next: .\gradlew.bat clean build
pause
exit /b 0
:failed
echo.
echo ERROR: Site-ID repair or a diagnostic failed. Look at the first error above.
echo No population regeneration was attempted.
pause
exit /b 1
