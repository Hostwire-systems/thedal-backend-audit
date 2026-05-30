-- Add gender-wise DOB count fields to election_dashboard_stats
ALTER TABLE election_dashboard_stats
ADD COLUMN IF NOT EXISTS male_date_of_birth_count INTEGER DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS female_date_of_birth_count INTEGER DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS transgender_date_of_birth_count INTEGER DEFAULT 0 NOT NULL;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_election_dashboard_stats_dob_counts 
ON election_dashboard_stats(male_date_of_birth_count, female_date_of_birth_count, transgender_date_of_birth_count);
