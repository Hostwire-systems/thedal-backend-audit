-- Fix family sequence numbers for account 9842505911, election 133
-- Assign sequence numbers based on modified_time (when families were created/updated)
-- Earlier modified_time gets lower sequence number

-- STEP 1: Check how many families and voters will be updated
SELECT 
    COUNT(DISTINCT family_id) as total_families,
    COUNT(*) as total_voters_to_update
FROM voter_entity
WHERE account_id = 9842505911
  AND election_id = 133
  AND family_id IS NOT NULL;

-- STEP 2: Preview what the new sequence numbers will be
WITH numbered_families AS (
    SELECT DISTINCT 
        family_id,
        MIN(modified_time) as family_modified_time,
        ROW_NUMBER() OVER (ORDER BY MIN(modified_time) ASC) as new_seq
    FROM voter_entity
    WHERE account_id = 9842505911
      AND election_id = 133
      AND family_id IS NOT NULL
    GROUP BY family_id
)
SELECT 
    nf.family_id,
    v.family_sequence_number as current_sequence,
    nf.new_seq as new_sequence,
    nf.family_modified_time,
    COUNT(*) as family_size
FROM numbered_families nf
JOIN voter_entity v ON v.family_id = nf.family_id 
    AND v.account_id = 9842505911 
    AND v.election_id = 133
GROUP BY nf.family_id, v.family_sequence_number, nf.new_seq, nf.family_modified_time
ORDER BY nf.new_seq ASC;

-- STEP 3: Execute the update (uncomment to run)
/*
WITH numbered_families AS (
    SELECT DISTINCT 
        family_id,
        MIN(modified_time) as family_modified_time,
        ROW_NUMBER() OVER (ORDER BY MIN(modified_time) ASC) as new_seq
    FROM voter_entity
    WHERE account_id = 9842505911
      AND election_id = 133
      AND family_id IS NOT NULL
    GROUP BY family_id
)
UPDATE voter_entity v
SET family_sequence_number = nf.new_seq
FROM numbered_families nf
WHERE v.family_id = nf.family_id
  AND v.account_id = 9842505911
  AND v.election_id = 133;
*/

-- STEP 4: Verify the results after update
SELECT 
    family_id,
    family_sequence_number,
    MIN(modified_time) as family_modified_time,
    COUNT(*) as family_size
FROM voter_entity
WHERE account_id = 9842505911
  AND election_id = 133
  AND family_id IS NOT NULL
GROUP BY family_id, family_sequence_number
ORDER BY family_sequence_number ASC;
