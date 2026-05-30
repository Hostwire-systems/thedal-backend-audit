-- Add part_type column to part_manager table
-- This field stores whether a part is URBAN or RURAL
-- Optional field (nullable)

ALTER TABLE part_manager 
ADD COLUMN IF NOT EXISTS part_type VARCHAR(20) NULL;

-- Add comment to the column
COMMENT ON COLUMN part_manager.part_type IS 'Type of part - URBAN or RURAL';

-- Optional: Create an index if filtering by part type will be common
CREATE INDEX IF NOT EXISTS idx_part_manager_part_type ON part_manager(part_type);
