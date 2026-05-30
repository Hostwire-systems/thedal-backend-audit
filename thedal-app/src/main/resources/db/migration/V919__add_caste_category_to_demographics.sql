-- Add caste_category_json, sub_caste_json, availability_json, and schemes_json columns to election_dashboard_demographics table
ALTER TABLE election_dashboard_demographics 
ADD COLUMN IF NOT EXISTS caste_category_json jsonb NOT NULL DEFAULT '{}',
ADD COLUMN IF NOT EXISTS sub_caste_json jsonb NOT NULL DEFAULT '{}',
ADD COLUMN IF NOT EXISTS availability_json jsonb NOT NULL DEFAULT '{}',
ADD COLUMN IF NOT EXISTS schemes_json jsonb NOT NULL DEFAULT '{}';

-- Backfill with empty JSON for existing rows (will be recomputed on next aggregation)
UPDATE election_dashboard_demographics 
SET caste_category_json = '{}'
WHERE caste_category_json IS NULL OR caste_category_json::text = '';
