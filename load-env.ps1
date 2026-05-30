# PowerShell script to load environment variables from .env.production
Write-Host "Loading environment variables from .env.production..." -ForegroundColor Green

# Get the script directory and look for .env.production in parent directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$envFile = Join-Path $scriptDir ".env.production"

if (-not (Test-Path $envFile)) {
    Write-Host "Error: .env.production file not found at: $envFile" -ForegroundColor Red
    exit 1
}

# Read the .env.production file and set environment variables
Get-Content $envFile | ForEach-Object {
    if ($_ -match "^([^#][^=]+)=(.*)$") {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        
        # Remove quotes if present
        $value = $value -replace '^"(.*)"$', '$1'
        $value = $value -replace "^'(.*)'$", '$1'
        
        # Set environment variable
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
        Write-Host "Set $name" -ForegroundColor Yellow
    }
}

Write-Host "Environment variables loaded successfully!" -ForegroundColor Green
Write-Host "You can now run: mvn spring-boot:run" -ForegroundColor Cyan
