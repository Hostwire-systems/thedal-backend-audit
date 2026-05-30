-- Fix part_no column type from INTEGER to VARCHAR(50) to match part_manager table

BEGIN;

-- 1. Fix election_dashboard_stats
ALTER TABLE election_dashboard_stats DROP COLUMN IF EXISTS part_no CASCADE;
ALTER TABLE election_dashboard_stats ADD COLUMN IF NOT EXISTS part_no VARCHAR(50);
ALTER TABLE election_dashboard_stats DROP CONSTRAINT IF EXISTS uq_election_dashboard_stats;
ALTER TABLE election_dashboard_stats ADD CONSTRAINT uq_election_dashboard_stats UNIQUE (account_id, election_id, part_no);
CREATE INDEX IF NOT EXISTS idx_election_dashboard_stats_part_no ON election_dashboard_stats (account_id, election_id, part_no) WHERE part_no IS NOT NULL;

-- 2. Fix election_dashboard_demographics
ALTER TABLE election_dashboard_demographics DROP COLUMN IF EXISTS part_no CASCADE;
ALTER TABLE election_dashboard_demographics ADD COLUMN IF NOT EXISTS part_no VARCHAR(50);
ALTER TABLE election_dashboard_demographics DROP CONSTRAINT IF EXISTS uq_election_dashboard_demographics;
ALTER TABLE election_dashboard_demographics ADD CONSTRAINT uq_election_dashboard_demographics UNIQUE (account_id, election_id, part_no);
CREATE INDEX IF NOT EXISTS idx_election_dashboard_demographics_part_no ON election_dashboard_demographics (account_id, election_id, part_no) WHERE part_no IS NOT NULL;

-- 3. Fix election_dashboard_party_polling
ALTER TABLE election_dashboard_party_polling DROP COLUMN IF EXISTS part_no CASCADE;
ALTER TABLE election_dashboard_party_polling ADD COLUMN IF NOT EXISTS part_no VARCHAR(50);
ALTER TABLE election_dashboard_party_polling DROP CONSTRAINT IF EXISTS uq_election_dashboard_party_polling;
ALTER TABLE election_dashboard_party_polling ADD CONSTRAINT uq_election_dashboard_party_polling UNIQUE (account_id, election_id, part_no);
CREATE INDEX IF NOT EXISTS idx_election_dashboard_party_polling_part_no ON election_dashboard_party_polling (account_id, election_id, part_no) WHERE part_no IS NOT NULL;

COMMIT;
