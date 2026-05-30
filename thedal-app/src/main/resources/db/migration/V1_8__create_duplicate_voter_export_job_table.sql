CREATE TABLE IF NOT EXISTS duplicate_voter_export_job (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES voter_duplicate_run(id) ON DELETE CASCADE,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    part_no INTEGER,
    status VARCHAR(20) NOT NULL,
    s3_key VARCHAR(255),
    s3_url TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    row_count BIGINT,
    CONSTRAINT chk_dup_export_status CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_dup_export_run ON duplicate_voter_export_job(run_id);
CREATE INDEX IF NOT EXISTS idx_dup_export_status ON duplicate_voter_export_job(status, created_at DESC);
