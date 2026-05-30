-- Migration script to initialize 3 default static roles for all existing accounts
-- This should be run once after deploying the role initialization feature
-- These roles will be automatically created for new elections going forward

DO $$
DECLARE
    account_record RECORD;
    role_exists BOOLEAN;
BEGIN
    -- Loop through all distinct account IDs that have elections
    FOR account_record IN 
        SELECT DISTINCT account_id 
        FROM election 
        WHERE account_id IS NOT NULL
        ORDER BY account_id
    LOOP
        RAISE NOTICE 'Processing account_id: %', account_record.account_id;
        
        -- 1. Create Booth Captain role if it doesn't exist
        SELECT EXISTS (
            SELECT 1 FROM role 
            WHERE LOWER(role_name) = 'booth captain' 
            AND account_id = account_record.account_id
        ) INTO role_exists;
        
        IF NOT role_exists THEN
            INSERT INTO role (role_name, permission, role_permission, description, account_id)
            VALUES (
                'Booth Captain',
                0,
                '{"voterList":["R","U"],"voterMap":["R","U"],"newVoter":["C","R","U","D"]}'::jsonb,
                'Manages voter engagement at grassroots level and collects voter data',
                account_record.account_id
            );
            RAISE NOTICE 'Created Booth Captain role for account_id: %', account_record.account_id;
        ELSE
            RAISE NOTICE 'Booth Captain role already exists for account_id: %', account_record.account_id;
        END IF;
        
        -- 2. Create Family Captain role if it doesn't exist
        SELECT EXISTS (
            SELECT 1 FROM role 
            WHERE LOWER(role_name) = 'family captain' 
            AND account_id = account_record.account_id
        ) INTO role_exists;
        
        IF NOT role_exists THEN
            INSERT INTO role (role_name, permission, role_permission, description, account_id)
            VALUES (
                'Family Captain',
                0,
                '{"family":["R"],"familyPollStatus":["R"]}'::jsonb,
                'Manages assigned families and tracks polling on election day',
                account_record.account_id
            );
            RAISE NOTICE 'Created Family Captain role for account_id: %', account_record.account_id;
        ELSE
            RAISE NOTICE 'Family Captain role already exists for account_id: %', account_record.account_id;
        END IF;
        
        -- 3. Create Poll Captain role if it doesn't exist
        SELECT EXISTS (
            SELECT 1 FROM role 
            WHERE LOWER(role_name) = 'poll captain' 
            AND account_id = account_record.account_id
        ) INTO role_exists;
        
        IF NOT role_exists THEN
            INSERT INTO role (role_name, permission, role_permission, description, account_id)
            VALUES (
                'Poll Captain',
                0,
                '{"polldayVote":["R","U"]}'::jsonb,
                'Manages polling booth and tracks real-time voting on election day',
                account_record.account_id
            );
            RAISE NOTICE 'Created Poll Captain role for account_id: %', account_record.account_id;
        ELSE
            RAISE NOTICE 'Poll Captain role already exists for account_id: %', account_record.account_id;
        END IF;
        
    END LOOP;
    
    RAISE NOTICE 'Default role initialization completed for all existing accounts';
END $$;

-- Verify the results
SELECT 
    account_id,
    role_name,
    description,
    role_permission
FROM role 
WHERE role_name IN ('Booth Captain', 'Family Captain', 'Poll Captain')
ORDER BY account_id, role_name;

-- Count summary
SELECT 
    role_name,
    COUNT(*) as total_accounts
FROM role 
WHERE role_name IN ('Booth Captain', 'Family Captain', 'Poll Captain')
GROUP BY role_name
ORDER BY role_name;
