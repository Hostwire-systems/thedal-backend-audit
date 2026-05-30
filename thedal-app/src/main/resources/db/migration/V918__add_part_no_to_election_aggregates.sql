-- Add part_no column to election aggregate tables for part-level filtering
-- part_no NULL = election-wide aggregates
-- part_no = X = part-specific aggregates
-- Note: Using VARCHAR(50) to match part_manager.part_no column type

-- 1. Add part_no to election_dashboard_stats
ALTER TABLE election_dashboard_stats
    ADD COLUMN IF NOT EXISTS part_no VARCHAR(50);

-- Drop existing unique constraint
ALTER TABLE election_dashboard_stats
    DROP CONSTRAINT IF EXISTS uq_election_dashboard_stats;

-- Add new unique constraint including part_no
ALTER TABLE election_dashboard_stats
    ADD CONSTRAINT uq_election_dashboard_stats 
    UNIQUE (account_id, election_id, part_no);

-- Create index for part_no queries
CREATE INDEX IF NOT EXISTS idx_election_dashboard_stats_part_no
    ON election_dashboard_stats (account_id, election_id, part_no)
    WHERE part_no IS NOT NULL;


-- 2. Add part_no to election_dashboard_demographics
ALTER TABLE election_dashboard_demographics
    ADD COLUMN IF NOT EXISTS part_no VARCHAR(50);

-- Drop existing unique constraint
ALTER TABLE election_dashboard_demographics
    DROP CONSTRAINT IF EXISTS uq_election_dashboard_demographics;

-- Add new unique constraint including part_no
ALTER TABLE election_dashboard_demographics
    ADD CONSTRAINT uq_election_dashboard_demographics 
    UNIQUE (account_id, election_id, part_no);

-- Create index for part_no queries
CREATE INDEX IF NOT EXISTS idx_election_dashboard_demographics_part_no
    ON election_dashboard_demographics (account_id, election_id, part_no)
    WHERE part_no IS NOT NULL;


-- 3. Add part_no to election_dashboard_party_polling
ALTER TABLE election_dashboard_party_polling
    ADD COLUMN IF NOT EXISTS part_no VARCHAR(50);

-- Drop existing unique constraint
ALTER TABLE election_dashboard_party_polling
    DROP CONSTRAINT IF EXISTS uq_election_dashboard_party_polling;

-- Add new unique constraint including part_no
ALTER TABLE election_dashboard_party_polling
    ADD CONSTRAINT uq_election_dashboard_party_polling 
    UNIQUE (account_id, election_id, part_no);

-- Create index for part_no queries
CREATE INDEX IF NOT EXISTS idx_election_dashboard_party_polling_part_no
    ON election_dashboard_party_polling (account_id, election_id, part_no)
    WHERE part_no IS NOT NULL;

-- Comment for documentation
COMMENT ON COLUMN election_dashboard_stats.part_no IS 'NULL for election-wide stats, specific part number for part-level stats';
COMMENT ON COLUMN election_dashboard_demographics.part_no IS 'NULL for election-wide demographics, specific part number for part-level demographics';
COMMENT ON COLUMN election_dashboard_party_polling.part_no IS 'NULL for election-wide party polling, specific part number for part-level party polling';
