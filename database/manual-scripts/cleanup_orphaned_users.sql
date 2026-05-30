-- Script to identify and clean up orphaned user records
-- Run this to fix existing data issues where users exist without volunteer records

-- Step 1: View orphaned users (users without any volunteer records)
-- This query shows users who have no volunteer records across any elections
SELECT 
    u.user_id, 
    u.mobile_number, 
    u.first_name, 
    u.last_name,
    u.email,
    r.role_name,
    u.is_active,
    u.created_at
FROM _user u
INNER JOIN role r ON u.role_id = r.id
LEFT JOIN volunteers v ON v.user_id = u.user_id
WHERE v.id IS NULL 
    AND r.role_name NOT IN ('SUPER_ADMIN', 'ELECTION_OFFICER', 'DATA_ENTRY_OPERATOR', 'BOOTH_MANAGER')
ORDER BY u.created_at DESC;

-- Step 2: Count orphaned users
SELECT COUNT(*) as orphaned_user_count
FROM _user u
INNER JOIN role r ON u.role_id = r.id
LEFT JOIN volunteers v ON v.user_id = u.user_id
WHERE v.id IS NULL 
    AND r.role_name NOT IN ('SUPER_ADMIN', 'ELECTION_OFFICER', 'DATA_ENTRY_OPERATOR', 'BOOTH_MANAGER');

-- Step 3: Delete orphaned users (CAUTION: Run this only after verifying Step 1 results)
-- Uncomment the following lines when you're ready to clean up

/*
DELETE FROM mobile_verification 
WHERE user_id IN (
    SELECT u.user_id
    FROM _user u
    INNER JOIN role r ON u.role_id = r.id
    LEFT JOIN volunteers v ON v.user_id = u.user_id
    WHERE v.id IS NULL 
        AND r.role_name NOT IN ('SUPER_ADMIN', 'ELECTION_OFFICER', 'DATA_ENTRY_OPERATOR', 'BOOTH_MANAGER')
);

DELETE FROM _user 
WHERE user_id IN (
    SELECT u.user_id
    FROM _user u
    INNER JOIN role r ON u.role_id = r.id
    LEFT JOIN volunteers v ON v.user_id = u.user_id
    WHERE v.id IS NULL 
        AND r.role_name NOT IN ('SUPER_ADMIN', 'ELECTION_OFFICER', 'DATA_ENTRY_OPERATOR', 'BOOTH_MANAGER')
);
*/

-- Step 4: Verify cleanup (run after Step 3)
-- This should return 0 if cleanup was successful
/*
SELECT COUNT(*) as remaining_orphaned_users
FROM _user u
INNER JOIN role r ON u.role_id = r.id
LEFT JOIN volunteers v ON v.user_id = u.user_id
WHERE v.id IS NULL 
    AND r.role_name NOT IN ('SUPER_ADMIN', 'ELECTION_OFFICER', 'DATA_ENTRY_OPERATOR', 'BOOTH_MANAGER');
*/
