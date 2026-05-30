-- Feedback issues aggregation
CREATE TABLE IF NOT EXISTS election_dashboard_feedback_issues (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    issues_json JSONB DEFAULT '{}'::jsonb NOT NULL, -- {"issueName": count, ...}
    computed_at TIMESTAMPTZ NOT NULL,
    refreshed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_election_dashboard_feedback_issues UNIQUE (account_id, election_id)
);
CREATE INDEX IF NOT EXISTS idx_election_dashboard_feedback_issues_account_election ON election_dashboard_feedback_issues(account_id, election_id);