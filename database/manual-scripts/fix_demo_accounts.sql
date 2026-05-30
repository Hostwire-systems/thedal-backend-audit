-- Fix existing demo accounts by setting accountentity_id
-- This updates users created with account_id to have the correct accountentity_id

DO $$
BEGIN
    -- Update ADMK user
    UPDATE _user 
    SET account_entity_id = account_id 
    WHERE mobile_number = '9008007001' AND account_entity_id IS NULL;

    -- Update DMK user
    UPDATE _user 
    SET account_entity_id = account_id 
    WHERE mobile_number = '9008007002' AND account_entity_id IS NULL;

    -- Update INC user
    UPDATE _user 
    SET account_entity_id = account_id 
    WHERE mobile_number = '9008007003' AND account_entity_id IS NULL;

    -- Update BJP user
    UPDATE _user 
    SET account_entity_id = account_id 
    WHERE mobile_number = '9008007004' AND account_entity_id IS NULL;

    -- Update NTK user
    UPDATE _user 
    SET account_entity_id = account_id 
    WHERE mobile_number = '9008007005' AND account_entity_id IS NULL;

    -- Update TVK user
    UPDATE _user 
    SET account_entity_id = account_id 
    WHERE mobile_number = '9008007006' AND account_entity_id IS NULL;

    RAISE NOTICE 'Demo accounts fixed successfully!';
END $$;

-- Verification Query
SELECT 
    u.user_id, 
    u.first_name, 
    u.mobile_number, 
    u.account_id,
    u.account_entity_id,
    u.is_mobile_verified, 
    u.is_active
FROM _user u
WHERE u.mobile_number IN ('9008007001', '9008007002', '9008007003', '9008007004', '9008007005', '9008007006')
ORDER BY u.mobile_number;
