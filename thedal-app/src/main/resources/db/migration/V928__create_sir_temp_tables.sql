-- Create temporary table for base voter data during SIR comparison
CREATE TABLE IF NOT EXISTS sir_temp_base_voters (
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create temporary table for new voter data during comparison
CREATE TABLE IF NOT EXISTS sir_temp_new_voters (
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for fast lookups and joins
CREATE INDEX IF NOT EXISTS idx_sir_temp_base_job_epic ON sir_temp_base_voters(job_id, epic_number);
CREATE INDEX IF NOT EXISTS idx_sir_temp_base_cleanup ON sir_temp_base_voters(created_at);

CREATE INDEX IF NOT EXISTS idx_sir_temp_new_job_epic ON sir_temp_new_voters(job_id, epic_number);
CREATE INDEX IF NOT EXISTS idx_sir_temp_new_cleanup ON sir_temp_new_voters(created_at);
