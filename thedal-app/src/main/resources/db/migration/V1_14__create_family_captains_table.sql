-- Create family_captains table
CREATE TABLE IF NOT EXISTS family_captains (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255),
    email VARCHAR(255),
    mobile_number VARCHAR(20) NOT NULL,
    
    -- Address fields (embedded)
    street VARCHAR(500),
    city VARCHAR(255),
    state VARCHAR(255),
    postal_code VARCHAR(10),
    country VARCHAR(255),
    
    status VARCHAR(50) DEFAULT 'active',
    photo_url VARCHAR(1000),
    remarks TEXT,
    whats_app_number VARCHAR(20),
    gender VARCHAR(10),
    
    account_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL REFERENCES _user(user_id),
    election_id BIGINT NOT NULL REFERENCES election(id),
    admin_user_id BIGINT,
    
    created_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    UNIQUE(mobile_number, account_id),
    UNIQUE(user_id, election_id)
);

-- Create family_captain_assigned_families table for the @ElementCollection
CREATE TABLE IF NOT EXISTS family_captain_assigned_families (
    family_captain_id BIGINT NOT NULL REFERENCES family_captains(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    
    PRIMARY KEY (family_captain_id, family_id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_family_captain_id ON family_captains(id);
CREATE INDEX IF NOT EXISTS idx_fc_election_account ON family_captains(election_id, account_id);
CREATE INDEX IF NOT EXISTS idx_fc_mobile_number ON family_captains(mobile_number);
CREATE INDEX IF NOT EXISTS idx_fc_user_election ON family_captains(user_id, election_id);

-- Index for assigned families join table
CREATE INDEX IF NOT EXISTS idx_fc_assigned_families_captain_id ON family_captain_assigned_families(family_captain_id);
CREATE INDEX IF NOT EXISTS idx_fc_assigned_families_family_id ON family_captain_assigned_families(family_id);

-- Add comment to document the table
COMMENT ON TABLE family_captains IS 'Family Captain management system - stores captains assigned to manage specific families within elections';
COMMENT ON TABLE family_captain_assigned_families IS 'Junction table storing family assignments for family captains';
