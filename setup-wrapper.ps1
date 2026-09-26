$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$wrapperDir = Join-Path $root "gradle\wrapper"
$wrapperJar = Join-Path $wrapperDir "gradle-wrapper.jar"

New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null

if (Test-Path $wrapperJar) {
    Write-Host "gradle-wrapper.jar already exists."
    exit 0
}

$url = "https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.3/gradle/wrapper/gradle-wrapper.jar"

Write-Host "Downloading official FabricMC Gradle wrapper..."
Invoke-WebRequest -Uri $url -OutFile $wrapperJar
Write-Host "Downloaded: $wrapperJar"
