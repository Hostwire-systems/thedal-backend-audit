<#!
Usage:
  pwsh -File ./start-reporting.ps1
  (Or) ./start-reporting.ps1

What it does:
  1. Loads staging env from .env.staging via existing load-env.ps1 (reporting DB / target DB)
  2. Sets SPRING_PROFILES_ACTIVE=reporting to enable secondary source datasource
  3. Optionally loads a separate source DB credentials file .env.primary-source (NOT committed) with keys:
       PRIMARY_DATASOURCE_URL=jdbc:postgresql://host:5432/source_db
       PRIMARY_DATASOURCE_USERNAME=...
       PRIMARY_DATASOURCE_PASSWORD=...
       # (optional) PRIMARY_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
  4. Launches mvn spring-boot:run

Place your source DB creds in .env.primary-source (same folder) so you don't have to export each time.

Safety:
  Do NOT commit .env.primary-source if it contains secrets.
#>

param(
  [ValidateSet('full','reporting')]
  [string]$Mode = 'full',    # 'full' = normal platform app, 'reporting' = shadow reporting-only launcher
  [switch]$SkipBuild,
  [string]$MavenGoals = 'spring-boot:run'
)

$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

Write-Host "=== THEDAL Reporting Startup (staging + reporting profile) ===" -ForegroundColor Cyan

# 1. Load staging / target DB + app credentials
if (Test-Path "$scriptDir/load-env.ps1") {
  Write-Host "Loading staging .env.staging (target/reporting DB)" -ForegroundColor Green
    . "$scriptDir/load-env.ps1"
} else {
  Write-Host "ERROR: load-env.ps1 not found" -ForegroundColor Red
  exit 1
}

# 2. Activate profiles
if ($Mode -eq 'reporting') {
  # Include staging overlay so application-reporting-staging.yml merges
  $env:SPRING_PROFILES_ACTIVE = 'reporting,staging'
  Write-Host "Set SPRING_PROFILES_ACTIVE=reporting,staging" -ForegroundColor Yellow
  # Preflight: ensure at least one of REPORTING_DB_URL or DB_URL present
  if (-not $env:REPORTING_DB_URL -and -not $env:DB_URL) {
    Write-Host "ERROR: Neither REPORTING_DB_URL nor DB_URL is set. Cannot start reporting instance." -ForegroundColor Red
    exit 2
  }
  # Normalize blank reporting URL to null so spring fallback works
  if ($env:REPORTING_DB_URL -and $env:REPORTING_DB_URL.Trim() -eq '') { $env:REPORTING_DB_URL=$null }
} else {
  # Full application does NOT use reporting profile
  $env:SPRING_PROFILES_ACTIVE = 'staging'
  Write-Host "Set SPRING_PROFILES_ACTIVE=staging" -ForegroundColor Yellow
}

# 3a. Load reporting DB override (target / Neon) if present
$reportingFile = Join-Path $scriptDir '.env.reporting'
if (Test-Path $reportingFile) {
  Write-Host "Loading reporting (Neon) DB creds from .env.reporting" -ForegroundColor Green
  Get-Content $reportingFile | ForEach-Object {
    if ($_ -match "^([^#][^=]+)=(.*)$") {
      $name = $matches[1].Trim()
      $value = $matches[2].Trim()
      $value = $value -replace '^"(.*)"$', '$1'
      $value = $value -replace "^'(.*)'$", '$1'
      [Environment]::SetEnvironmentVariable($name, $value, 'Process')
      Write-Host "Set $name" -ForegroundColor DarkCyan
    }
  }
} else {
  if (-not $env:REPORTING_DB_URL) {
    Write-Host "No .env.reporting found; reporting DB falling back to DB_URL (NOT Neon)." -ForegroundColor Yellow
  }
}

