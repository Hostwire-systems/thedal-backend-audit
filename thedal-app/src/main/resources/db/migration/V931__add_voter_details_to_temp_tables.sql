-- Add voter detail columns to temp tables for full data retrieval
-- This allows us to store complete voter information during comparison

-- Add columns to sir_temp_base_voters
ALTER TABLE sir_temp_base_voters 
ADD COLUMN IF NOT EXISTS voter_name_en VARCHAR(255),
ADD COLUMN IF NOT EXISTS serial_no BIGINT,
ADD COLUMN IF NOT EXISTS section_no INTEGER,
ADD COLUMN IF NOT EXISTS house_no_en VARCHAR(100),
ADD COLUMN IF NOT EXISTS age INTEGER,
ADD COLUMN IF NOT EXISTS gender VARCHAR(10);

-- Add columns to sir_temp_new_voters
ALTER TABLE sir_temp_new_voters 
ADD COLUMN IF NOT EXISTS voter_name_en VARCHAR(255),
ADD COLUMN IF NOT EXISTS serial_no BIGINT,
ADD COLUMN IF NOT EXISTS section_no INTEGER,
ADD COLUMN IF NOT EXISTS house_no_en VARCHAR(100),
ADD COLUMN IF NOT EXISTS age INTEGER,
ADD COLUMN IF NOT EXISTS gender VARCHAR(10);
