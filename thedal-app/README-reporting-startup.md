# Reporting / Dual-DB Startup Guide

This guide explains how to start the application with or without an isolated reporting (Neon) database using `start-reporting.ps1`.

## Script Modes

| Mode | Main Class | Use Case |
|------|------------|----------|
| full | `com.thedal.thedal_app.ThedalAppApplication` | Normal full platform including reporting aggregations (recommended) |
| reporting | `com.thedal.thedal_app.reporting.ReportingApplication` | Lightweight shadow reporting-only instance (optional) |

Default mode is `full`.

## One-Click Startup
From `thedal-app` directory in PowerShell:
```
./start-reporting.ps1
```
Equivalent explicit:
```
./start-reporting.ps1 -Mode full
```
Shadow reporting only:
```
./start-reporting.ps1 -Mode reporting
```

## Environment Files

| File | Purpose | Loaded Automatically | Required |
|------|---------|----------------------|----------|
| `.env.staging` | Core app + primary DB credentials | Yes | Yes |
| `.env.reporting` | Neon reporting DB (REPORTING_DB_*) | If present | No (falls back to primary) |
| `.env.primary-source` | Separate operational/source DB (PRIMARY_DB_*) | If present | No (falls back to staging DB) |

## Variables
- Reporting datasource: `REPORTING_DB_URL`, `REPORTING_DB_USERNAME`, `REPORTING_DB_PASSWORD`
- Primary/source datasource: `PRIMARY_DB_URL`, `PRIMARY_DB_USERNAME`, `PRIMARY_DB_PASSWORD`
- Legacy compatibility vars also accepted: `PRIMARY_DATASOURCE_URL`, etc.

If `REPORTING_DB_URL` is absent the script prints a warning and uses `DB_URL` for both roles.

## Validation Output
On startup the script prints the two JDBC URLs:
```
Reporting (target) URL: jdbc:postgresql://<neon-host>/...
Primary (source) URL:   jdbc:postgresql://<primary-host>/...
[OK] Using isolated reporting DB.
```
Or warning if they are identical.

## Overriding Main Class Manually
You can still run:
```
mvn spring-boot:run -Dspring-boot.run.main-class=com.thedal.thedal_app.ThedalAppApplication
```
But the script automates this selection.

## Common Issues
| Symptom | Cause | Fix |
|---------|-------|-----|
| Multiple main class error | Ran mvn directly without main-class flag | Use script or add -Dspring-boot.run.main-class |
| Reporting not isolated | Missing `.env.reporting` | Create file with Neon creds |
| Secondary datasource fails | Missing PRIMARY_DB_* vars when profile=reporting | Add `.env.primary-source` or let script map staging vars |

## Security
`.env.reporting` and `.env.primary-source` are in `.gitignore`. Do not commit credentials.

---
Updated: 2025-09-03
