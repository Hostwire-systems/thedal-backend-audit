-- Performance Optimization Indexes for Voters API
-- These indexes improve query performance for common filter combinations
-- Safe to run - will skip if indexes already exist

-- Index for mobile number filters (hasMobileNo parameter)
CREATE INDEX IF NOT EXISTS idx_account_election_has_mobile 
ON _voters(account_id, election_id) 
WHERE mobile_no IS NOT NULL AND TRIM(mobile_no) != '';

-- Index for poll status filter (hasVoted parameter)
CREATE INDEX IF NOT EXISTS idx_account_election_has_voted 
ON _voters(account_id, election_id, has_voted);

-- Index for section number (overseas voters filter)
CREATE INDEX IF NOT EXISTS idx_account_election_section 
ON _voters(account_id, election_id, section_no);

-- Index for family count (single voter family filter)
CREATE INDEX IF NOT EXISTS idx_account_election_family_count 
ON _voters(account_id, election_id, family_id, family_count) 
WHERE family_id IS NOT NULL;

-- Composite index for common gender + age filtering
CREATE INDEX IF NOT EXISTS idx_account_election_gender_age 
ON _voters(account_id, election_id, gender, age);

-- Index for relation type (fatherless/guardian filters)
CREATE INDEX IF NOT EXISTS idx_account_election_rln_type 
ON _voters(account_id, election_id, rln_type);

-- Composite index for booth + voting status (poll day queries)
CREATE INDEX IF NOT EXISTS idx_booth_has_voted_part_serial 
ON _voters(part_no, has_voted, serial_no);

-- Index for birthday queries (DOB month/day extraction)
CREATE INDEX IF NOT EXISTS idx_account_election_dob_month_day 
ON _voters(account_id, election_id, EXTRACT(MONTH FROM dob), EXTRACT(DAY FROM dob)) 
WHERE dob IS NOT NULL;

-- Analyze tables after index creation for query planner optimization
ANALYZE _voters;

-- Display index statistics
SELECT 
    schemaname,
    relname as tablename,
    indexrelname as indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
FROM pg_stat_user_indexes
WHERE relname = '_voters'
ORDER BY pg_relation_size(indexrelid) DESC;
