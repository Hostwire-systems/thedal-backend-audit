-- Add 8 new aggregate fields to election_dashboard_stats table
-- totalSchool: Count of distinct schools from part_manager
-- crossBoothFamily: Families spread across multiple booths
-- oneVoterFamily: Single-voter families
-- casteCategoryCount: Distinct caste categories
-- subCasteCount: Distinct sub-castes
-- languageCount: Distinct languages
-- partyAffiliationCount: Distinct political parties
-- schemesCount: Distinct benefit schemes

ALTER TABLE election_dashboard_stats
ADD COLUMN IF NOT EXISTS total_school INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS cross_booth_family INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS one_voter_family INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS caste_category_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS sub_caste_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS language_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS party_affiliation_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS schemes_count INTEGER DEFAULT 0;

-- Add comments for documentation
COMMENT ON COLUMN election_dashboard_stats.total_school IS 'Count of distinct polling schools/locations';
COMMENT ON COLUMN election_dashboard_stats.cross_booth_family IS 'Number of families with members in different booths';
COMMENT ON COLUMN election_dashboard_stats.one_voter_family IS 'Number of families with only one voter';
COMMENT ON COLUMN election_dashboard_stats.caste_category_count IS 'Count of distinct caste categories';
COMMENT ON COLUMN election_dashboard_stats.sub_caste_count IS 'Count of distinct sub-castes';
COMMENT ON COLUMN election_dashboard_stats.language_count IS 'Count of distinct languages spoken by voters';
COMMENT ON COLUMN election_dashboard_stats.party_affiliation_count IS 'Count of distinct political party affiliations';
COMMENT ON COLUMN election_dashboard_stats.schemes_count IS 'Count of distinct benefit schemes enrolled by voters';
