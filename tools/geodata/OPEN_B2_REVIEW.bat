@echo off
setlocal
cd /d "%~dp0\..\.."
echo.
echo KNOWN WORLD - B2.2 VISUAL LOCATION REVIEW
echo Building a searchable dashboard from the existing local CSV and JSON audit files...
echo.
py "tools\geodata\build_b2_review_dashboard.py"
if errorlevel 1 (
  echo.
  echo ERROR: The dashboard could not be generated. Check the error above.
  pause
  exit /b 1
)
echo.
echo Opening the interactive location review in your default browser...
start "" "%CD%\tools\geodata\output\b2_holding_links\KnownWorld_B2_Location_Review.html"
echo.
echo Review notes are saved in your browser. Use 'Export my reviews' to preserve them.
exit /b 0
