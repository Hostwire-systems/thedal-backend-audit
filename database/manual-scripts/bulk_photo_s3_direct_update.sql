-- ============================================================================
-- BULK PHOTO URL UPDATE FROM S3 DIRECT UPLOAD
-- ============================================================================
-- This script updates voter photo URLs when photos are uploaded directly to S3
-- Photos should be named with EPIC number (e.g., IPR1840586.png)
-- ============================================================================

-- ====================
-- OPTION 1: Update specific election
-- ====================
-- Replace these values:
--   - 'YOUR_ELECTION_ID' with actual election ID
--   - 'YOUR_ACCOUNT_ID' with actual account ID
--   - Update S3 base URL if different
-- ====================

DO $$
DECLARE
    v_election_id BIGINT := YOUR_ELECTION_ID;  -- CHANGE THIS
    v_account_id BIGINT := YOUR_ACCOUNT_ID;    -- CHANGE THIS
    v_s3_base_url TEXT := 'https://thedalnew.s3.ap-south-1.amazonaws.com/thedalnew/image/';
    v_updated_count INTEGER := 0;
BEGIN
    -- Update all voters with constructed photo URLs
    -- Tries both .png and .jpg extensions
    UPDATE voter
    SET photo_url = CASE 
        WHEN EXISTS (
            SELECT 1 FROM voter v2 
            WHERE v2.epic_number = voter.epic_number 
            AND v2.account_id = v_account_id 
            AND v2.election_id = v_election_id
        ) THEN v_s3_base_url || 'voter_photo_' || epic_number || '_*' || '.png'
        ELSE NULL
    END,
    updated_at = NOW()
    WHERE account_id = v_account_id
      AND election_id = v_election_id
      AND epic_number IS NOT NULL
      AND epic_number != '';
    
    GET DIAGNOSTICS v_updated_count = ROW_COUNT;
    
    RAISE NOTICE 'Updated % voters with photo URLs', v_updated_count;
END $$;


-- ====================
-- OPTION 2: Simple UPDATE with pattern matching
-- ====================
-- This assumes photo filenames follow pattern: voter_photo_{EPIC}_{timestamp}_{random}.{ext}
-- Or just: {EPIC}.png or {EPIC}.jpg
-- ====================

-- If photos are named exactly as EPIC.png or EPIC.jpg:
/*
UPDATE voter
SET photo_url = 'https://thedalnew.s3.ap-south-1.amazonaws.com/thedalnew/image/' || epic_number || '.png',
    updated_at = NOW()
WHERE account_id = YOUR_ACCOUNT_ID
  AND election_id = YOUR_ELECTION_ID
  AND epic_number IS NOT NULL
  AND epic_number != '';
*/


-- ====================
-- OPTION 3: Batch update with progress tracking
-- ====================
-- Updates in batches of 1000 to avoid long locks
-- ====================

DO $$
DECLARE
    v_election_id BIGINT := YOUR_ELECTION_ID;  -- CHANGE THIS
    v_account_id BIGINT := YOUR_ACCOUNT_ID;    -- CHANGE THIS
    v_s3_base_url TEXT := 'https://thedalnew.s3.ap-south-1.amazonaws.com/thedalnew/image/';
    v_batch_size INTEGER := 1000;
    v_total_updated INTEGER := 0;
    v_batch_updated INTEGER;
    v_extension TEXT;
BEGIN
    -- Try .png extension first
    v_extension := '.png';
    
    LOOP
        UPDATE voter
        SET photo_url = v_s3_base_url || epic_number || v_extension,
            updated_at = NOW()
        WHERE id IN (
            SELECT id
            FROM voter
            WHERE account_id = v_account_id
              AND election_id = v_election_id
              AND epic_number IS NOT NULL
              AND epic_number != ''
              AND (photo_url IS NULL OR photo_url = '')
            LIMIT v_batch_size
        );
        
        GET DIAGNOSTICS v_batch_updated = ROW_COUNT;
        v_total_updated := v_total_updated + v_batch_updated;
        
        RAISE NOTICE 'Batch updated % voters, total: %', v_batch_updated, v_total_updated;
        
        EXIT WHEN v_batch_updated = 0;
        
        -- Small delay between batches
        PERFORM pg_sleep(0.1);
    END LOOP;
    
    RAISE NOTICE 'Total updated: % voters with photo URLs', v_total_updated;
END $$;


-- ====================
-- OPTION 4: Java Service Method (Recommended)
-- ====================
-- See VoterPhotoUrlUpdateService.java for the service implementation
-- This provides:
-- - Better error handling
-- - Progress tracking
-- - Support for multiple file extensions
-- - Verification of S3 file existence (optional)
-- ====================


-- ====================
-- VERIFICATION QUERIES
-- ====================

-- Check how many voters have photos after update
SELECT 
    COUNT(*) as total_voters,
    COUNT(photo_url) as voters_with_photos,
    COUNT(*) - COUNT(photo_url) as voters_without_photos,
    ROUND(COUNT(photo_url) * 100.0 / COUNT(*), 2) as coverage_percentage
FROM voter
WHERE account_id = YOUR_ACCOUNT_ID
  AND election_id = YOUR_ELECTION_ID;

-- Sample voters with updated photos
SELECT id, epic_number, photo_url
FROM voter
WHERE account_id = YOUR_ACCOUNT_ID
  AND election_id = YOUR_ELECTION_ID
  AND photo_url IS NOT NULL
LIMIT 10;

-- Find voters without photos
SELECT id, epic_number, first_name, last_name
FROM voter
WHERE account_id = YOUR_ACCOUNT_ID
  AND election_id = YOUR_ELECTION_ID
  AND (photo_url IS NULL OR photo_url = '')
LIMIT 20;


-- ====================
-- ROLLBACK (if needed)
-- ====================
-- Clear all photo URLs for an election
/*
UPDATE voter
SET photo_url = NULL,
    updated_at = NOW()
WHERE account_id = YOUR_ACCOUNT_ID
  AND election_id = YOUR_ELECTION_ID;
*/
