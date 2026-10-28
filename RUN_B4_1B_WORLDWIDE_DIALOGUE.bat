@echo off
setlocal
cd /d "%~dp0"
echo =============================================================
echo B4.1B WORLDWIDE INDIVIDUAL LLM CONVERSATIONS
echo =============================================================
echo This keeps Winterfell V3 and B4.0 population files untouched.
echo It patches 3 Java files with backups, preserving current C4.15.
echo.
where py >nul 2>nul
if errorlevel 1 (
 echo ERROR: Python launcher py not found.
 exit /b 1
)
echo === Prerequisite / source-anchor check ===
py -3 tools\geodata\b4_1b_install.py --check
if errorlevel 1 goto :fail
echo === Regression tests ===
py -3 -m unittest discover -s src\test\python -p "test_b4_1b*.py" -v
if errorlevel 1 goto :fail
echo === Install dialogue packet/actor/screen patch with backups ===
py -3 tools\geodata\b4_1b_install.py
if errorlevel 1 goto :fail
echo === Confirm installation ===
py -3 tools\geodata\b4_1b_install.py --check
if errorlevel 1 goto :fail
echo.
echo B4.1B installed. Run: .\gradlew.bat clean build
exit /b 0
:fail
echo B4.1B FAILED: see first diagnostic above. Do not reset the repo.
exit /b 1
