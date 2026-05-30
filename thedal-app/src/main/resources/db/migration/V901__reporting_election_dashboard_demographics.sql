-- Demographics aggregation table
CREATE TABLE IF NOT EXISTS election_dashboard_demographics (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    caste_json JSONB DEFAULT '{}'::jsonb NOT NULL,
    religion_json JSONB DEFAULT '{}'::jsonb NOT NULL,
    language_json JSONB DEFAULT '{}'::jsonb NOT NULL,
    relation_json JSONB DEFAULT '{}'::jsonb NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL,
    refreshed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_election_dashboard_demographics UNIQUE (account_id, election_id)
);
CREATE INDEX IF NOT EXISTS idx_election_dashboard_demographics_account_election ON election_dashboard_demographics(account_id, election_id);