$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot

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
        throw 'Docker Engine is unavailable. Open Docker Desktop manually before running the full checks. See docs/local-development.md for socket startup errors. No service was started by this script.'
    }
}
finally {
    Pop-Location
}

Push-Location (Join-Path $projectRoot 'backend')
try {
    & .\mvnw.cmd --no-transfer-progress verify
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

Write-Host 'All project checks passed.' -ForegroundColor Green
