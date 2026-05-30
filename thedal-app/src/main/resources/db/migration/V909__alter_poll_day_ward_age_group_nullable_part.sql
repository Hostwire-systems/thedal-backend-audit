-- Migration: V909 - Alter poll_day_ward_age_group_turnout to make part_number optional
-- Description: Allow null part_number for aggregated all-booths data and update unique constraint

-- Drop the existing unique constraint
ALTER TABLE poll_day_ward_age_group_turnout 
DROP CONSTRAINT IF EXISTS uq_poll_day_ward_age_group;

-- Make part_number nullable
ALTER TABLE poll_day_ward_age_group_turnout 
ALTER COLUMN part_number DROP NOT NULL;

-- Create a partial unique index for non-null part_number
CREATE UNIQUE INDEX IF NOT EXISTS idx_uq_poll_day_ward_age_with_part 
ON poll_day_ward_age_group_turnout(account_id, election_id, part_number, polling_date)
WHERE part_number IS NOT NULL;

-- Create a partial unique index for null part_number (all booths aggregated)
CREATE UNIQUE INDEX IF NOT EXISTS idx_uq_poll_day_ward_age_all_booths 
ON poll_day_ward_age_group_turnout(account_id, election_id, polling_date)
WHERE part_number IS NULL;

-- Comment
COMMENT ON COLUMN poll_day_ward_age_group_turnout.part_number IS 'Ward/Part number from voter records. NULL means aggregated data across all booths/parts.';
