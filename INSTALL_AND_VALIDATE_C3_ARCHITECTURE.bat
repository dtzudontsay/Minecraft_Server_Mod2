@echo off
setlocal
cd /d "%~dp0"
echo [C3] Compile all 43 C1 architectural programs...
py tools\geodata\build_c3_architectural_templates.py || goto :fail
echo [C3] Validate catalog and installer safeguards...
py tools\geodata\test_c3_architectural_templates.py || goto :fail
echo [C3] Connect C3 renderer to the guarded C2 physical pilots...
py tools\geodata\install_c3_architecture.py || goto :fail
echo [C3] Verify deterministic building catalog...
py tools\geodata\build_c3_architectural_templates.py --check || goto :fail
echo [C3] Compile and test entire Fabric mod...
call gradlew.bat clean build || goto :fail
echo.
echo C3 BUILD PASSED. Test /kwc3 status and /kwc3 template BAKERY.
echo Only newly generated C2 pilot chunks receive new physical architecture.
exit /b 0
:fail
echo.
echo C3 FAILED. Send the full terminal error. Backups under tools\geodata\output\c3_architecture\backups.
exit /b 1
