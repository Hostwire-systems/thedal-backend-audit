@echo off
REM =============================================================================
REM Thedal Environment Setup Script for Windows
REM =============================================================================

echo 🚀 Thedal Environment Setup Script
echo ==================================
echo.

echo Let's set up your environment variables...
echo.

REM Environment selection
echo Select your environment:
echo 1^) Development ^(local^)
echo 2^) Staging
echo 3^) Production
set /p env_choice="Choose [1-3]: "

if "%env_choice%"=="1" (
    set ENV_TYPE=development
) else if "%env_choice%"=="2" (
    set ENV_TYPE=staging
) else if "%env_choice%"=="3" (
    set ENV_TYPE=production
) else (
    echo Invalid choice. Defaulting to development.
    set ENV_TYPE=development
)

echo.
echo Setting up for %ENV_TYPE% environment...
echo.

REM Database Configuration
echo 📦 Database Configuration
echo ------------------------
if "%ENV_TYPE%"=="development" (
    set /p DB_URL="PostgreSQL URL [jdbc:postgresql://localhost:5432/thedal]: "
    if "%DB_URL%"=="" set DB_URL=jdbc:postgresql://localhost:5432/thedal
    
    set /p DB_USERNAME="PostgreSQL Username [postgres]: "
    if "%DB_USERNAME%"=="" set DB_USERNAME=postgres
) else (
    set /p DB_URL="PostgreSQL URL (required): "
    set /p DB_USERNAME="PostgreSQL Username (required): "
)

set /p DB_PASSWORD="PostgreSQL Password (required): "

set /p MONGODB_URI="MongoDB URI [mongodb://localhost:27017]: "
if "%MONGODB_URI%"=="" set MONGODB_URI=mongodb://localhost:27017

set /p MONGODB_DATABASE="MongoDB Database [thedal_db]: "
if "%MONGODB_DATABASE%"=="" set MONGODB_DATABASE=thedal_db

REM JWT Configuration
echo.
echo 🔐 JWT Configuration
echo -------------------
set /p JWT_SECRET_KEY="JWT Secret Key [auto-generated]: "
if "%JWT_SECRET_KEY%"=="" for /f "delims=" %%A in ('powershell -NoProfile -Command "[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))"') do set JWT_SECRET_KEY=%%A

set /p JWT_ACCESS_TOKEN_EXPIRATION="Access Token Expiration (minutes) [30]: "
if "%JWT_ACCESS_TOKEN_EXPIRATION%"=="" set JWT_ACCESS_TOKEN_EXPIRATION=30

set /p JWT_REFRESH_TOKEN_EXPIRATION="Refresh Token Expiration (minutes) [60]: "
if "%JWT_REFRESH_TOKEN_EXPIRATION%"=="" set JWT_REFRESH_TOKEN_EXPIRATION=60

REM AWS Configuration
echo.
echo ☁️  AWS Configuration
echo --------------------
set /p AWS_S3_ACCESS_KEY="AWS S3 Access Key (required): "
set /p AWS_S3_SECRET_KEY="AWS S3 Secret Key (required): "

set /p AWS_S3_REGION="AWS S3 Region [ap-south-1]: "
if "%AWS_S3_REGION%"=="" set AWS_S3_REGION=ap-south-1

set /p AWS_S3_ENDPOINT="AWS S3 Endpoint (required): "

REM Email Configuration
echo.
echo 📧 Email Configuration
echo ---------------------
set /p MAIL_HOST="SMTP Host [smtp.gmail.com]: "
if "%MAIL_HOST%"=="" set MAIL_HOST=smtp.gmail.com

set /p MAIL_PORT="SMTP Port [587]: "
if "%MAIL_PORT%"=="" set MAIL_PORT=587

set /p MAIL_USERNAME="SMTP Username (required): "
set /p MAIL_PASSWORD="SMTP Password (required): "

