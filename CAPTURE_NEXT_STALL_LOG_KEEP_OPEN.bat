@echo off
setlocal
cd /d "%~dp0"
if not exist "run\logs\latest.log" (echo No run\logs\latest.log found.&pause&exit /b 1)
powershell -NoProfile -Command "$src='run\logs\latest.log'; $dst='C4_5_4_LAST_STALL_LOG.txt'; Get-Content -LiteralPath $src -Tail 500 | Set-Content -LiteralPath $dst -Encoding utf8; Write-Host ('Saved last 500 lines to '+$dst); Write-Host 'Look for ERROR, Exception, OutOfMemoryError, and Watchdog.'"
if exist "run\crash-reports" (echo Crash reports are in run\crash-reports. Upload the latest if present.)
pause
