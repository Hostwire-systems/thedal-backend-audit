-- Add unique constraint to ensure dynamic field 'name' is unique per account + election (case-insensitive)
-- and supporting index for active status filtering.
-- Safe because max 5 rows per (account,election); adjust names if already duplicate manually before applying.

-- Data cleanup: standardize and de-duplicate existing names before enforcing uniqueness.
-- 1. Trim whitespace
UPDATE _dynamic_fields SET name = trim(name) WHERE name IS NOT NULL;

-- 2. Replace NULL or blank names with placeholder 'field_<id>'
UPDATE _dynamic_fields
SET name = concat('field_', id)
WHERE name IS NULL OR trim(name) = '';

-- 3. De-duplicate (case-insensitive). For any duplicates of (account,election,lower(name)), keep first, rename others by suffixing _<id>
WITH ranked AS (
    SELECT id, account_id, election_id, name,
           row_number() OVER (PARTITION BY account_id, election_id, lower(name) ORDER BY id) rn
    FROM _dynamic_fields
)
UPDATE _dynamic_fields f
SET name = concat(f.name, '_', f.id)
FROM ranked r
WHERE f.id = r.id AND r.rn > 1;  -- only adjust duplicates beyond the first occurrence (qualify f.name to avoid ambiguity)

-- 4. Enforce non-blank going forward
ALTER TABLE _dynamic_fields
    ADD CONSTRAINT chk_dynamic_field_name_not_blank CHECK (name IS NOT NULL AND length(trim(name)) > 0);

-- 5. Create functional unique index (case-insensitive uniqueness per account/election)
CREATE UNIQUE INDEX IF NOT EXISTS uq_dynamic_field_name_per_election
    ON _dynamic_fields (account_id, election_id, lower(name));

-- Partial index to speed lookups of active dynamic fields
CREATE INDEX IF NOT EXISTS idx_dynamic_fields_active
    ON _dynamic_fields (account_id, election_id)
    WHERE status = TRUE;
