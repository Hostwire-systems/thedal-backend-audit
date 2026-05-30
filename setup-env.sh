#!/bin/bash

# =============================================================================
# Thedal Environment Setup Script
# =============================================================================
# This script helps set up environment variables for the Thedal application

set -e  # Exit on any error

echo "🚀 Thedal Environment Setup Script"
echo "=================================="

# Function to prompt for input with default value
prompt_with_default() {
    local prompt="$1"
    local default="$2"
    local var_name="$3"
    
    if [ -n "$default" ]; then
        read -p "$prompt [$default]: " input
        if [ -z "$input" ]; then
            input="$default"
        fi
    else
        read -p "$prompt (required): " input
        while [ -z "$input" ]; do
            echo "This field is required."
            read -p "$prompt (required): " input
        done
    fi
    
    eval "$var_name='$input'"
}

# Function to prompt for sensitive input (password)
prompt_password() {
    local prompt="$1"
    local var_name="$2"
    
    read -s -p "$prompt (hidden): " input
    echo
    while [ -z "$input" ]; do
        echo "This field is required."
        read -s -p "$prompt (hidden): " input
        echo
    done
    
    eval "$var_name='$input'"
}

echo
echo "Let's set up your environment variables..."
echo

# Environment selection
echo "Select your environment:"
echo "1) Development (local)"
echo "2) Staging"
echo "3) Production"
read -p "Choose [1-3]: " env_choice

case $env_choice in
    1) ENV_TYPE="development" ;;
    2) ENV_TYPE="staging" ;;
    3) ENV_TYPE="production" ;;
    *) echo "Invalid choice. Defaulting to development."; ENV_TYPE="development" ;;
esac

echo
echo "Setting up for $ENV_TYPE environment..."
echo

# Database Configuration
echo "📦 Database Configuration"
echo "------------------------"
if [ "$ENV_TYPE" = "development" ]; then
    prompt_with_default "PostgreSQL URL" "jdbc:postgresql://localhost:5432/thedal" DB_URL
    prompt_with_default "PostgreSQL Username" "postgres" DB_USERNAME
else
    prompt_with_default "PostgreSQL URL" "" DB_URL
    prompt_with_default "PostgreSQL Username" "" DB_USERNAME
fi
prompt_password "PostgreSQL Password" DB_PASSWORD

prompt_with_default "MongoDB URI" "mongodb://localhost:27017" MONGODB_URI
prompt_with_default "MongoDB Database" "thedal_db" MONGODB_DATABASE

# JWT Configuration
echo
echo "🔐 JWT Configuration"
echo "-------------------"
prompt_with_default "JWT Secret Key" "$(openssl rand -base64 32)" JWT_SECRET_KEY
prompt_with_default "Access Token Expiration (minutes)" "30" JWT_ACCESS_TOKEN_EXPIRATION
prompt_with_default "Refresh Token Expiration (minutes)" "60" JWT_REFRESH_TOKEN_EXPIRATION

# AWS Configuration
echo
echo "☁️  AWS Configuration"
echo "--------------------"
prompt_with_default "AWS S3 Access Key" "" AWS_S3_ACCESS_KEY
prompt_password "AWS S3 Secret Key" AWS_S3_SECRET_KEY
prompt_with_default "AWS S3 Region" "ap-south-1" AWS_S3_REGION
prompt_with_default "AWS S3 Endpoint" "" AWS_S3_ENDPOINT

# Email Configuration
echo
echo "📧 Email Configuration"
echo "---------------------"
prompt_with_default "SMTP Host" "smtp.gmail.com" MAIL_HOST
prompt_with_default "SMTP Port" "587" MAIL_PORT
prompt_with_default "SMTP Username" "" MAIL_USERNAME
prompt_password "SMTP Password" MAIL_PASSWORD

# Application URLs
echo
echo "🌐 Application URLs"
echo "------------------"
if [ "$ENV_TYPE" = "development" ]; then
    prompt_with_default "Domain Name" "localhost" THEDAL_DOMAIN_NAME
    prompt_with_default "UI URL" "http://localhost:3000/" THEDAL_UI_URL
    prompt_with_default "Server URL" "http://localhost:8080/" THEDAL_SERVER_URL
else
    prompt_with_default "Domain Name" "" THEDAL_DOMAIN_NAME
    prompt_with_default "UI URL" "" THEDAL_UI_URL
    prompt_with_default "Server URL" "" THEDAL_SERVER_URL
fi

# Create .env file
echo
echo "Creating .env file..."

cat > .env << EOF
# =============================================================================
# THEDAL APPLICATION ENVIRONMENT VARIABLES
# Generated on $(date)
# Environment: $ENV_TYPE
# =============================================================================

# Database Configuration
DB_URL=$DB_URL
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
MONGODB_URI=$MONGODB_URI
MONGODB_DATABASE=$MONGODB_DATABASE

# JWT Configuration
JWT_SECRET_KEY=$JWT_SECRET_KEY
JWT_ACCESS_TOKEN_EXPIRATION=$JWT_ACCESS_TOKEN_EXPIRATION
JWT_REFRESH_TOKEN_EXPIRATION=$JWT_REFRESH_TOKEN_EXPIRATION

# AWS Configuration
AWS_S3_ACCESS_KEY=$AWS_S3_ACCESS_KEY
AWS_S3_SECRET_KEY=$AWS_S3_SECRET_KEY
AWS_S3_REGION=$AWS_S3_REGION
AWS_S3_ENDPOINT=$AWS_S3_ENDPOINT

# Email Configuration
MAIL_HOST=$MAIL_HOST
MAIL_PORT=$MAIL_PORT
MAIL_USERNAME=$MAIL_USERNAME
MAIL_PASSWORD=$MAIL_PASSWORD
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true

# Application URLs
THEDAL_DOMAIN_NAME=$THEDAL_DOMAIN_NAME
THEDAL_UI_URL=$THEDAL_UI_URL
THEDAL_SERVER_URL=$THEDAL_SERVER_URL

# Additional configuration (set these manually if needed)
# AWS_S3_IMAGE_BUCKET=
# AWS_S3_FILES_BUCKET=
# REDIS_HOST=localhost
# REDIS_PORT=6379
# REDIS_PASSWORD=
# SENDGRID_KEY=
# MSG91_AUTHKEY=
# GOOGLE_OAUTH_CLIENT_ID=
# GOOGLE_OAUTH_CLIENT_SECRET=
EOF

echo
echo "✅ Environment setup complete!"
echo
echo "Your .env file has been created with basic configuration."
echo "You may need to add additional variables manually:"
echo "- Redis configuration"
echo "- SendGrid keys"
echo "- OAuth credentials"
echo "- SMS service credentials"
echo
echo "Please review the .env file and update any missing values."
echo "Refer to .env.example for a complete list of available variables."
echo

# Set file permissions
chmod 600 .env
echo "🔒 Set secure permissions on .env file (600)"

echo
echo "🚀 You can now start the application with:"
echo "   docker-compose up -d"
echo "   or"
echo "   mvn spring-boot:run"
echo
