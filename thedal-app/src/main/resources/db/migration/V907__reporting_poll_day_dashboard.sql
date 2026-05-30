-- Poll Day Dashboard tables (hourly turnout, age group turnout, booth summary)
-- Using India Standard Time (IST, UTC+05:30) semantics for bucketing hours. We store timestamps in DB local time;
-- aggregation will project voted_timestamp into IST hour.

CREATE TABLE IF NOT EXISTS poll_day_hourly_turnout (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    polling_date DATE NOT NULL, -- Local calendar date (IST) of polling
    hourly_json JSONB NOT NULL DEFAULT '{}'::jsonb, -- {"00":{"voted":0},"06":{"voted":25}, ...}
    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    refreshed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_poll_day_hourly UNIQUE (account_id, election_id, polling_date)
);
CREATE INDEX IF NOT EXISTS idx_poll_day_hourly_account_election_date ON poll_day_hourly_turnout(account_id, election_id, polling_date);

CREATE TABLE IF NOT EXISTS poll_day_age_group_turnout (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    polling_date DATE NOT NULL,
    age_groups_json JSONB NOT NULL DEFAULT '{}'::jsonb, -- {"18_30":{"registered":100,"voted":40,"pct":40.0}, ...}
    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    refreshed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_poll_day_age UNIQUE (account_id, election_id, polling_date)
);
CREATE INDEX IF NOT EXISTS idx_poll_day_age_account_election_date ON poll_day_age_group_turnout(account_id, election_id, polling_date);

CREATE TABLE IF NOT EXISTS poll_day_booth_summary (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    polling_date DATE NOT NULL,
    booth_summary_json JSONB NOT NULL DEFAULT '{}'::jsonb, -- {"12":{"total":123,"voted":45,"pct":36.6,"lastVote":"2025-09-03T08:15:00+05:30"}, ...}
    computed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    refreshed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_poll_day_booth_summary UNIQUE (account_id, election_id, polling_date)
);
CREATE INDEX IF NOT EXISTS idx_poll_day_booth_account_election_date ON poll_day_booth_summary(account_id, election_id, polling_date);
