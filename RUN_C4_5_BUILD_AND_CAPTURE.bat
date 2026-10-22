@echo off
setlocal EnableExtensions
cd /d "%~dp0"
if not exist gradlew.bat (
  echo ERROR: Extract this package into the mod repository root, beside gradlew.bat.
  pause
  exit /b 1
)
echo Checking Gradle compilation. This does NOT regenerate locations or alter saved worlds.
echo Building and saving the complete output to C4_5_BUILD_CHECK.log...
set "LOG=%~dp0C4_5_BUILD_CHECK.log"
call gradlew.bat build --console=plain --stacktrace > "%LOG%" 2>&1
set "RC=%ERRORLEVEL%"
echo.
if %RC% EQU 0 (
  echo BUILD SUCCESSFUL: Java code and Gradle tasks completed.
) else (
  echo BUILD FAILED with code %RC%.
)
echo.
echo Last 70 lines from %LOG%:
powershell -NoProfile -Command "Get-Content -LiteralPath '%LOG%' -Tail 70" 
echo.
echo Full log: %LOG%
echo Please send this log if the build failed.
pause
exit /b %RC%