# 3b. Load primary/source (operational) DB creds if file present
$primaryFile = Join-Path $scriptDir '.env.primary-source'
if (Test-Path $primaryFile) {
  Write-Host "Loading primary source (operational) DB creds from .env.primary-source" -ForegroundColor Green
  Get-Content $primaryFile | ForEach-Object {
    if ($_ -match "^([^#][^=]+)=(.*)$") {
      $name = $matches[1].Trim()
      $value = $matches[2].Trim()
      $value = $value -replace '^"(.*)"$', '$1'
      $value = $value -replace "^'(.*)'$", '$1'
      [Environment]::SetEnvironmentVariable($name, $value, 'Process')
      Write-Host "Set $name" -ForegroundColor DarkYellow
    }
  }
} else {
  Write-Host "No .env.primary-source file found. Auto-mapping staging DB vars as PRIMARY_DB_* (dual-use)." -ForegroundColor Yellow
  if (-not $env:PRIMARY_DB_URL -and $env:DB_URL) { $env:PRIMARY_DB_URL = $env:DB_URL }
  if (-not $env:PRIMARY_DB_USERNAME -and $env:DB_USERNAME) { $env:PRIMARY_DB_USERNAME = $env:DB_USERNAME }
  if (-not $env:PRIMARY_DB_PASSWORD -and $env:DB_PASSWORD) { $env:PRIMARY_DB_PASSWORD = $env:DB_PASSWORD }
  # Backward compatibility for earlier variable names
  if (-not $env:PRIMARY_DATASOURCE_URL -and $env:PRIMARY_DB_URL) { $env:PRIMARY_DATASOURCE_URL = $env:PRIMARY_DB_URL }
  if (-not $env:PRIMARY_DATASOURCE_USERNAME -and $env:PRIMARY_DB_USERNAME) { $env:PRIMARY_DATASOURCE_USERNAME = $env:PRIMARY_DB_USERNAME }
  if (-not $env:PRIMARY_DATASOURCE_PASSWORD -and $env:PRIMARY_DB_PASSWORD) { $env:PRIMARY_DATASOURCE_PASSWORD = $env:PRIMARY_DB_PASSWORD }
}

# 4. Show key variables (masked)
function Mask($s) { if (!$s) { return '<unset>' } elseif ($s.Length -le 6) { return '***' } else { return $s.Substring(0,3) + '***' + $s.Substring($s.Length-2) } }
$reportingUrl = if ($env:REPORTING_DB_URL -and $env:REPORTING_DB_URL.Trim() -ne '') { $env:REPORTING_DB_URL } else { $env:DB_URL }
$primaryUrl = if ($env:PRIMARY_DB_URL -and $env:PRIMARY_DB_URL.Trim() -ne '') { $env:PRIMARY_DB_URL } elseif ($env:PRIMARY_DATASOURCE_URL) { $env:PRIMARY_DATASOURCE_URL } else { $env:DB_URL }
$primaryUser = if ($env:PRIMARY_DB_USERNAME -and $env:PRIMARY_DB_USERNAME.Trim() -ne '') { $env:PRIMARY_DB_USERNAME } elseif ($env:PRIMARY_DATASOURCE_USERNAME) { $env:PRIMARY_DATASOURCE_USERNAME } else { $env:DB_USERNAME }

Write-Host "Reporting (target) URL: $reportingUrl" -ForegroundColor Gray
Write-Host "Primary (source) URL:   $primaryUrl" -ForegroundColor Gray
Write-Host "Primary (source) User:  $primaryUser" -ForegroundColor Gray
if ($reportingUrl -eq $primaryUrl) {
  Write-Host "NOTE: Reporting & primary URLs identical -> single DB mode (Neon NOT separated)." -ForegroundColor Yellow
} else {
  Write-Host "Using distinct databases for reporting (Neon) and primary source." -ForegroundColor Green
}

# 5. Run Maven
$cmd = "mvn $MavenGoals"
if ($SkipBuild) { $cmd = "mvn -DskipTests $MavenGoals" }
$mainClass = if ($Mode -eq 'reporting') { 'com.thedal.thedal_app.reporting.ReportingApplication' } else { 'com.thedal.thedal_app.ThedalAppApplication' }

# Validate separation (only informational)
if ($Mode -eq 'full') {
  if ($reportingUrl -eq $primaryUrl) {
    Write-Host "[WARN] Reporting & primary DB the same. Consider Neon in .env.reporting for isolation." -ForegroundColor Yellow
  } else {
    Write-Host "[OK] Using isolated reporting DB." -ForegroundColor Green
  }
}

$cmd = "$cmd -Dspring-boot.run.main-class=$mainClass"
Write-Host "Mode: $Mode  -> MainClass=$mainClass" -ForegroundColor Cyan
Write-Host "Starting application: $cmd" -ForegroundColor Cyan

# Diagnostics: show key Flyway & datasource env values (masked) prior to launch
$mask = { param($v) if(!$v){'<unset>'} elseif($v.Length -le 8){'***'} else {$v.Substring(0,4)+'***'+$v.Substring($v.Length-3)} }
Write-Host "Diag: spring.flyway.enabled -> $($mask.Invoke($env:SPRING_FLYWAY_ENABLED))" -ForegroundColor DarkGray
Write-Host "Diag: REPORTING_DB_URL       -> $($mask.Invoke($env:REPORTING_DB_URL))" -ForegroundColor DarkGray
Write-Host "Diag: DB_URL                -> $($mask.Invoke($env:DB_URL))" -ForegroundColor DarkGray
Write-Host "Diag: PRIMARY_DB_URL        -> $($mask.Invoke($env:PRIMARY_DB_URL))" -ForegroundColor DarkGray

# Force disable Flyway defensively (already excluded in reporting profile config)
if ($Mode -eq 'reporting') { $env:SPRING_FLYWAY_ENABLED = 'false' }
& cmd /c $cmd
