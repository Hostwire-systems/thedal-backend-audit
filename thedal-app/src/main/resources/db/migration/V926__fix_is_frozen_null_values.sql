-- Fix null values in is_frozen column for existing elections
-- Set all null values to false as the default

UPDATE election 
SET is_frozen = FALSE 
WHERE is_frozen IS NULL;

-- Ensure the column has proper constraints
ALTER TABLE election 
ALTER COLUMN is_frozen SET NOT NULL,
ALTER COLUMN is_frozen SET DEFAULT FALSE;
