$ErrorActionPreference = 'Stop'
$originalLocation = Get-Location

$envFile = Join-Path $PSScriptRoot '.env'
$key = $null
$keySource = $null

if (Test-Path -LiteralPath $envFile) {
    $keyLines = @(Get-Content -LiteralPath $envFile | Where-Object { $_ -match '^\s*SERPAPI_KEY\s*=' })
    if ($keyLines.Count -gt 1) {
        throw '.env must contain at most one SERPAPI_KEY assignment.'
    }
    if ($keyLines.Count -eq 1) {
        $key = $keyLines[0] -replace '^\s*SERPAPI_KEY\s*=', ''
        $keySource = 'backend .env'
    }
}
if ($key -eq 'replace-with-your-serpapi-key') {
    $key = $null
}

if ([string]::IsNullOrWhiteSpace($key)) {
    $key = [Environment]::GetEnvironmentVariable('SERPAPI_KEY', 'User')
    $keySource = 'Windows user SERPAPI_KEY'
}
if ([string]::IsNullOrWhiteSpace($key)) {
    $key = [Environment]::GetEnvironmentVariable('SERPAPI_KEY', 'Process')
    $keySource = 'process SERPAPI_KEY'
}

$key = if ($null -eq $key) { '' } else { $key.Trim() }
$invalidKey = [string]::IsNullOrWhiteSpace($key) `
    -or $key -match '\s|["'']' `
    -or $key -eq 'replace-with-your-serpapi-key'
if ($invalidKey) {
    throw 'No usable SerpApi key found. Put a real SERPAPI_KEY in the backend .env file or set it in the Windows user environment.'
}

$previousKey = [Environment]::GetEnvironmentVariable('SERPAPI_KEY', 'Process')
try {
    $env:SERPAPI_KEY = $key
    Set-Location -LiteralPath $PSScriptRoot
    Write-Output "Starting backend with key from $keySource (value hidden)."
    & (Join-Path $PSScriptRoot 'mvnw.cmd') quarkus:dev
    $exitCode = $LASTEXITCODE
}
finally {
    if ($null -eq $previousKey) {
        Remove-Item Env:SERPAPI_KEY -ErrorAction SilentlyContinue
    }
    else {
        $env:SERPAPI_KEY = $previousKey
    }
    Set-Location -LiteralPath $originalLocation
    Remove-Variable key, keyLines, keySource, previousKey -ErrorAction SilentlyContinue
}

exit $exitCode
