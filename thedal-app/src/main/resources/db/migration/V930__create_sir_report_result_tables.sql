-- Create tables to store SIR comparison results (additions, deletions, shifts)
-- These tables store the results of comparing base and new voter lists

-- Table for new voters (additions) - voters in new list but not in base
CREATE TABLE IF NOT EXISTS sir_report_additions (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL,
    epic_number VARCHAR(50) NOT NULL,
    part_no INTEGER,
    voter_name_en VARCHAR(255),
    serial_no BIGINT,
    section_no INTEGER,
    house_no_en VARCHAR(100),
    age INTEGER,
    gender VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for deleted voters - voters in base list but not in new list
CREATE TABLE IF NOT EXISTS sir_report_deletions (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL,
    epic_number VARCHAR(50) NOT NULL,
    part_no INTEGER,
    voter_name_en VARCHAR(255),
    serial_no BIGINT,
    section_no INTEGER,
    house_no_en VARCHAR(100),
    age INTEGER,
    gender VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for shifted voters - voters in both lists but with different part numbers
CREATE TABLE IF NOT EXISTS sir_report_shifts (
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
    gender VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for fast job-based queries
CREATE INDEX IF NOT EXISTS idx_sir_additions_job ON sir_report_additions(job_id);
CREATE INDEX IF NOT EXISTS idx_sir_additions_epic ON sir_report_additions(epic_number);
CREATE INDEX IF NOT EXISTS idx_sir_additions_part ON sir_report_additions(part_no);

CREATE INDEX IF NOT EXISTS idx_sir_deletions_job ON sir_report_deletions(job_id);
CREATE INDEX IF NOT EXISTS idx_sir_deletions_epic ON sir_report_deletions(epic_number);
CREATE INDEX IF NOT EXISTS idx_sir_deletions_part ON sir_report_deletions(part_no);

CREATE INDEX IF NOT EXISTS idx_sir_shifts_job ON sir_report_shifts(job_id);
CREATE INDEX IF NOT EXISTS idx_sir_shifts_epic ON sir_report_shifts(epic_number);
CREATE INDEX IF NOT EXISTS idx_sir_shifts_old_part ON sir_report_shifts(old_part_no);
CREATE INDEX IF NOT EXISTS idx_sir_shifts_new_part ON sir_report_shifts(new_part_no);

-- Composite index for efficient cleanup by job
CREATE INDEX IF NOT EXISTS idx_sir_additions_job_created ON sir_report_additions(job_id, created_at);
CREATE INDEX IF NOT EXISTS idx_sir_deletions_job_created ON sir_report_deletions(job_id, created_at);
CREATE INDEX IF NOT EXISTS idx_sir_shifts_job_created ON sir_report_shifts(job_id, created_at);
