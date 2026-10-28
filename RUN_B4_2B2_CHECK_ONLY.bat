@echo off
cd /d "%~dp0"
py -3 tools\geodata\b4_2b2_guarded_install.py --check
if errorlevel 1 (echo CHECK FAILED. Do not run installer.&exit /b 1)
echo READY for existing Java; NEW for new Java and resource are expected.
