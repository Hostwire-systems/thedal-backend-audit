-- Manual SQL to create volunteer_entity_assigned_families table
-- Run this directly in your PostgreSQL database if the migration hasn't run automatically

-- Create the junction table for volunteer assigned families
CREATE TABLE IF NOT EXISTS volunteer_entity_assigned_families (
    volunteer_entity_id BIGINT NOT NULL,
    assigned_families BIGINT NOT NULL,
    CONSTRAINT fk_volunteer_assigned_families 
        FOREIGN KEY (volunteer_entity_id) 
        REFERENCES volunteers(id) 
        ON DELETE CASCADE
);

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_volunteer_assigned_families_volunteer 
    ON volunteer_entity_assigned_families(volunteer_entity_id);

CREATE INDEX IF NOT EXISTS idx_volunteer_assigned_families_family 
    ON volunteer_entity_assigned_families(assigned_families);

-- Verify the table was created
SELECT 
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE table_name = 'volunteer_entity_assigned_families'
ORDER BY ordinal_position;
