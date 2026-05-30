-- Migration: Add indexes on _voters table for filter performance
-- Purpose: Optimize poll-day reporting queries with advanced filters
-- Date: 2025-11-07

-- Composite index for common filter combinations
CREATE INDEX IF NOT EXISTS idx_voters_filter_composite 
ON _voters(account_id, election_id, part_no, has_voted, party_id, religion_id, caste_id);

-- Index for gender and age filters
CREATE INDEX IF NOT EXISTS idx_voters_demographics 
ON _voters(account_id, election_id, gender, age);

-- Individual foreign key indexes for joins
CREATE INDEX IF NOT EXISTS idx_voters_party_fk 
ON _voters(party_id) WHERE party_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_voters_religion_fk 
ON _voters(religion_id) WHERE religion_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_voters_caste_fk 
ON _voters(caste_id) WHERE caste_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_voters_caste_category_fk 
ON _voters(caste_category_id) WHERE caste_category_id IS NOT NULL;

-- Index for family-wise aggregation with filters
CREATE INDEX IF NOT EXISTS idx_voters_family_filters 
ON _voters(account_id, election_id, family_id, has_voted, party_id, religion_id)
WHERE family_id IS NOT NULL;

-- Index for polling date filtering
CREATE INDEX IF NOT EXISTS idx_voters_voted_timestamp 
ON _voters(account_id, election_id, voted_timestamp) 
WHERE has_voted = true;
