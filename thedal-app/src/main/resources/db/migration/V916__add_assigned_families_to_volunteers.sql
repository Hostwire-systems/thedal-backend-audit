-- Add assigned families support to volunteers
-- This allows assigning specific families to volunteers for monitoring/tracking

-- Create the junction table for volunteer assigned families
-- This table is used by JPA @ElementCollection annotation
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
