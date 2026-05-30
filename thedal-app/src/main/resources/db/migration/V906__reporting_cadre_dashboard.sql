-- Cadre dashboard aggregate tables
-- One row per (account_id, election_id) capturing overall cadre stats and JSON payloads for performance lists.

CREATE TABLE IF NOT EXISTS cadre_dashboard_stats (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,

    total_cadres INTEGER NOT NULL DEFAULT 0,
    cadres_logged INTEGER NOT NULL DEFAULT 0,           -- users who have created/updated at least one voter (proxy from volunteer_vs_voter_report total_voter_created/updated > 0)
    cadres_not_logged INTEGER NOT NULL DEFAULT 0,       -- derived = total_cadres - cadres_logged
    booths_assigned INTEGER NOT NULL DEFAULT 0,         -- count of distinct volunteer->booth assignments (list length aggregated)

    total_mobile_updated INTEGER NOT NULL DEFAULT 0,
    total_dob_updated INTEGER NOT NULL DEFAULT 0,
    total_party_updated INTEGER NOT NULL DEFAULT 0,
    total_caste_updated INTEGER NOT NULL DEFAULT 0,
    total_religion_updated INTEGER NOT NULL DEFAULT 0,
    total_language_updated INTEGER NOT NULL DEFAULT 0,  -- placeholder (language concept if present later)

    top_10_cadres JSONB DEFAULT '[]',   -- array of { "userId": <id>, "value": <votersCreated> }
    least_10_cadres JSONB DEFAULT '[]',

    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    refreshed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_cadre_dashboard_stats UNIQUE (account_id, election_id)
);

CREATE INDEX IF NOT EXISTS idx_cadre_dashboard_stats_account_election
    ON cadre_dashboard_stats (account_id, election_id);

-- Notes / assumptions:
-- 1. cadres_logged derived from volunteer_vs_voter_report rows with (total_voter_created + total_voter_updated) > 0 for election/account.
-- 2. booths_assigned counts total distinct volunteer->booth assignments by summing array_length(assigned_booth) in volunteers table.
-- 3. Attribute *updated counts are SUM of volunteer_vs_voter_report *_updated columns.
-- 4. top_10 / least_10 ordering uses total_voter_created (fallback 0) as engagement metric.
-- 5. language_updated currently always 0 until source attribute available.
-- Future refinement: introduce separate table for performance history if temporal trend charts needed.
