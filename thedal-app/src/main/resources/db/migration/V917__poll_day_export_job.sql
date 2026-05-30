-- Create poll_day_export_job table for tracking export jobs
CREATE TABLE poll_day_export_job (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    format VARCHAR(10) NOT NULL CHECK (format IN ('excel', 'pdf')),
    chart_type VARCHAR(20) NOT NULL CHECK (chart_type IN ('voterCount', 'familyCount')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    selected_parts JSONB NOT NULL DEFAULT '[]',
    polling_date DATE,
    filters JSONB NOT NULL DEFAULT '{}',
    s3_url TEXT,
    row_count INTEGER,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP WITH TIME ZONE
);

-- Create index for faster lookups by account and election
CREATE INDEX idx_poll_day_export_account_election ON poll_day_export_job(account_id, election_id);

-- Create index for status lookups
CREATE INDEX idx_poll_day_export_status ON poll_day_export_job(status);

-- Create index for created_at to support cleanup operations
CREATE INDEX idx_poll_day_export_created_at ON poll_day_export_job(created_at);

-- Add comments
COMMENT ON TABLE poll_day_export_job IS 'Tracks async export jobs for Poll Day Chart data (voters/families)';
COMMENT ON COLUMN poll_day_export_job.format IS 'Export format: excel or pdf';
COMMENT ON COLUMN poll_day_export_job.chart_type IS 'Type of data to export: voterCount or familyCount';
COMMENT ON COLUMN poll_day_export_job.selected_parts IS 'Array of part numbers to include in export';
COMMENT ON COLUMN poll_day_export_job.polling_date IS 'Optional date to include polling status';
COMMENT ON COLUMN poll_day_export_job.filters IS 'JSON object containing filter criteria (parties, religions, ages, etc.)';
COMMENT ON COLUMN poll_day_export_job.s3_url IS 'S3 URL of generated file (set when status=COMPLETED)';
COMMENT ON COLUMN poll_day_export_job.row_count IS 'Total number of records exported';
