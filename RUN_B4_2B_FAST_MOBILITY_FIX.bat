@echo off
setlocal
cd /d "%~dp0"
echo ============================================================
echo B4.2B.1 SAFE MOBILITY INDEX PERFORMANCE REPAIR
echo No edits to Java, population identities or Minecraft saves
echo ============================================================
py -3 tools\geodata\b4_2b_fast_mobility_hotfix.py --check
if errorlevel 1 goto fail
py -3 -m unittest discover -s B4_2B_FAST_PAYLOAD -p "test_b4_2b_fast_mobility.py" -v
if errorlevel 1 goto fail
py -3 -m unittest discover -s B4_2B_FAST_PAYLOAD -p "test_b4_2b_pending_site_ids.py" -v
if errorlevel 1 goto fail
py -3 tools\geodata\b4_2b_fast_mobility_hotfix.py --apply
if errorlevel 1 goto fail
echo.
echo HOTFIX SUCCESS. Rerun the original B4.2B.1 check and installer.
exit /b 0
:fail
echo.
echo HOTFIX STOPPED SAFELY. Send the error output; do not force overwrite.
pause
exit /b 1
