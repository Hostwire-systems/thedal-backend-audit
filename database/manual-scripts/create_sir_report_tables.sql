-- SIR Report Tables Migration
-- Create tables for Supplement, Inclusion, Rejection (SIR) report feature

-- Main job tracking table
CREATE TABLE IF NOT EXISTS sir_report_job (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL UNIQUE,
    account_id BIGINT NOT NULL,
    election_id BIGINT,
    status VARCHAR(20) NOT NULL,
    total_base_records INTEGER,
    total_new_records INTEGER,
    additions_count INTEGER,
    deletions_count INTEGER,
    shifts_count INTEGER,
    base_file_name VARCHAR(255),
    new_file_name VARCHAR(255),
    progress INTEGER DEFAULT 0,
    message VARCHAR(500),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sir_job_account_election ON sir_report_job(account_id, election_id);
CREATE INDEX IF NOT EXISTS idx_sir_job_id ON sir_report_job(job_id);

-- Addition records (new voters)
CREATE TABLE IF NOT EXISTS sir_report_addition (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL,
    epic_number VARCHAR(50) NOT NULL,
    part_no INTEGER,
    voter_name_en VARCHAR(255),
    serial_no BIGINT,
    section_no INTEGER,
    house_no_en VARCHAR(100),
    age INTEGER,
    gender VARCHAR(10)
);

CREATE INDEX IF NOT EXISTS idx_sir_addition_job ON sir_report_addition(job_id);
CREATE INDEX IF NOT EXISTS idx_sir_addition_epic ON sir_report_addition(epic_number);

-- Deletion records (removed voters)
CREATE TABLE IF NOT EXISTS sir_report_deletion (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL,
    epic_number VARCHAR(50) NOT NULL,
    part_no INTEGER,
    voter_name_en VARCHAR(255),
    serial_no BIGINT,
    section_no INTEGER,
    house_no_en VARCHAR(100),
    age INTEGER,
    gender VARCHAR(10)
);

CREATE INDEX IF NOT EXISTS idx_sir_deletion_job ON sir_report_deletion(job_id);
CREATE INDEX IF NOT EXISTS idx_sir_deletion_epic ON sir_report_deletion(epic_number);

-- Shift records (voters moved to different parts)
CREATE TABLE IF NOT EXISTS sir_report_shift (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL,
    epic_number VARCHAR(50) NOT NULL,
    old_part_no INTEGER,
    new_part_no INTEGER,
    voter_name_en VARCHAR(255),
    serial_no BIGINT,
    section_no INTEGER,
    house_no_en VARCHAR(100),
    age INTEGER,
    gender VARCHAR(10)
);

CREATE INDEX IF NOT EXISTS idx_sir_shift_job ON sir_report_shift(job_id);
CREATE INDEX IF NOT EXISTS idx_sir_shift_epic ON sir_report_shift(epic_number);

-- Comments
COMMENT ON TABLE sir_report_job IS 'Tracks SIR report comparison jobs';
COMMENT ON TABLE sir_report_addition IS 'Stores newly added voters (in new file but not in base)';
COMMENT ON TABLE sir_report_deletion IS 'Stores deleted voters (in base file but not in new)';
COMMENT ON TABLE sir_report_shift IS 'Stores voters who moved to different polling booths';
