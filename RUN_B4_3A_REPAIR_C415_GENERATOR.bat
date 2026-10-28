@echo off
setlocal
cd /d "%~dp0"
echo ==============================================================
echo B4.3A C4.15 GENERATOR COMPATIBILITY - STAGE ONLY
 echo Checks C4.15 commute-aware generator and original B4.3A stage.
echo Does NOT overwrite the live source, saves or B4.0 data.
echo ==============================================================
py -3 tools\geodata\b4_3a_c415_compatibility.py --check
if errorlevel 1 goto failed
py -3 tools\geodata\b4_3a_c415_compatibility.py
if errorlevel 1 goto failed
py -3 tools\geodata\b4_3a_guarded_install.py --check
if errorlevel 1 goto failed
echo SUCCESS. You may now run RUN_B4_3A_DOUBLE_NPCS_OFFSCREEN_TRAVEL.bat
exit /b 0
:failed
echo STOPPED. Do not bypass the original B4.3A safety checks.
exit /b 1
