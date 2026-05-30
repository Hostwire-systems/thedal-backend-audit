-- Aggregate stats for election dashboard (Phase 1 reporting)
-- Fresh table to avoid altering existing structures.
-- One row per (account_id, election_id).

CREATE TABLE IF NOT EXISTS election_dashboard_stats (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,

    total_booth INTEGER NOT NULL DEFAULT 0,
    total_voters INTEGER NOT NULL DEFAULT 0,
    total_family INTEGER NOT NULL DEFAULT 0,
    distinct_pincode_count INTEGER NOT NULL DEFAULT 0,
    distinct_mobile_count INTEGER NOT NULL DEFAULT 0,

    male INTEGER NOT NULL DEFAULT 0,
    female INTEGER NOT NULL DEFAULT 0,
    transgender INTEGER NOT NULL DEFAULT 0,

    age_18_30 INTEGER NOT NULL DEFAULT 0,
    age_30_40 INTEGER NOT NULL DEFAULT 0,
    age_40_50 INTEGER NOT NULL DEFAULT 0,
    age_50_60 INTEGER NOT NULL DEFAULT 0,
    age_60_70 INTEGER NOT NULL DEFAULT 0,
    age_gt_70 INTEGER NOT NULL DEFAULT 0,

    first_time_voters INTEGER NOT NULL DEFAULT 0,  -- optional (18-21) derivation
    senior_citizens INTEGER NOT NULL DEFAULT 0,     -- e.g. 60-70
    super_seniors INTEGER NOT NULL DEFAULT 0,       -- e.g. >70 or >80 depending on definition

    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    refreshed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_election_dashboard_stats UNIQUE (account_id, election_id)
);

CREATE INDEX IF NOT EXISTS idx_election_dashboard_stats_account_election
    ON election_dashboard_stats (account_id, election_id);

-- Future extension tables (not created yet):
-- election_gender_stats, election_age_band_stats, election_party_stats, election_issue_stats, etc.
-- Added later with their own migrations when needed.
