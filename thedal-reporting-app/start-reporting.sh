#!/bin/bash

# Change to the reporting app directory
cd "$(dirname "$0")"

# Use same database as main app for now
export DB_URL="jdbc:postgresql://localhost:5432/thedal"
export DB_USERNAME="postgres"
export DB_PASSWORD=""

echo "Starting Thedal Reporting Service..."
echo "Database: $DB_URL"
echo "Working directory: $(pwd)"

mvn spring-boot:run
