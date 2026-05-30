-- Migration script to add unique constraint to poll_day_chart_configs table
-- Date: 2025-11-14
-- Description: Add unique constraint on account_id and election_id to prevent duplicate configurations

-- First, check if there are any duplicates (there shouldn't be based on the data provided)
-- SELECT account_id, election_id, COUNT(*) 
-- FROM poll_day_chart_configs 
-- GROUP BY account_id, election_id 
-- HAVING COUNT(*) > 1;

-- Add unique constraint
ALTER TABLE poll_day_chart_configs 
ADD CONSTRAINT uk_poll_day_chart_config_account_election 
UNIQUE (account_id, election_id);

-- Add index for better query performance (if not already created by the unique constraint)
-- CREATE INDEX IF NOT EXISTS idx_poll_day_chart_config_account_election 
-- ON poll_day_chart_configs(account_id, election_id);
