-- ==========================================
-- VOTER ACTIVITY TRACKING - DATABASE MIGRATION
-- ==========================================
-- This script creates the voter_activity_log table and adds activity counter columns to _voters table
-- Run this migration on your PostgreSQL database

-- ==========================================
-- STEP 1: Create voter_activity_log table
-- ==========================================
CREATE TABLE IF NOT EXISTS voter_activity_log (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    voter_id VARCHAR(50) NOT NULL,
    activity_type VARCHAR(30) NOT NULL,
    activity_time TIMESTAMP NOT NULL,
    volunteer_id BIGINT,
    template_id BIGINT,
    metadata TEXT,
    
    -- Constraints
    CONSTRAINT chk_activity_type CHECK (activity_type IN (
        'VOTER_SLIP_PRINT',
        'FAMILY_SLIP_PRINT',
        'BENEFIT_SLIP_PRINT',
        'WHATSAPP_SHARE',
        'SMS_SHARE',
        'VOICE_SHARE'
    ))
);

-- ==========================================
-- STEP 2: Create optimized indexes for performance
-- ==========================================

-- Composite index for most common query pattern (voter lookup with activity type)
CREATE INDEX IF NOT EXISTS idx_activity_voter_lookup 
ON voter_activity_log(account_id, election_id, voter_id, activity_type);

-- Index for time-based queries
CREATE INDEX IF NOT EXISTS idx_activity_time 
ON voter_activity_log(activity_time);

-- Index for election-wide statistics
CREATE INDEX IF NOT EXISTS idx_activity_election_type 
ON voter_activity_log(account_id, election_id, activity_type);

-- Index for voter-specific time queries
CREATE INDEX IF NOT EXISTS idx_activity_voter_time 
ON voter_activity_log(voter_id, activity_time);

-- ==========================================
-- STEP 3: Add activity counter columns to _voters table
-- ==========================================

-- Voter slip print count
ALTER TABLE _voters 
ADD COLUMN IF NOT EXISTS voter_slip_print_count INTEGER NOT NULL DEFAULT 0;

-- Family slip print count
ALTER TABLE _voters 
ADD COLUMN IF NOT EXISTS family_slip_print_count INTEGER NOT NULL DEFAULT 0;

-- Benefit slip print count
ALTER TABLE _voters 
ADD COLUMN IF NOT EXISTS benefit_slip_print_count INTEGER NOT NULL DEFAULT 0;

-- WhatsApp share count
ALTER TABLE _voters 
ADD COLUMN IF NOT EXISTS whatsapp_share_count INTEGER NOT NULL DEFAULT 0;

-- SMS share count
ALTER TABLE _voters 
ADD COLUMN IF NOT EXISTS sms_share_count INTEGER NOT NULL DEFAULT 0;

-- Voice share count
ALTER TABLE _voters 
ADD COLUMN IF NOT EXISTS voice_share_count INTEGER NOT NULL DEFAULT 0;

-- ==========================================
-- STEP 4: (Optional) Backfill counters from existing booth_slip_print data
-- ==========================================
-- If you already have booth slip print data, uncomment and run this to backfill:

/*
UPDATE _voters v
SET voter_slip_print_count = COALESCE(
    (SELECT COUNT(*) 
     FROM booth_slip_print b 
     WHERE b.account_id = v.account_id 
     AND b.election_id = v.election_id 
     AND b.voter_id = v.voter_id),
    0
)
WHERE EXISTS (
    SELECT 1 FROM booth_slip_print b 
    WHERE b.account_id = v.account_id 
    AND b.election_id = v.election_id 
    AND b.voter_id = v.voter_id
);
*/

-- ==========================================
-- STEP 5: Add comments for documentation
-- ==========================================

COMMENT ON TABLE voter_activity_log IS 'Audit log for all voter-related activities including slip prints and communication shares';
COMMENT ON COLUMN voter_activity_log.activity_type IS 'Type of activity: VOTER_SLIP_PRINT, FAMILY_SLIP_PRINT, BENEFIT_SLIP_PRINT, WHATSAPP_SHARE, SMS_SHARE, VOICE_SHARE';
COMMENT ON COLUMN voter_activity_log.metadata IS 'JSON field for storing additional context about the activity';

COMMENT ON COLUMN _voters.voter_slip_print_count IS 'Cached count of voter slip prints for performance';
COMMENT ON COLUMN _voters.family_slip_print_count IS 'Cached count of family slip prints for performance';
COMMENT ON COLUMN _voters.benefit_slip_print_count IS 'Cached count of benefit slip prints for performance';
COMMENT ON COLUMN _voters.whatsapp_share_count IS 'Cached count of WhatsApp shares for performance';
COMMENT ON COLUMN _voters.sms_share_count IS 'Cached count of SMS shares for performance';
COMMENT ON COLUMN _voters.voice_share_count IS 'Cached count of voice shares for performance';

-- ==========================================
-- VERIFICATION QUERIES
-- ==========================================
-- Run these to verify the migration was successful:

-- Check if voter_activity_log table exists
SELECT table_name, column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'voter_activity_log' 
ORDER BY ordinal_position;

-- Check if indexes were created
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'voter_activity_log';

-- Check if counter columns were added to _voters
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_name = '_voters' 
AND column_name LIKE '%_count';

-- ==========================================
-- ROLLBACK SCRIPT (if needed)
-- ==========================================
-- Uncomment and run this if you need to rollback the migration:

/*
-- Drop indexes
DROP INDEX IF EXISTS idx_activity_voter_lookup;
DROP INDEX IF EXISTS idx_activity_time;
DROP INDEX IF EXISTS idx_activity_election_type;
DROP INDEX IF EXISTS idx_activity_voter_time;

-- Drop table
DROP TABLE IF EXISTS voter_activity_log;

-- Remove columns from _voters
ALTER TABLE _voters DROP COLUMN IF EXISTS voter_slip_print_count;
ALTER TABLE _voters DROP COLUMN IF EXISTS family_slip_print_count;
ALTER TABLE _voters DROP COLUMN IF EXISTS benefit_slip_print_count;
ALTER TABLE _voters DROP COLUMN IF EXISTS whatsapp_share_count;
ALTER TABLE _voters DROP COLUMN IF EXISTS sms_share_count;
ALTER TABLE _voters DROP COLUMN IF EXISTS voice_share_count;
*/
