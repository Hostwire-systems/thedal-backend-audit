-- Add gender-wise mobile count columns to election_dashboard_stats table
ALTER TABLE election_dashboard_stats 
ADD COLUMN IF NOT EXISTS male_mobile_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS female_mobile_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS transgender_mobile_count INTEGER NOT NULL DEFAULT 0;
