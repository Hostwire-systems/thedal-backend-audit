-- Create election_freeze_otp table for storing OTPs for freeze/unfreeze operations

CREATE TABLE IF NOT EXISTS election_freeze_otp (
    id BIGSERIAL PRIMARY KEY,
    election_id BIGINT NOT NULL,
    mobile_number VARCHAR(20) NOT NULL,
    otp VARCHAR(10) NOT NULL,
    action VARCHAR(10) NOT NULL CHECK (action IN ('FREEZE', 'UNFREEZE')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    user_id BIGINT,
    CONSTRAINT fk_election_freeze_otp_user FOREIGN KEY (user_id) REFERENCES _user(user_id) ON DELETE SET NULL
);

-- Add indexes for faster lookups
CREATE INDEX IF NOT EXISTS idx_election_freeze_otp_election_id ON election_freeze_otp(election_id);
CREATE INDEX IF NOT EXISTS idx_election_freeze_otp_is_active ON election_freeze_otp(is_active);
CREATE INDEX IF NOT EXISTS idx_election_freeze_otp_expires_at ON election_freeze_otp(expires_at);

-- Add comment
COMMENT ON TABLE election_freeze_otp IS 'Stores OTPs for election freeze/unfreeze operations';
