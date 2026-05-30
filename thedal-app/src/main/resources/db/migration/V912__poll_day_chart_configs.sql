-- Create poll_day_chart_configs table for storing user's chart preferences
CREATE TABLE poll_day_chart_configs (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    charts JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_account_election_chart_config UNIQUE(account_id, election_id)
);

-- Create index for faster lookups
CREATE INDEX idx_chart_config_account_election ON poll_day_chart_configs(account_id, election_id);

-- Add comment to the table
COMMENT ON TABLE poll_day_chart_configs IS 'Stores user preferences for Poll Day Dashboard chart configurations';
COMMENT ON COLUMN poll_day_chart_configs.charts IS 'JSONB array of chart configurations: [{chartId: string, selectedParts: number[]}]';
