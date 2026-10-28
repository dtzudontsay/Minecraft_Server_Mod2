@echo off
cd /d "%~dp0"
py -3 tools\geodata\b4_2b4_guarded_install.py --rollback latest
if errorlevel 1 (
 echo ROLLBACK STOPPED. Do not delete manual backups.
 pause
 exit /b 1
)
echo Managed files restored; run .\gradlew.bat clean build
