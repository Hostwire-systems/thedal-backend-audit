-- Create bulk_photo_upload table for tracking voter photo upload operations
CREATE TABLE IF NOT EXISTS bulk_photo_upload (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    total_photos INTEGER DEFAULT 0,
    successful_uploads INTEGER DEFAULT 0,
    failed_uploads INTEGER DEFAULT 0,
    voters_not_found INTEGER DEFAULT 0,
    zip_file_name VARCHAR(255),
    uploaded_by VARCHAR(255),
    error_details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_bulk_photo_upload_account_election 
    ON bulk_photo_upload (account_id, election_id);

CREATE INDEX IF NOT EXISTS idx_bulk_photo_upload_status 
    ON bulk_photo_upload (status);

CREATE INDEX IF NOT EXISTS idx_bulk_photo_upload_start_time 
    ON bulk_photo_upload (start_time DESC);
