-- Add part_image_url column to part_manager table for storing booth/part photo URLs

ALTER TABLE part_manager 
ADD COLUMN IF NOT EXISTS part_image_url VARCHAR(500);

COMMENT ON COLUMN part_manager.part_image_url IS 'S3 URL of the booth/part image uploaded by users';
