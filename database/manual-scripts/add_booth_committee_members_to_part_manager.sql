-- ==========================================
-- ADD BOOTH COMMITTEE MEMBERS TO PART MANAGER
-- ==========================================
-- Migration: Add booth_committee_members field to part_manager table
-- Purpose: Store up to 15 committee members per booth with name, designation, and mobile
-- Storage: TEXT column (JSON format) for PostgreSQL compatibility
-- Date: December 3, 2025

-- ==========================================
-- STEP 1: Add booth_committee_members column
-- ==========================================
ALTER TABLE part_manager 
ADD COLUMN IF NOT EXISTS booth_committee_members TEXT DEFAULT '[]';

-- ==========================================
-- STEP 2: Add comment for documentation
-- ==========================================
COMMENT ON COLUMN part_manager.booth_committee_members IS 
'JSON array of booth committee members (max 15). Format: [{"name":"string","designation":"string","mobileNumber":"string"}]';

-- ==========================================
-- STEP 3: Update existing NULL values to empty array
-- ==========================================
UPDATE part_manager 
SET booth_committee_members = '[]' 
WHERE booth_committee_members IS NULL;

-- ==========================================
-- VERIFICATION QUERIES
-- ==========================================
-- Run these to verify the migration was successful:

-- Check if column exists
SELECT column_name, data_type, column_default, is_nullable
FROM information_schema.columns 
WHERE table_name = 'part_manager' 
AND column_name = 'booth_committee_members';

-- Check sample data
SELECT 
    id, 
    part_no, 
    part_name_english,
    booth_committee_members,
    LENGTH(booth_committee_members) as json_length
FROM part_manager 
LIMIT 10;

-- Count rows with committee members
SELECT 
    COUNT(*) as total_parts,
    COUNT(CASE WHEN booth_committee_members != '[]' THEN 1 END) as parts_with_members,
    COUNT(CASE WHEN booth_committee_members = '[]' THEN 1 END) as parts_without_members
FROM part_manager;

-- ==========================================
-- ROLLBACK SCRIPT (if needed)
-- ==========================================
-- Uncomment and run this if you need to rollback the migration:

/*
ALTER TABLE part_manager 
DROP COLUMN IF EXISTS booth_committee_members;
*/

-- ==========================================
-- SAMPLE DATA INSERTION (for testing)
-- ==========================================
-- Example: Add committee members to a specific part

/*
UPDATE part_manager 
SET booth_committee_members = '[
    {
        "name": "John Doe",
        "designation": "President",
        "mobileNumber": "9876543210"
    },
    {
        "name": "Jane Smith",
        "designation": "Secretary",
        "mobileNumber": "9876543211"
    }
]'
WHERE id = 1;
*/

-- ==========================================
-- VALIDATION QUERIES
-- ==========================================
-- Query to validate JSON format

/*
SELECT 
    id, 
    part_no,
    booth_committee_members,
    CASE 
        WHEN booth_committee_members::text = '[]' THEN 'Empty'
        WHEN booth_committee_members IS NULL THEN 'NULL'
        ELSE 'Has Data'
    END as status
FROM part_manager
WHERE booth_committee_members IS NOT NULL
LIMIT 20;
*/
