CREATE TABLE IF NOT EXISTS election_dashboard_contact_status (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    contact_status_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    computed_at TIMESTAMPTZ NOT NULL,
    refreshed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_contact_status_account_election UNIQUE (account_id, election_id)
);

CREATE INDEX IF NOT EXISTS idx_contact_status_account_election ON election_dashboard_contact_status(account_id, election_id);
