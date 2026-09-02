$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot

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
