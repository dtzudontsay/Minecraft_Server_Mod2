@echo off
setlocal
cd /d "%~dp0"
echo === C4.12 atlas teleport repair ===
py -3 tools\install_atlas_teleport_c412.py --check
if errorlevel 1 goto fail
py -3 tools\install_atlas_teleport_c412.py
if errorlevel 1 goto fail
echo.
echo Repair installed. Run your normal Gradle build and restart Minecraft.
exit /b 0
:fail
echo Repair failed. No Minecraft save was modified.
pause
exit /b 1
