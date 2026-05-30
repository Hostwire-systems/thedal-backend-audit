-- Migration: create tables for merge job feature
-- Creates merge_jobs and merge_job_fields to store merge job metadata and selected fields.

CREATE TABLE IF NOT EXISTS merge_jobs (
    id UUID PRIMARY KEY,
    account_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    source_election_id BIGINT NOT NULL,
    target_election_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_voters BIGINT,
    processed_voters BIGINT,
    -- Use BYTEA for LOB style storage (initial version) - later migrations may convert to TEXT
    result_stats BYTEA,
    error_message VARCHAR(512),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    finished_at TIMESTAMP WITHOUT TIME ZONE
);

-- If table already existed from a previous start with TEXT column, adapt to BYTEA (PostgreSQL specific)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name='merge_jobs' AND column_name='result_stats' AND data_type='text'
    ) THEN
        ALTER TABLE merge_jobs ALTER COLUMN result_stats TYPE BYTEA USING result_stats::bytea;
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS merge_job_fields (
    job_id UUID NOT NULL,
    field VARCHAR(64) NOT NULL,
    CONSTRAINT fk_merge_job_fields_job FOREIGN KEY (job_id) REFERENCES merge_jobs(id) ON DELETE CASCADE
);

-- Helpful indexes for lookups & concurrency checks
CREATE INDEX IF NOT EXISTS idx_merge_jobs_target_status ON merge_jobs (target_election_id, status);
CREATE INDEX IF NOT EXISTS idx_merge_jobs_account_created ON merge_jobs (account_id, created_at DESC);
