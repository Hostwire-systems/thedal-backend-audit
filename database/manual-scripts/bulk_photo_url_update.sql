-- ============================================================================
-- Bulk Photo URL Update SQL Script
-- Purpose: Update voter photo URLs based on EPIC numbers matching S3 filenames
-- ============================================================================

-- STEP 1: Preview what will be updated (CHECK FIRST!)
-- This shows how many voters will be affected
SELECT 
    COUNT(*) as total_voters_to_update,
    COUNT(DISTINCT election_id) as elections_affected
FROM _voters 
WHERE election_id = 169 
  AND account_id = 1903
  AND (photo_url IS NULL OR photo_url = '');

-- STEP 2: Preview sample transformations (VERIFY EPIC FORMAT!)
-- Check if the EPIC normalization logic works for your data
SELECT 
    epic_number as original_epic,
    REPLACE(REPLACE(REPLACE(REPLACE(epic_number, '/', '_'), '-', '_'), ' ', '_'), '\', '_') as normalized_epic,
    'https://thedalnew.s3.ap-south-1.amazonaws.com/thedalnew/image/' || 
    REPLACE(REPLACE(REPLACE(REPLACE(epic_number, '/', '_'), '-', '_'), ' ', '_'), '\', '_') || '.png' as generated_url
FROM _voters 
WHERE election_id = 151 
  AND account_id = 1653
  AND (photo_url IS NULL OR photo_url = '')
LIMIT 20;

-- STEP 3: BACKUP BEFORE UPDATE (CRITICAL!)
-- Create a backup table with current state
CREATE TABLE IF NOT EXISTS _voters_photo_backup_20251007 AS
SELECT id, epic_number, photo_url, modified_time
FROM _voters 
WHERE election_id = 169 
  AND account_id = 1903;

-- Verify backup created
SELECT COUNT(*) as backed_up_records FROM _voters_photo_backup_20251007;

-- ============================================================================
-- STEP 4: ACTUAL UPDATE (RUN THIS AFTER VERIFYING ABOVE STEPS!)
-- ============================================================================

-- Option A: Update in SMALL BATCHES (RECOMMENDED - Safer, less connection issues)
-- Run this query multiple times until 0 rows affected
UPDATE _voters 
SET 
    photo_url = 'https://thedalnew.s3.ap-south-1.amazonaws.com/thedalnew/image/' || 
                REPLACE(REPLACE(REPLACE(REPLACE(epic_number, '/', '_'), '-', '_'), ' ', '_'), '\', '_') || '.png',
    modified_time = NOW()
WHERE id IN (
    SELECT id 
    FROM _voters 
    WHERE election_id = 151 
      AND account_id = 1653
      AND (photo_url IS NULL OR photo_url = '')
      AND epic_number IS NOT NULL 
      AND epic_number != ''
    LIMIT 5000  -- Process 5000 at a time
);

-- Check progress after each batch
SELECT 
    COUNT(*) as remaining_without_photos
FROM _voters 
WHERE election_id = 151 
  AND account_id = 1653
  AND (photo_url IS NULL OR photo_url = '');

-- ============================================================================
-- Option B: SINGLE UPDATE (If you have good connection, use this)
-- WARNING: May timeout for large datasets!
-- ============================================================================

/*
UPDATE _voters 
SET 
    photo_url = 'https://thedalnew.s3.ap-south-1.amazonaws.com/thedalnew/image/' || 
                REPLACE(REPLACE(REPLACE(REPLACE(epic_number, '/', '_'), '-', '_'), ' ', '_'), '\', '_') || '.png',
    modified_time = NOW()
WHERE election_id = 169 
  AND account_id = 1903
  AND (photo_url IS NULL OR photo_url = '')
  AND epic_number IS NOT NULL 
  AND epic_number != '';
*/

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Check how many voters now have photo URLs
SELECT 
    COUNT(*) as total_voters,
    COUNT(photo_url) as voters_with_photos,
    COUNT(*) - COUNT(photo_url) as voters_without_photos,
    ROUND(COUNT(photo_url) * 100.0 / COUNT(*), 2) as percentage_with_photos
FROM _voters 
WHERE election_id = 169 
  AND account_id = 1903;

-- Sample some updated URLs to verify format
SELECT 
    epic_number,
    photo_url,
    modified_time
FROM _voters 
WHERE election_id = 169 
  AND account_id = 1903
  AND photo_url IS NOT NULL
LIMIT 20;

-- Check for any voters with NULL epic numbers (these won't be updated)
SELECT COUNT(*) as voters_with_null_epic
FROM _voters 
WHERE election_id = 169 
  AND account_id = 1903
  AND (epic_number IS NULL OR epic_number = '');

-- ============================================================================
-- ROLLBACK (If something went wrong!)
-- ============================================================================

/*
-- Restore from backup
UPDATE _voters v
SET 
    photo_url = b.photo_url,
    modified_time = b.modified_time
FROM _voters_photo_backup_20251007 b
WHERE v.id = b.id;

-- Verify rollback
SELECT COUNT(*) FROM _voters WHERE election_id = 169 AND account_id = 1903 AND photo_url IS NOT NULL;
*/

-- ============================================================================
-- CLEANUP (After confirming everything works)
-- ============================================================================

/*
-- Drop backup table (only after verifying everything is correct!)
DROP TABLE IF EXISTS _voters_photo_backup_20251007;
*/
