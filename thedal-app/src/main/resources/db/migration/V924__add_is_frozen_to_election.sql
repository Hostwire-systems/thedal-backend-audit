-- Add is_frozen column to election table for freeze/unfreeze functionality
-- This allows admin to freeze an election making it read-only for all users

ALTER TABLE election 
ADD COLUMN IF NOT EXISTS is_frozen BOOLEAN;

-- Update existing rows to set is_frozen to false
UPDATE election 
SET is_frozen = FALSE 
WHERE is_frozen IS NULL;

-- Now make the column NOT NULL with default
ALTER TABLE election 
ALTER COLUMN is_frozen SET NOT NULL,
ALTER COLUMN is_frozen SET DEFAULT FALSE;

-- Add comment to explain the column
COMMENT ON COLUMN election.is_frozen IS 'Indicates if the election is frozen (read-only). When true, no data modifications are allowed but data can still be viewed.';
