@echo off
cd /d "%~dp0"
echo ==============================================================
echo B4.2B.4 WORLDWIDE HOUSEHOLD DAILY FINANCE BRIDGE
 echo 248 sites - NO AUTO WAGES, GRAIN CONSUMPTION, OR MONEY MINTING
 echo Guarded backups, resource planning, Java Gradle build, rollback
 echo ==============================================================
py -3 tools\geodata\b4_2b4_guarded_install.py --install
if errorlevel 1 (
 echo B4.2B.4 FAILED; inspect restoration report above.
 pause
 exit /b 1
)
echo B4.2B.4 COMPLETED. Read the README, then run Gradle client.
