-- Reintroduced at correct version (1.9) so later V1_10 alter migration can succeed.
-- Creates family_voter_card_export_job table and supporting indexes.

CREATE TABLE IF NOT EXISTS family_voter_card_export_job (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    family_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    s3_key VARCHAR(255),
    s3_url TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    row_count BIGINT,
    CONSTRAINT chk_family_export_status CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_family_export_account ON family_voter_card_export_job(account_id, election_id, family_id);
CREATE INDEX IF NOT EXISTS idx_family_export_status ON family_voter_card_export_job(status, created_at DESC);