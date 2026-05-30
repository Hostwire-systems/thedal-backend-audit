-- Migration: V915 - Add export_type column to family_voter_card_export_job table
-- Description: Adds export_type column to support both PDF and Excel exports, and add orderBy column

ALTER TABLE family_voter_card_export_job 
    ADD COLUMN IF NOT EXISTS export_type VARCHAR(20) NOT NULL DEFAULT 'PDF';

ALTER TABLE family_voter_card_export_job 
    ADD CONSTRAINT chk_family_export_type CHECK (export_type IN ('PDF', 'EXCEL'));

-- Add index for querying by export type
CREATE INDEX IF NOT EXISTS idx_family_export_type ON family_voter_card_export_job(export_type);

-- Add orderBy column if it doesn't exist (may already exist from previous migration)
ALTER TABLE family_voter_card_export_job 
    ADD COLUMN IF NOT EXISTS order_by VARCHAR(20) NOT NULL DEFAULT 'family';

COMMENT ON COLUMN family_voter_card_export_job.export_type IS 'Type of export: PDF (voter cards) or EXCEL (spreadsheet)';
COMMENT ON COLUMN family_voter_card_export_job.order_by IS 'Ordering preference: family (by family_sequence_number) or serial (by serialNo)';