REM Application URLs
echo.
echo 🌐 Application URLs
echo ------------------
if "%ENV_TYPE%"=="development" (
    set /p THEDAL_DOMAIN_NAME="Domain Name [localhost]: "
    if "%THEDAL_DOMAIN_NAME%"=="" set THEDAL_DOMAIN_NAME=localhost
    
    set /p THEDAL_UI_URL="UI URL [http://localhost:3000/]: "
    if "%THEDAL_UI_URL%"=="" set THEDAL_UI_URL=http://localhost:3000/
    
    set /p THEDAL_SERVER_URL="Server URL [http://localhost:8080/]: "
    if "%THEDAL_SERVER_URL%"=="" set THEDAL_SERVER_URL=http://localhost:8080/
) else (
    set /p THEDAL_DOMAIN_NAME="Domain Name (required): "
    set /p THEDAL_UI_URL="UI URL (required): "
    set /p THEDAL_SERVER_URL="Server URL (required): "
)

REM Create .env file
echo.
echo Creating .env file...

(
echo # =============================================================================
echo # THEDAL APPLICATION ENVIRONMENT VARIABLES
echo # Generated on %date% %time%
echo # Environment: %ENV_TYPE%
echo # =============================================================================
echo.
echo # Database Configuration
echo DB_URL=%DB_URL%
echo DB_USERNAME=%DB_USERNAME%
echo DB_PASSWORD=%DB_PASSWORD%
echo MONGODB_URI=%MONGODB_URI%
echo MONGODB_DATABASE=%MONGODB_DATABASE%
echo.
echo # JWT Configuration
echo JWT_SECRET_KEY=%JWT_SECRET_KEY%
echo JWT_ACCESS_TOKEN_EXPIRATION=%JWT_ACCESS_TOKEN_EXPIRATION%
echo JWT_REFRESH_TOKEN_EXPIRATION=%JWT_REFRESH_TOKEN_EXPIRATION%
echo.
echo # AWS Configuration
echo AWS_S3_ACCESS_KEY=%AWS_S3_ACCESS_KEY%
echo AWS_S3_SECRET_KEY=%AWS_S3_SECRET_KEY%
echo AWS_S3_REGION=%AWS_S3_REGION%
echo AWS_S3_ENDPOINT=%AWS_S3_ENDPOINT%
echo.
echo # Email Configuration
echo MAIL_HOST=%MAIL_HOST%
echo MAIL_PORT=%MAIL_PORT%
echo MAIL_USERNAME=%MAIL_USERNAME%
echo MAIL_PASSWORD=%MAIL_PASSWORD%
echo MAIL_SMTP_AUTH=true
echo MAIL_SMTP_STARTTLS=true
echo.
echo # Application URLs
echo THEDAL_DOMAIN_NAME=%THEDAL_DOMAIN_NAME%
echo THEDAL_UI_URL=%THEDAL_UI_URL%
echo THEDAL_SERVER_URL=%THEDAL_SERVER_URL%
echo.
echo # Additional configuration ^(set these manually if needed^)
echo # AWS_S3_IMAGE_BUCKET=
echo # AWS_S3_FILES_BUCKET=
echo # REDIS_HOST=localhost
echo # REDIS_PORT=6379
echo # REDIS_PASSWORD=
echo # SENDGRID_KEY=
echo # MSG91_AUTHKEY=
echo # GOOGLE_OAUTH_CLIENT_ID=
echo # GOOGLE_OAUTH_CLIENT_SECRET=
) > .env

echo.
echo ✅ Environment setup complete!
echo.
echo Your .env file has been created with basic configuration.
echo You may need to add additional variables manually:
echo - Redis configuration
echo - SendGrid keys
echo - OAuth credentials
echo - SMS service credentials
echo.
echo Please review the .env file and update any missing values.
echo Refer to .env.example for a complete list of available variables.
echo.
echo 🚀 You can now start the application with:
echo    docker-compose up -d
echo    or
echo    mvn spring-boot:run
echo.
pause
