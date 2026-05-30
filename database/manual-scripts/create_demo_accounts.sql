-- Create Demo Accounts with Mobile Numbers Activated
-- BCrypt hashed passwords (strength 10) for:
-- Admk@123, Dmk@123, Inc@123, Bjp@123, Ntk@123, Tvk@123

DO $$
DECLARE
    admk_account_id BIGINT;
    dmk_account_id BIGINT;
    inc_account_id BIGINT;
    bjp_account_id BIGINT;
    ntk_account_id BIGINT;
    tvk_account_id BIGINT;
    admin_role_id BIGINT;
BEGIN
    -- Get admin role ID
    SELECT id INTO admin_role_id FROM role WHERE role_name = 'ADMIN' LIMIT 1;
    IF admin_role_id IS NULL THEN
        RAISE EXCEPTION 'ADMIN role not found';
    END IF;

    -- Insert ADMK Account
    INSERT INTO account (id, on_board_status) 
    VALUES (nextval('account_seq'), 1) 
    RETURNING id INTO admk_account_id;
    
    INSERT INTO _user (
        user_id, role_id, account_entity_id, first_name, last_name, email_address, 
        mobile_number, password, is_email_verified, is_mobile_verified, 
        is_active, created_by, updated_by, created_at, updated_at,
        slip_box, is_two_factor_enabled, password_version, is_otp_required
    ) VALUES (
        nextval('_user_seq'), admin_role_id, admk_account_id, 'ADMK', 'Demo', 'admk@demo.com',
        '9008007001', '$2a$10$C29gkjNARtGBOh5gCPUgwOWv3SCzmB6bFhf4SRMb1Tf5CbErb7.Q6',
        true, true, true, 'system', 'system', NOW(), NOW(),
        true, false, 1, false
    );

    -- Insert DMK Account
    INSERT INTO account (id, on_board_status) 
    VALUES (nextval('account_seq'), 1) 
    RETURNING id INTO dmk_account_id;
    
    INSERT INTO _user (
        user_id, role_id, account_entity_id, first_name, last_name, email_address, 
        mobile_number, password, is_email_verified, is_mobile_verified, 
        is_active, created_by, updated_by, created_at, updated_at,
        slip_box, is_two_factor_enabled, password_version, is_otp_required
    ) VALUES (
        nextval('_user_seq'), admin_role_id, dmk_account_id, 'DMK', 'Demo', 'dmk@demo.com',
        '9008007002', '$2a$10$XTCZCMWnGy68SQ0GyyI5keW0sWjcm3Aeax9AFjZ3mah74.tnK1PKq',
        true, true, true, 'system', 'system', NOW(), NOW(),
        true, false, 1, false
    );

    -- Insert INC Account
    INSERT INTO account (id, on_board_status) 
    VALUES (nextval('account_seq'), 1) 
    RETURNING id INTO inc_account_id;
    
    INSERT INTO _user (
        user_id, role_id, account_entity_id, first_name, last_name, email_address, 
        mobile_number, password, is_email_verified, is_mobile_verified, 
        is_active, created_by, updated_by, created_at, updated_at,
        slip_box, is_two_factor_enabled, password_version, is_otp_required
    ) VALUES (
        nextval('_user_seq'), admin_role_id, inc_account_id, 'INC', 'Demo', 'inc@demo.com',
        '9008007003', '$2a$10$dtrI8aKc.o.gJ5CYWJWaNuckNjugs/8VjCcWdkQVnxGrT18DKmVF.',
        true, true, true, 'system', 'system', NOW(), NOW(),
        true, false, 1, false
    );

    -- Insert BJP Account
    INSERT INTO account (id, on_board_status) 
    VALUES (nextval('account_seq'), 1) 
    RETURNING id INTO bjp_account_id;
    
    INSERT INTO _user (
        user_id, role_id, account_id, first_name, last_name, email_address, 
        mobile_number, password, is_email_verified, is_mobile_verified, 
        is_active, created_by, updated_by, created_at, updated_at,
        slip_box, is_two_factor_enabled, password_version, is_otp_required
    ) VALUES (
        nextval('_user_seq'), admin_role_id, bjp_account_id, 'BJP', 'Demo', 'bjp@demo.com',
        '9008007004', '$2a$10$wz8JNopHGchUX5RI2x28HOidLBWzWuTqtQPY/0PqKJaJM2VRIYzzC',
        true, true, true, 'system', 'system', NOW(), NOW(),
        true, false, 1, false
    );

    -- Insert NTK Account
    INSERT INTO account (id, on_board_status) 
    VALUES (nextval('account_seq'), 1) 
    RETURNING id INTO ntk_account_id;
    
    INSERT INTO _user (
        user_id, role_id, account_entity_id, first_name, last_name, email_address,
        mobile_number, password, is_email_verified, is_mobile_verified, 
        is_active, created_by, updated_by, created_at, updated_at,
        slip_box, is_two_factor_enabled, password_version, is_otp_required
    ) VALUES (
        nextval('_user_seq'), admin_role_id, ntk_account_id, 'NTK', 'Demo', 'ntk@demo.com',
        '9008007005', '$2a$10$wByM9g9G6ZQeC.KiLetZkuLhKSPxGxWguSPl5/Vx/etuX9XdDz0d.',
        true, true, true, 'system', 'system', NOW(), NOW(),
        true, false, 1, false
    );

    -- Insert TVK Account
    INSERT INTO account (id, on_board_status) 
    VALUES (nextval('account_seq'), 1) 
    RETURNING id INTO tvk_account_id;
    
    INSERT INTO _user (
        user_id, role_id, account_entity_id, first_name, last_name, email_address,
        mobile_number, password, is_email_verified, is_mobile_verified, 
        is_active, created_by, updated_by, created_at, updated_at,
        slip_box, is_two_factor_enabled, password_version, is_otp_required
    ) VALUES (
        nextval('_user_seq'), admin_role_id, tvk_account_id, 'TVK', 'Demo', 'tvk@demo.com',
        '9008007006', '$2a$10$8evFuQX5n4Mg/9zmrJHSe.uuIYsMX.OFpeqnesRI2ZfuJWGWc6Nai',
        true, true, true, 'system', 'system', NOW(), NOW(),
        true, false, 1, false
    );

    RAISE NOTICE 'Demo accounts created successfully!';
    RAISE NOTICE 'ADMK Account ID: %, User Mobile: 9008007001', admk_account_id;
    RAISE NOTICE 'DMK Account ID: %, User Mobile: 9008007002', dmk_account_id;
    RAISE NOTICE 'INC Account ID: %, User Mobile: 9008007003', inc_account_id;
    RAISE NOTICE 'BJP Account ID: %, User Mobile: 9008007004', bjp_account_id;
    RAISE NOTICE 'NTK Account ID: %, User Mobile: 9008007005', ntk_account_id;
    RAISE NOTICE 'TVK Account ID: %, User Mobile: 9008007006', tvk_account_id;
END $$;

-- Verification Query
-- SELECT u.user_id, u.first_name, u.mobile_number, u.is_mobile_verified, u.is_active, a.id as account_id
-- FROM _user u
-- JOIN account a ON u.account_id = a.id
-- WHERE u.mobile_number IN ('9008007001', '9008007002', '9008007003', '9008007004', '9008007005', '9008007006')
-- ORDER BY u.mobile_number;
