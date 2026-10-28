@echo off
cd /d "%~dp0"
py -3 tools\geodata\b4_2b3_guarded_install.py --check
if errorlevel 1 (echo B4.2B.3 CHECK FAILED. Do not force installation. & pause & exit /b 1)
echo READY existing / NEW files mean you may run the guarded installer.
