@echo off
setlocal
cd /d "%~dp0"
set "INDEX=src\main\resources\data\knownworld\scenarios\agot_298_ac\universal_construction_c4_11_global_v1.json"
if not exist "%INDEX%" (echo Global index absent: already inactive. & exit /b 0)
set /p CONFIRM=Type DISABLE to turn off NEW-CHUNK global generation: 
if not "%CONFIRM%"=="DISABLE" exit /b 1
ren "%INDEX%" universal_construction_c4_11_global_v1.json.disabled
if errorlevel 1 (echo Rename failed. & exit /b 1)
echo Disabled new-chunk generation. Rebuild mod. Existing placed blocks remain.
exit /b 0
