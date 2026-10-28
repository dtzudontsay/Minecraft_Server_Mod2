@echo off
setlocal
cd /d "%~dp0"
echo ==========================================================
echo B4.2 WORLDWIDE DAILY LIFE / EMPLOYMENT FOUNDATION
 echo ==========================================================
echo Read-only identity baseline. Keeps B4.1B and Winterfell V3.
echo.
py -3 -m unittest discover -s src\test\python -p "test_b4_2_*.py" -v
if errorlevel 1 goto failed
echo.
echo === Compile read-only planned workplace coordinates first ===
py -3 tools\geodata\b4_2_workplace_index.py
if errorlevel 1 goto failed
echo.
echo === Validate and patch existing C4.15/B4.1B runtime ===
py -3 tools\geodata\b4_2_install.py
if errorlevel 1 goto failed
echo.
echo ALL B4.2 STEPS COMPLETE. Now run gradlew.bat clean build then runClient.
exit /b 0
:failed
echo B4.2 stopped. Do not rerun B4.0 population genesis.
exit /b 1
