-- Drop old heavy tables
DROP TABLE IF EXISTS sir_temp_base_voters CASCADE;
DROP TABLE IF EXISTS sir_temp_new_voters CASCADE;
DROP TABLE IF EXISTS sir_temp_voter_details_cache CASCADE;

-- Create ultra-lightweight tables for fast comparison (only 2 columns needed)
CREATE TABLE sir_temp_base_voters (
    job_id UUID NOT NULL,
    epic_number VARCHAR(50) NOT NULL,
    part_no INTEGER
);

CREATE TABLE sir_temp_new_voters (
    job_id UUID NOT NULL,
    epic_number VARCHAR(50) NOT NULL,
    part_no INTEGER
);

-- Optimized indexes for blazing-fast JOINs
CREATE INDEX idx_sir_temp_base_job_epic ON sir_temp_base_voters(job_id, epic_number) INCLUDE (part_no);
CREATE INDEX idx_sir_temp_new_job_epic ON sir_temp_new_voters(job_id, epic_number) INCLUDE (part_no);

-- Partial indexes for even faster queries (only index records for active jobs)
CREATE INDEX idx_sir_temp_base_job ON sir_temp_base_voters(job_id);
CREATE INDEX idx_sir_temp_new_job ON sir_temp_new_voters(job_id);

-- Add table for storing full voter details cache (only for results)
CREATE TABLE sir_temp_voter_details_cache (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL,
    epic_number VARCHAR(50) NOT NULL,
    voter_name_en VARCHAR(255),
    part_no INTEGER,
    serial_no BIGINT,
    section_no INTEGER,
    house_no_en VARCHAR(100),
    age INTEGER,
    gender VARCHAR(10),
    source_file VARCHAR(10), -- 'BASE' or 'NEW'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_voter_details_job_epic ON sir_temp_voter_details_cache(job_id, epic_number);

-- Enable parallel query execution on these tables
ALTER TABLE sir_temp_base_voters SET (parallel_workers = 4);
ALTER TABLE sir_temp_new_voters SET (parallel_workers = 4);
