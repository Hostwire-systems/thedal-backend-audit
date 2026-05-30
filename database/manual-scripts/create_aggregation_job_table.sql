-- Migration: Create aggregation_job table for async job tracking
-- Purpose: Track async election aggregation jobs with progress and status
-- Performance: Indexed on jobId and account+election for fast lookups
-- Date: 2024

-- Create aggregation_job table
CREATE TABLE IF NOT EXISTS aggregation_job (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(50) NOT NULL UNIQUE,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    part_number VARCHAR(10),
    total_parts INTEGER DEFAULT 0,
    completed_parts INTEGER DEFAULT 0,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    error_message TEXT,
    cancelled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for fast lookups
CREATE INDEX IF NOT EXISTS idx_aggregation_job_job_id ON aggregation_job(job_id);
CREATE INDEX IF NOT EXISTS idx_aggregation_job_account_election ON aggregation_job(account_id, election_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_aggregation_job_status ON aggregation_job(status, started_at DESC);

-- Add comments
COMMENT ON TABLE aggregation_job IS 'Tracks async aggregation jobs with progress and status';
COMMENT ON COLUMN aggregation_job.job_id IS 'Unique job identifier returned to client';
COMMENT ON COLUMN aggregation_job.job_type IS 'Type of aggregation: ELECTION_STATS, DEMOGRAPHICS, etc';
COMMENT ON COLUMN aggregation_job.status IS 'Job status: QUEUED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED';
COMMENT ON COLUMN aggregation_job.part_number IS 'Specific part number if single part job, null for full election';
COMMENT ON COLUMN aggregation_job.total_parts IS 'Total number of parts to process';
COMMENT ON COLUMN aggregation_job.completed_parts IS 'Number of parts completed so far';

-- Verification query
SELECT 
    COUNT(*) as total_jobs,
    status,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') as completed,
    COUNT(*) FILTER (WHERE status = 'FAILED') as failed,
    COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') as in_progress
FROM aggregation_job
GROUP BY status;

-- Sample query to find recent jobs
-- SELECT * FROM aggregation_job 
-- WHERE account_id = ? AND election_id = ? 
-- ORDER BY started_at DESC LIMIT 10;

-- Sample query to find stale jobs (running > 1 hour)
-- SELECT * FROM aggregation_job 
-- WHERE status = 'IN_PROGRESS' 
-- AND started_at < NOW() - INTERVAL '1 hour';

-- Rollback script (run this to undo migration)
/*
DROP TABLE IF EXISTS aggregation_job CASCADE;
*/
