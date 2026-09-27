param(
    [switch]$SkipDocker
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot

if (-not $SkipDocker) {
    Push-Location $projectRoot
    try {
        & docker.exe compose --env-file .env.example config --quiet
        if ($LASTEXITCODE -ne 0) {
            throw "Docker Compose validation failed with exit code $LASTEXITCODE."
        }

        try {
            & docker.exe info --format 'Docker Engine: {{.ServerVersion}}'
            if ($LASTEXITCODE -ne 0) {
                throw 'Docker Engine did not respond.'
            }
        }
        catch {
            throw 'Docker Engine is unavailable. Open Docker Desktop manually before running the full checks. To run checks that do not require PostgreSQL, use .\scripts\check.ps1 -SkipDocker. No service was started by this script.'
        }
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Host 'Skipping Docker preflight and PostgreSQL/Testcontainers tests by request.' -ForegroundColor Yellow
}

Push-Location (Join-Path $projectRoot 'backend')
try {
    $mavenArguments = @('--no-transfer-progress')
    if ($SkipDocker) {
        $mavenArguments += '-Pwithout-docker'
    }
    $mavenArguments += 'verify'

    & .\mvnw.cmd @mavenArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Backend validation failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

Push-Location (Join-Path $projectRoot 'frontend')
try {
    & npm.cmd run check
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend validation failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

if ($SkipDocker) {
    Write-Host 'All checks that do not require Docker passed.' -ForegroundColor Green
}
else {
    Write-Host 'All project checks passed.' -ForegroundColor Green
}
