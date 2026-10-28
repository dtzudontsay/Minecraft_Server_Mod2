@echo off
setlocal
cd /d "%~dp0"
echo This removes the opt-in C4.10B preview resource (the hooks stay installed).
if exist "src\main\resources\data\knownworld\scenarios\agot_298_ac\universal_construction_c4_10b_preview_v1.json" (
  del "src\main\resources\data\knownworld\scenarios\agot_298_ac\universal_construction_c4_10b_preview_v1.json"
  if errorlevel 1 exit /b 1
)
echo C4.10B preview is disabled in subsequent builds; existing built chunks unchanged.
exit /b 0
