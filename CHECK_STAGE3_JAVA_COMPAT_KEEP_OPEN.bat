@echo off
setlocal
cd /d "%~dp0"
echo Checking C4.5.1 Java patch compatibility (READ ONLY)...
py -3 tools\geodata\c4_5_patch_java.py --npc-visual-stress
set "STATUS=%errorlevel%"
echo.
if "%STATUS%"=="0" (
  echo PASS: Stage 3 should now succeed in the main installer.
) else (
  echo FAIL: Copy the error above and send it to ChatGPT.
)
echo Exit code: %STATUS%
pause
