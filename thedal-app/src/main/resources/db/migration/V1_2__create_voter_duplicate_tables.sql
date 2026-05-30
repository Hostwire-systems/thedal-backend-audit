-- Schema for persisted voter duplicate detection runs (moved from duplicate V1_3 to unique V1_9)
-- Creates: voter_duplicate_run, voter_duplicate_group, voter_duplicate_member
-- Also adds an index on normalized name fields in _voters to speed matching (age ignored)

-- 1) Runs table
CREATE TABLE IF NOT EXISTS voter_duplicate_run (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('BATCH','ELECTION')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED')),
    started_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP WITHOUT TIME ZONE NULL,
    triggered_by BIGINT NULL,
    bulk_upload_id BIGINT NULL,
    cooldown_until TIMESTAMP WITHOUT TIME ZONE NULL
);

CREATE INDEX IF NOT EXISTS idx_dup_run_account_election_started
    ON voter_duplicate_run (account_id, election_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_dup_run_scope_status
    ON voter_duplicate_run (scope, status, started_at DESC);

-- 2) Groups table
CREATE TABLE IF NOT EXISTS voter_duplicate_group (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES voter_duplicate_run(id) ON DELETE CASCADE,
    voter_fname_en_norm TEXT,
    voter_lname_en_norm TEXT,
    rln_fname_en_norm TEXT,
    rln_lname_en_norm TEXT,
    key_hash TEXT NOT NULL,
    size INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dup_group_run ON voter_duplicate_group (run_id);
CREATE INDEX IF NOT EXISTS idx_dup_group_keyhash ON voter_duplicate_group (key_hash);
CREATE INDEX IF NOT EXISTS idx_dup_group_size_desc ON voter_duplicate_group (size DESC);

-- 3) Members table
CREATE TABLE IF NOT EXISTS voter_duplicate_member (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES voter_duplicate_group(id) ON DELETE CASCADE,
    voter_id BIGINT NOT NULL REFERENCES _voters(id) ON DELETE CASCADE,
    part_no INTEGER NULL,
    serial_no BIGINT NULL
);

CREATE INDEX IF NOT EXISTS idx_dup_member_group ON voter_duplicate_member (group_id);
CREATE INDEX IF NOT EXISTS idx_dup_member_voter ON voter_duplicate_member (voter_id);

-- 4) Support index on normalized names in _voters to accelerate grouping (ignore age)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE schemaname = ANY (current_schemas(true)) AND indexname = 'idx_voters_norm_names'
    ) THEN
        EXECUTE 'CREATE INDEX idx_voters_norm_names ON _voters (
            account_id,
            election_id,
            lower(trim(voter_fname_en)),
            lower(trim(voter_lname_en)),
            lower(trim(rln_fname_en)),
            lower(trim(rln_lname_en))
        )';
    END IF;
END $$;
