ALTER TABLE family_voter_card_export_job ADD COLUMN IF NOT EXISTS part_no INT;
ALTER TABLE family_voter_card_export_job ADD COLUMN IF NOT EXISTS columns INT NOT NULL DEFAULT 2;

-- Backfill null columns values to 2 just in case (older rows)
UPDATE family_voter_card_export_job SET columns = 2 WHERE columns IS NULL;

-- Optional: index to speed up part-wide exports
CREATE INDEX IF NOT EXISTS idx_family_export_part ON family_voter_card_export_job(part_no);
