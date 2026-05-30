-- Booth progress aggregation: one row per (account,election)
CREATE TABLE IF NOT EXISTS election_dashboard_booth_progress (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    booth_progress_json JSONB DEFAULT '{}'::jsonb NOT NULL, -- {"12":{"total":123,"voted":45}, ...}
    computed_at TIMESTAMPTZ NOT NULL,
    refreshed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_election_dashboard_booth_progress UNIQUE (account_id, election_id)
);
CREATE INDEX IF NOT EXISTS idx_election_dashboard_booth_progress_account_election ON election_dashboard_booth_progress(account_id, election_id);