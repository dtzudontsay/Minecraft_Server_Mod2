@echo off
cd /d "%~dp0"
py -3 tools\geodata\b4_2b2_guarded_install.py --rollback latest
if errorlevel 1 (echo ROLLBACK FAILED - inspect snapshot.&pause&exit /b 1)
echo Run .\gradlew.bat clean build after manual source rollback.
