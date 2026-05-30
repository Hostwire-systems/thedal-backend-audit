-- Create campaigns table for campaign management system
-- Supports both SMS and WhatsApp campaigns with JSON data storage

CREATE TABLE campaigns (
    id VARCHAR(36) PRIMARY KEY,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('sms', 'whatsapp')),
    title VARCHAR(255) NOT NULL,
    sender_id VARCHAR(100),
    language VARCHAR(10),
    content_html TEXT,
    buttons_json TEXT,
    media_json TEXT,
    tags_json TEXT,
    filters_json TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('draft', 'scheduled', 'sending', 'sent', 'failed')),
    recipients_count BIGINT,
    created_at TIMESTAMP WITH TIME ZONE,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    db_created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    db_updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indices for better query performance
CREATE INDEX idx_campaigns_channel ON campaigns(channel);
CREATE INDEX idx_campaigns_status ON campaigns(status);
CREATE INDEX idx_campaigns_created_at ON campaigns(created_at);
CREATE INDEX idx_campaigns_title_search ON campaigns USING gin(to_tsvector('english', title));

-- Comment on table and key columns
COMMENT ON TABLE campaigns IS 'Campaign management table for SMS and WhatsApp campaigns';
COMMENT ON COLUMN campaigns.buttons_json IS 'JSON array of campaign buttons (WhatsApp only)';
COMMENT ON COLUMN campaigns.media_json IS 'JSON object of campaign media (WhatsApp only)';
COMMENT ON COLUMN campaigns.tags_json IS 'JSON array of campaign tags';
COMMENT ON COLUMN campaigns.filters_json IS 'JSON object of campaign filters for recipient selection';