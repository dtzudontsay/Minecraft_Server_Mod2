# One visible installer/validator for C4. No other batch files needed.
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$log = Join-Path $PSScriptRoot 'C4_INSTALL_LOG.txt'
$started = $false
$exitCode = 0
function Run-C4Step([string] $label, [string] $exe, [string[]] $argv) {
    Write-Host "`n[C4] $label" -ForegroundColor Cyan
    & $exe @argv
    if ($LASTEXITCODE -ne 0) { throw "Failed: $label (exit code $LASTEXITCODE)" }
}
try {
    Start-Transcript -Path $log -Force | Out-Null
    $started = $true
    Run-C4Step 'Check project files, pilot identities and hooks' 'py' @('tools/geodata/check_c4_project.py')
    Run-C4Step 'Run C4 layout regression suite' 'py' @('-m','unittest','discover','-s','tools/geodata','-p','test_c4_settlement_layouts.py','-v')
    Run-C4Step 'Generate C4 construction manifest' 'py' @('tools/geodata/build_c4_settlement_layouts.py')
    Run-C4Step 'Check generated resource is deterministic' 'py' @('tools/geodata/build_c4_settlement_layouts.py','--check')
    Run-C4Step 'Connect C4 resource and farmland painter to C2' 'py' @('tools/geodata/install_c4_settlement_layouts.py')
    Run-C4Step 'Check integration is installed once' 'py' @('tools/geodata/install_c4_settlement_layouts.py','--check')
    Run-C4Step 'Verify live construction wiring and show two exact anchors' 'py' @('tools/geodata/check_c4_project.py','--after')
    Run-C4Step 'Build and test Fabric mod' '.\gradlew.bat' @('clean','build','--console=plain')
    Write-Host "`nC4 SUCCESS. Use a NEW Known World TEST WORLD for first physical inspection." -ForegroundColor Green
    Write-Host 'Run /kwc2 sites in-game, then spectator-teleport to the coordinates printed above.'
} catch {
    $exitCode = 1
    Write-Host "`nC4 FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host 'Nothing was modified in your existing Minecraft world.'
} finally {
    Write-Host "Full log: $log"
    if ($started) { Stop-Transcript | Out-Null }
}
exit $exitCode
