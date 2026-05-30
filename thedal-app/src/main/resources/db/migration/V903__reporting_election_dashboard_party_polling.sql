-- Party polling aggregation
CREATE TABLE IF NOT EXISTS election_dashboard_party_polling (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    party_counts_json JSONB DEFAULT '{}'::jsonb NOT NULL, -- {"<partyId>": count, "unknown": n}
    computed_at TIMESTAMPTZ NOT NULL,
    refreshed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_election_dashboard_party_polling UNIQUE (account_id, election_id)
);
CREATE INDEX IF NOT EXISTS idx_election_dashboard_party_polling_account_election ON election_dashboard_party_polling(account_id, election_id);