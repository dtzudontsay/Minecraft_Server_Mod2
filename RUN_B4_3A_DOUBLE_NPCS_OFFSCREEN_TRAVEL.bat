@echo off
setlocal
cd /d "%~dp0"
echo =========================================================
echo B4.3A: WORLDWIDE 256 NPC BUDGET + OFFSCREEN COMMUTING
echo =========================================================
echo Safety: source/manifest snapshots; automatic restore on build failure.
echo Not modified: Minecraft saves, B4.0 population, C4.17 worldgen.
py -3 tools\geodata\b4_3a_guarded_install.py --install
if errorlevel 1 (
 echo B4.3A STOPPED. Check the first error above.
 pause
 exit /b 1
)
echo B4.3A installation and Gradle build succeeded.
