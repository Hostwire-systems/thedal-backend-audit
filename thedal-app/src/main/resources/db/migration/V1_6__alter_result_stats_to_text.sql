-- Migration: Normalize result_stats column to TEXT
-- Some environments created V1_5 with result_stats as BYTEA. Entity now maps it as TEXT.
-- This migration conditionally alters the column type only if it's currently BYTEA.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='merge_jobs'
          AND column_name='result_stats'
          AND data_type='bytea'
    ) THEN
        -- Convert binary contents to UTF8 text. If non-textual data was stored, this may fail.
        ALTER TABLE merge_jobs
            ALTER COLUMN result_stats TYPE TEXT
            USING convert_from(result_stats, 'UTF8');
    END IF;
END $$;

-- No-op if already TEXT.