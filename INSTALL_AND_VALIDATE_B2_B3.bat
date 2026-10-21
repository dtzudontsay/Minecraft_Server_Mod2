@echo off
setlocal
cd /d "%~dp0"
echo ========================================================
echo  KNOWN WORLD - B2 plus B3 worldwide site registry
echo ========================================================
if not exist "tools\geodata\output\b2_holding_links\markers_253_audit.csv" (
  echo ERROR: Existing B2.2 marker audit missing.
  echo Run: py tools\geodata\build_b2_holding_links.py
  exit /b 1
)
if not exist "tools\geodata\output\b2_3_world_profiles\site_profiles_253.json" (
  echo ERROR: B2.3 research site profiles missing.
  echo Run: py tools\geodata\build_b2_3_world_profiles.py
  exit /b 1
)
if not exist "tools\geodata\output\b2_3_world_profiles\holding_profiles_259.json" (
  echo ERROR: B2.3 research holding profiles missing.
  echo Run: py tools\geodata\build_b2_3_world_profiles.py
  exit /b 1
)
py tools\geodata\test_b2_b3_world_registry.py
if errorlevel 1 exit /b 1
py tools\geodata\build_b2_b3_world_registry.py
if errorlevel 1 exit /b 1
py tools\geodata\build_b2_b3_world_registry.py --check
if errorlevel 1 exit /b 1
call gradlew.bat clean build
if errorlevel 1 exit /b 1
echo.
echo SUCCESS. Open tools\geodata\output\b2_b3_world_registry\KnownWorld_B2_B3_Overview.html
exit /b 0
