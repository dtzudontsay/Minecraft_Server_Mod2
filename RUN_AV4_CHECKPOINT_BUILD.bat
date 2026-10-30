@echo off
setlocal
cd /d "%~dp0"
echo [AV4] Running read-only AV4 source/Java21 preflight...
call RUN_AV4_CHECK_ONLY.bat
if errorlevel 1 goto bad
echo [AV4] Running full Fabric compileJava...
call gradlew.bat compileJava --no-daemon
if errorlevel 1 goto bad
echo [AV4] Running project Gradle unit tests...
call gradlew.bat test --no-daemon
if errorlevel 1 goto bad
echo AV4 BUILD CHECKPOINT PASS. Next: back up test save and run in-game commands in AV4 documentation.
exit /b 0
:bad
echo AV4 BUILD CHECKPOINT FAILED. Stop before attempting in-game verification.
exit /b 1
