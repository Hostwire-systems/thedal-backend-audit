-- Renamed from V1_9 to V1_11 to resolve Flyway version conflict.
-- Adds dynamic_field_id column to _voters table if missing and creates FK constraint safely.

DO $$
BEGIN
    -- Add column if it does not exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = '_voters' AND column_name = 'dynamic_field_id'
    ) THEN
        ALTER TABLE _voters ADD COLUMN dynamic_field_id BIGINT;
    END IF;

    -- Add constraint if it does not exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = '_voters' AND constraint_name = 'fkkwth8lqfmnbu2jcfxx25lmhu5'
    ) THEN
        ALTER TABLE _voters 
            ADD CONSTRAINT fkkwth8lqfmnbu2jcfxx25lmhu5 FOREIGN KEY (dynamic_field_id) REFERENCES _dynamic_fields (id);
    END IF;
END $$;
