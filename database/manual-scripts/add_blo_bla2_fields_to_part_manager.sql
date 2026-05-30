-- Migration script to add BLO and BLA-2 fields to part_manager table
-- Date: 2025-11-14
-- Description: Add optional fields for Booth Level Officer (BLO) and Booth Level Agent-2 (BLA-2) information

-- Add BLO fields
ALTER TABLE part_manager 
ADD COLUMN IF NOT EXISTS blo_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS blo_designation VARCHAR(255),
ADD COLUMN IF NOT EXISTS blo_mobile_number VARCHAR(20);

-- Add BLA-2 fields
ALTER TABLE part_manager 
ADD COLUMN IF NOT EXISTS bla2_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS bla2_designation VARCHAR(255),
ADD COLUMN IF NOT EXISTS bla2_mobile_number VARCHAR(20);

-- Add comments for documentation
COMMENT ON COLUMN part_manager.blo_name IS 'Booth Level Officer Name';
COMMENT ON COLUMN part_manager.blo_designation IS 'Booth Level Officer Designation';
COMMENT ON COLUMN part_manager.blo_mobile_number IS 'Booth Level Officer Mobile Number';
COMMENT ON COLUMN part_manager.bla2_name IS 'Booth Level Agent-2 Name';
COMMENT ON COLUMN part_manager.bla2_designation IS 'Booth Level Agent-2 Designation';
COMMENT ON COLUMN part_manager.bla2_mobile_number IS 'Booth Level Agent-2 Mobile Number';
