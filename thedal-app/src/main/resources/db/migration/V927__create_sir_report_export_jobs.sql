-- Create sequence for SIR Report Export Jobs
CREATE SEQUENCE IF NOT EXISTS sir_report_export_jobs_seq;

-- Create SIR Report Export Jobs table
CREATE TABLE IF NOT EXISTS sir_report_export_jobs (
    id BIGINT PRIMARY KEY DEFAULT nextval('sir_report_export_jobs_seq'),
    job_id UUID NOT NULL,
    account_id BIGINT NOT NULL,
    election_id BIGINT,
    export_type VARCHAR(50) NOT NULL,
    format VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    message TEXT,
    aws_s3_download_url TEXT,
    record_count INTEGER,
    time_started TIMESTAMP NOT NULL,
    time_completed TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

-- Associate sequence with table column
ALTER SEQUENCE sir_report_export_jobs_seq OWNED BY sir_report_export_jobs.id;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_sir_export_account_election ON sir_report_export_jobs(account_id, election_id);
CREATE INDEX IF NOT EXISTS idx_sir_export_job_id ON sir_report_export_jobs(job_id);
CREATE INDEX IF NOT EXISTS idx_sir_export_expires_at ON sir_report_export_jobs(expires_at);
CREATE INDEX IF NOT EXISTS idx_sir_export_time_started ON sir_report_export_jobs(time_started DESC);
