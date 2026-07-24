# IB Trader — cross-platform task runner (Windows PowerShell)
# Mirrors Makefile targets. Usage: .\make.ps1 build

param(
    [Parameter(Position = 0)]
    [string]$Target = "help"
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$Mvn = Join-Path $Root "mvnw.cmd"
$Jar = Join-Path $Root "bootstrap\target\ib-trader.jar"

function Show-Help {
    Write-Host ""
    Write-Host "Available commands:"
    Write-Host "  make build    - Build the fat JAR"
    Write-Host "  make run      - Run the application"
    Write-Host "  make dev      - Build and run"
    Write-Host "  make test     - Run tests"
    Write-Host "  make clean    - Clean build outputs"
    Write-Host "  make frontend - Start Angular dev server"
    Write-Host ""
}

function Invoke-Mvn {
    param([string[]]$MvnArgs)
    & $Mvn @MvnArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

function Invoke-Build {
    Invoke-Mvn @('clean', 'package', '-pl', 'bootstrap', '-am', '-DskipTests', '-q')
}

function Invoke-Run {
    if (-not (Test-Path $Jar)) {
        Write-Error "JAR not found at $Jar. Run 'make build' first."
    }
    java -jar $Jar
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

switch ($Target.ToLower()) {
    "build" { Invoke-Build }
    "run" { Invoke-Run }
    "dev" {
        Invoke-Build
        Invoke-Run
    }
    "test" { Invoke-Mvn @('test', '-pl', 'bootstrap', '-am') }
    "clean" { Invoke-Mvn @('clean', '-q') }
    "frontend" {
        Push-Location (Join-Path $Root "frontend")
        try {
            npm install
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
            npm start
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        }
        finally {
            Pop-Location
        }
    }
    "help" { Show-Help }
    default {
        Write-Error "Unknown target '$Target'. Run 'make help' for available commands."
    }
}
