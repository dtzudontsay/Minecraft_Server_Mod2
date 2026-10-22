@echo off
setlocal EnableExtensions
cd /d "%~dp0"
echo.
echo C4.5.1 - VERIFIED STAGED REPAIR ^& WINTERFELL CASTLE WARD RELOCATION
echo Repo: %CD%
echo ------------------------------------------------------------
echo PILOTS: Ramsgate, Acorn Hall, Winterfell (recommended first)
echo WORLD : normal SMALL and MEDIUM places + Winterfell; substantially heavier
echo NOTE  : Existing saves and chunks are NOT modified. Create a new world.
echo.
if not exist "gradlew.bat" (
  echo ERROR: ZIP must be extracted beside gradlew.bat
  goto done
)
set /p SCOPE=Type PILOTS or WORLD: 
if /I "%SCOPE%"=="PILOTS" set "SCOPE=PILOTS"
if /I "%SCOPE%"=="WORLD" set "SCOPE=WORLD"
if not "%SCOPE%"=="PILOTS" if not "%SCOPE%"=="WORLD" (
  echo Cancelled: no valid mode
  goto done
)
set /p CONFIRM=Type NEW_WORLD to confirm fresh-world source changes: 
if not "%CONFIRM%"=="NEW_WORLD" (
  echo Cancelled: confirmation missing
  goto done
)
set "KNOWNWORLD_C45_CONFIRM=NEW_WORLD"
echo.
py -3 tools\geodata\c4_5_1_install.py --scope %SCOPE%
set "EXIT=%errorlevel%"
echo.
if "%EXIT%"=="0" (
 echo SUCCESS: new-world generation resources installed and Gradle build passed.
) else (
 echo FAILURE code %EXIT%. Check C4_5_1_INSTALL.log before running Minecraft.
)
:done
echo.
echo This window will remain open for review.
pause
