@echo off
setlocal
cd /d "%~dp0"
echo CAUTION: restores B4.2B managed Java and generated resources ONLY.
echo Minecraft saves and B4.0 identities are not changed.
py -3 tools\geodata\b4_2b_guarded_install.py --rollback latest
if errorlevel 1 exit /b 1
echo Source/resource rollback complete. Run gradlew.bat clean build before testing.
endlocal
