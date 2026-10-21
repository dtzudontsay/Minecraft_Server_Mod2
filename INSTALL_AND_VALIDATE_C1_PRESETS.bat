@echo off
setlocal EnableExtensions
cd /d "%~dp0"
echo [C1] Prerequisite: B2/B3 registry must already have been generated.
if not exist "src\main\resources\data\knownworld\scenarios\agot_298_ac\world_sites_b2_b3_v1.json" (
  echo ERROR: Missing world_sites_b2_b3_v1.json. Run the B2/B3 installer first.
  exit /b 1
)
py tools\geodata\install_c1_integration.py
if errorlevel 1 exit /b 1
py tools\geodata\install_c1_integration.py --check
if errorlevel 1 exit /b 1
py tools\geodata\test_c1_settlement_presets.py
if errorlevel 1 exit /b 1
py tools\geodata\build_c1_settlement_presets.py
if errorlevel 1 exit /b 1
py tools\geodata\build_c1_settlement_presets.py --check
if errorlevel 1 exit /b 1
call gradlew.bat clean build
if errorlevel 1 exit /b 1
echo.
echo [C1] Data and Gradle validation passed.
echo [C1] Open tools\geodata\output\c1_settlement_presets\KnownWorld_C1_Preset_Explorer.html
exit /b 0
