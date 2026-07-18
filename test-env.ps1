# test-env.ps1
Write-Host "=== Testing Environment Variables ===" -ForegroundColor Cyan

# Load .env file
$envFile = Get-Content .env
foreach ($line in $envFile) {
    if ($line -match '^([^=]+)=(.*)$') {
        $key = $Matches[1].Trim()
        $value = $Matches[2].Trim()
        [Environment]::SetEnvironmentVariable($key, $value)
        Write-Host "$key = $value" -ForegroundColor Gray
    }
}

Write-Host "`n=== Environment Variables Set ===" -ForegroundColor Green
Write-Host "DB_USERNAME: $env:DB_USERNAME" -ForegroundColor Yellow
Write-Host "DB_PASSWORD: $env:DB_PASSWORD" -ForegroundColor Yellow
Write-Host "EMAIL_USERNAME: $env:EMAIL_USERNAME" -ForegroundColor Yellow

# Run the application
Write-Host "`n=== Starting Application ===" -ForegroundColor Cyan
mvn spring-boot:run