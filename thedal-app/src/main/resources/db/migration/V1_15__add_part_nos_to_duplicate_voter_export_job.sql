-- Add a column to store multiple part numbers (CSV) for a single export job
ALTER TABLE duplicate_voter_export_job
    ADD COLUMN IF NOT EXISTS part_nos TEXT;
