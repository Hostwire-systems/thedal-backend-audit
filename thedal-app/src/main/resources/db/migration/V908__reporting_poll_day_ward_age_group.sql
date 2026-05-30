-- Migration: V908 - Add poll_day_ward_age_group_turnout table for ward-specific age group analysis
-- Description: Creates table to store ward/part-specific age group turnout data with historical comparison

CREATE TABLE IF NOT EXISTS poll_day_ward_age_group_turnout (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    part_number VARCHAR(50) NOT NULL,
    polling_date DATE NOT NULL,
    age_group_breakdown_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    computed_at TIMESTAMPTZ,
    refreshed_at TIMESTAMPTZ,
    CONSTRAINT uq_poll_day_ward_age_group UNIQUE (account_id, election_id, part_number, polling_date)
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_poll_day_ward_age_account_election_part_date 
    ON poll_day_ward_age_group_turnout(account_id, election_id, part_number, polling_date);

CREATE INDEX IF NOT EXISTS idx_poll_day_ward_age_polling_date 
    ON poll_day_ward_age_group_turnout(polling_date);

-- Comment on table
COMMENT ON TABLE poll_day_ward_age_group_turnout IS 'Stores ward/part-specific age group turnout data with historical year-over-year comparison for poll day dashboard';

-- Comment on columns
COMMENT ON COLUMN poll_day_ward_age_group_turnout.part_number IS 'Ward/Part number from voter records';
COMMENT ON COLUMN poll_day_ward_age_group_turnout.age_group_breakdown_json IS 'JSONB containing age group statistics with categories: 18_21, 22_25, 26_35, 36_45, 46_59, expired, and overall summary';
