-- Fix Flyway schema history after failed migration V921
-- Run this in your PostgreSQL database before starting the application

-- Delete the failed migration record
DELETE FROM flyway_schema_history WHERE version = '921' AND success = false;

-- Verify the deletion
SELECT * FROM flyway_schema_history WHERE version = '921';
