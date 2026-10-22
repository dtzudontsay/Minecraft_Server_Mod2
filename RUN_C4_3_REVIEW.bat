@echo off
setlocal
cd /d "%~dp0"
if not exist "tools\geodata\build_c4_3_job_distribution.py" (echo ERROR: Extract whole ZIP at repo ROOT.& exit /b 1)
py tools/geodata/build_c4_3_job_distribution.py || exit /b 1
py tools/geodata/extend_c4_3_architecture.py || exit /b 1
py tools/geodata/build_c4_3_winterfell_job_overlay.py || exit /b 1
py tools/geodata/test_c4_3_job_distribution.py || exit /b 1
echo C4.3 REVIEW PASSED. Output in tools\geodata\output\c4_3_jobs\
endlocal
