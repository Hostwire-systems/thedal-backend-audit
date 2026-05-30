-- Migration: V910 - Add poll_day_part_wise_polling table for part-wise polling analysis
-- Description: Creates table to store part-wise polling data with historical comparison for poll day dashboard

CREATE TABLE IF NOT EXISTS poll_day_part_wise_polling (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    polling_date DATE NOT NULL,
    part_wise_data_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    computed_at TIMESTAMPTZ,
    refreshed_at TIMESTAMPTZ,
    CONSTRAINT uq_poll_day_part_wise UNIQUE (account_id, election_id, polling_date)
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_poll_day_part_wise_account_election_date 
    ON poll_day_part_wise_polling(account_id, election_id, polling_date);

CREATE INDEX IF NOT EXISTS idx_poll_day_part_wise_polling_date 
    ON poll_day_part_wise_polling(polling_date);

CREATE INDEX IF NOT EXISTS idx_poll_day_part_wise_refreshed 
    ON poll_day_part_wise_polling(refreshed_at DESC);

-- Comment on table
COMMENT ON TABLE poll_day_part_wise_polling IS 'Stores part-wise polling data with year-over-year comparison for poll day dashboard';

-- Comment on columns
COMMENT ON COLUMN poll_day_part_wise_polling.part_wise_data_json IS 'JSONB containing array of part-wise polling statistics and summary data';
COMMENT ON COLUMN poll_day_part_wise_polling.computed_at IS 'Initial computation timestamp';
COMMENT ON COLUMN poll_day_part_wise_polling.refreshed_at IS 'Last refresh timestamp for cache invalidation';
