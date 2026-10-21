# Single C4.2 runner; works in an existing PowerShell window and keeps its log.
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$log = Join-Path $PSScriptRoot 'C4_2_BUILD_LOG.txt'
$started = $false
$exitCode = 0
function Run-Step([string]$label, [string]$executable, [string[]]$arguments) {
    Write-Host "`n[C4.2] $label" -ForegroundColor Cyan
    & $executable @arguments
    if ($LASTEXITCODE -ne 0) { throw "Failed: $label (exit code $LASTEXITCODE)" }
}
try {
    Start-Transcript -Path $log -Force | Out-Null
    $started = $true
    Run-Step 'Source and patch tests' 'py' @('-m','unittest','discover','-s','tools/geodata','-p','test_c4_terrain_roads.py','-v')
    Run-Step 'Install guarded road painter' 'py' @('tools/geodata/install_c4_terrain_roads.py')
    Run-Step 'Verify guarded installation' 'py' @('tools/geodata/install_c4_terrain_roads.py','--check')
    Run-Step 'Full Fabric build' '.\gradlew.bat' @('clean','build','--console=plain')
    Write-Host 'C4.2 BUILD SUCCESSFUL. Inspect fresh Ramsgate and Acorn Hall chunks.' -ForegroundColor Green
} catch {
    $exitCode = 1
    Write-Host "C4.2 FAILED: $($_.Exception.Message)" -ForegroundColor Red
} finally {
    Write-Host "Full log: $log"
    if ($started) { Stop-Transcript | Out-Null }
}
exit $exitCode
