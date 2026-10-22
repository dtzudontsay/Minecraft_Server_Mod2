@echo off
setlocal
cd /d "%~dp0"
echo WARNING: This changes C3/C4 source resources and two NEW-WORLD pilot layouts.
echo WARNING: Existing chunks will not be retroactively changed.
echo Winterfell V3 and other 246 physical site presets remain untouched.
set /p CONFIRM=Type STRESS to confirm backup-and-install: 
if /I not "%CONFIRM%"=="STRESS" (echo Cancelled. & exit /b 1)
call RUN_C4_3_REVIEW.bat || exit /b 1
py tools/geodata/extend_c4_3_architecture.py --apply-resource || exit /b 1
py tools/geodata/patch_c4_3_java_guards.py || exit /b 1
py tools/geodata/build_c4_3_full_pilot_stress.py || exit /b 1
py tools/geodata/build_c4_3_full_pilot_stress.py --install-resource || exit /b 1
call gradlew.bat build || exit /b 1
echo Build finished. Inspect two pilots in NEW Minecraft world only.
endlocal
