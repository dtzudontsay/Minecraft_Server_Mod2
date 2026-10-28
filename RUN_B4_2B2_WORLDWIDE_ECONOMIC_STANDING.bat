@echo off
cd /d "%~dp0"
echo ===============================================================
echo B4.2B.2 WORLDWIDE HOUSEHOLD STANDING AND WORKPLACE STAFFING
echo 248 sites / 3.76m people / full guarded source rollback
echo NO crediting coin/grain, NO automatic starvation or taxes
echo ===============================================================
py -3 tools\geodata\b4_2b2_guarded_install.py --install
if errorlevel 1 (
 echo B4.2B.2 FAILED. Check rollback report. Existing saves unaffected.
 pause
 exit /b 1
)
echo B4.2B.2 SUCCESS. Ready for runClient.
