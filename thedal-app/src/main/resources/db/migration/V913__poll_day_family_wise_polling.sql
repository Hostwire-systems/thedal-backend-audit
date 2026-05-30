-- Migration: V913 - Add poll_day_family_wise_polling table for family-wise polling analysis
-- Description: Creates table to store family-wise polling data with caching for poll day dashboard

CREATE TABLE IF NOT EXISTS poll_day_family_wise_polling (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    polling_date DATE NOT NULL,
    family_wise_data_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    computed_at TIMESTAMPTZ,
    refreshed_at TIMESTAMPTZ,
    CONSTRAINT uq_poll_day_family_wise UNIQUE (account_id, election_id, polling_date)
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_poll_day_family_wise_account_election_date 
    ON poll_day_family_wise_polling(account_id, election_id, polling_date);

CREATE INDEX IF NOT EXISTS idx_poll_day_family_wise_polling_date 
    ON poll_day_family_wise_polling(polling_date);

CREATE INDEX IF NOT EXISTS idx_poll_day_family_wise_refreshed 
    ON poll_day_family_wise_polling(refreshed_at);

-- Add comments
COMMENT ON TABLE poll_day_family_wise_polling IS 'Stores family-wise polling statistics per part number for poll day dashboard';
COMMENT ON COLUMN poll_day_family_wise_polling.polling_date IS 'Polling date for the data. Use 1900-01-01 for all-time cumulative data';
COMMENT ON COLUMN poll_day_family_wise_polling.family_wise_data_json IS 'JSON structure containing family polling statistics per part number';
COMMENT ON COLUMN poll_day_family_wise_polling.computed_at IS 'First time this data was computed';
COMMENT ON COLUMN poll_day_family_wise_polling.refreshed_at IS 'Last time this data was refreshed/recomputed';
