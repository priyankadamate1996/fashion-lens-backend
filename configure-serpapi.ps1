$ErrorActionPreference = 'Stop'

$secureKey = Read-Host 'Enter the active SerpApi key from Manage API Key' -AsSecureString
$pointer = [IntPtr]::Zero

try {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
    $key = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer).Trim()
    if ([string]::IsNullOrWhiteSpace($key) -or $key -match '\s|["'']') {
        throw 'The key is empty or contains whitespace/quotes. No changes were saved.'
    }

    $uri = 'https://serpapi.com/account.json?api_key=' + [Uri]::EscapeDataString($key)
    try {
        $account = Invoke-RestMethod -Uri $uri -Method Get -TimeoutSec 20
    }
    catch {
        $statusCode = $null
        $providerMessage = $null
        if ($_.Exception.Response) {
            try {
                $statusCode = [int]$_.Exception.Response.StatusCode
                $providerMessage = $_.ErrorDetails.Message
                if ([string]::IsNullOrWhiteSpace($providerMessage)) {
                    $responseStream = $_.Exception.Response.GetResponseStream()
                    if ($responseStream) {
                        $reader = [System.IO.StreamReader]::new($responseStream)
                        $providerMessage = $reader.ReadToEnd()
                        $reader.Dispose()
                    }
                }
            }
            catch {
                $providerMessage = $null
            }
        }

        if ($providerMessage) {
            try {
                $errorBody = $providerMessage | ConvertFrom-Json
                if ($errorBody.error) {
                    $providerMessage = [string]$errorBody.error
                }
            }
            catch {
                $providerMessage = $null
            }
        }

        if ($providerMessage) {
            $providerMessage = $providerMessage.Replace($key, '[redacted]')
            throw "SerpApi returned HTTP $statusCode`: $providerMessage No changes were saved. Check Manage API Key or contact SerpApi support."
        }
        if ($statusCode) {
            throw "SerpApi returned HTTP $statusCode during key verification. No changes were saved. Check Manage API Key or contact SerpApi support."
        }
        throw 'Could not reach SerpApi to verify the key. No changes were saved. Check your internet connection and try again.'
    }

    if ($account.account_status -ne 'Active') {
        throw 'SerpApi did not report an active account for this key. No changes were saved.'
    }

    $envPath = Join-Path $PSScriptRoot '.env'
    [System.IO.File]::WriteAllText(
        $envPath,
        "SERPAPI_KEY=$key$([Environment]::NewLine)",
        [System.Text.Encoding]::ASCII)
    [Environment]::SetEnvironmentVariable('SERPAPI_KEY', $key, 'User')
    Write-Output 'SerpApi accepted the key. It is saved in the Git-ignored backend .env file.'
    Write-Output 'It is also saved as the Windows user SERPAPI_KEY. Restart the backend with .\run-dev.ps1. The account check used no search credit.'
}
finally {
    if ($pointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
    Remove-Variable key, uri, account, secureKey -ErrorAction SilentlyContinue
}
