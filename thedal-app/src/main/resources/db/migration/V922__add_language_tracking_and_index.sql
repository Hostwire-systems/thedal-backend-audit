-- Migration V922: Add language tracking column and performance index to volunteer_vs_voter_report
-- This migration supports the user tracking system for cadre dashboard aggregation

-- Add total_language_updated column if it doesn't exist
ALTER TABLE volunteer_vs_voter_report 
ADD COLUMN IF NOT EXISTS total_language_updated BIGINT NOT NULL DEFAULT 0;

-- Create performance index for faster aggregation queries
-- This index optimizes the query: SELECT ... FROM volunteer_vs_voter_report WHERE election_id = ? AND account_id = ?
CREATE INDEX IF NOT EXISTS idx_volunteer_vs_voter_report_election_account 
ON volunteer_vs_voter_report(election_id, account_id);

-- Additional index for user-specific queries
CREATE INDEX IF NOT EXISTS idx_volunteer_vs_voter_report_user_election 
ON volunteer_vs_voter_report(user_id, election_id, account_id);

-- Comment on the new column
COMMENT ON COLUMN volunteer_vs_voter_report.total_language_updated IS 
'Tracks the number of times this user has updated voter language fields';
