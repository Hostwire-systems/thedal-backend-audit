-- Create a Cpanel General table
CREATE TABLE IF NOT EXISTS cpanel_general (
    cpanel_name VARCHAR(255) NOT NULL,
    -- cpanel_value that can store a full text/string document content
    cpanel_value TEXT NOT NULL
);

-- Insert default values for cpanel_general for privacy policy, faq and terms of service
INSERT INTO cpanel_general (cpanel_name, cpanel_value)
SELECT 'privacy_policy', 'Privacy Policy content goes here'
WHERE NOT EXISTS (
    SELECT 1 FROM cpanel_general WHERE cpanel_name = 'privacy_policy'
);

INSERT INTO cpanel_general (cpanel_name, cpanel_value)
SELECT 'faq', 'FAQ content goes here'
WHERE NOT EXISTS (
    SELECT 1 FROM cpanel_general WHERE cpanel_name = 'faq'
);

INSERT INTO cpanel_general (cpanel_name, cpanel_value)
SELECT 'terms_of_service', 'Terms of Service content goes here'
WHERE NOT EXISTS (
    SELECT 1 FROM cpanel_general WHERE cpanel_name = 'terms_of_service'
);


INSERT INTO cpanel_general (cpanel_name, cpanel_value)
SELECT 'about', 'About content goes here'
WHERE NOT EXISTS (
    SELECT 1 FROM cpanel_general WHERE cpanel_name = 'about'
);




-- Create Role table with permission, description, account_id, and role_permission 
CREATE TABLE IF NOT EXISTS role (
  id SERIAL PRIMARY KEY,
  role_name VARCHAR(255) NOT NULL,
  permission INT NOT NULL,
  role_permission JSONB NOT NULL,
  description VARCHAR(120),
  account_id BIGINT 
);

INSERT INTO role (role_name, permission, role_permission, description, account_id) 
SELECT 
    'SUPER_ADMIN', 
    31, 
    '{
        "electionsList": ["C", "R", "U", "D"],
        "appsBanner": ["C", "R", "U", "D"],
        "availability": ["C", "R", "U", "D"],
        "benefitScheme": ["C", "R", "U", "D"],
        "boothSlip": ["C", "R", "U", "D"],
        "boothType": ["C", "R", "U", "D"],
        "language": ["C", "R", "U", "D"],
        "party": ["C", "R", "U", "D"],
        "religion": ["C", "R", "U", "D"],
        "caste": ["C", "R", "U", "D"],
        "subCaste": ["C", "R", "U", "D"],
        "votersList": ["C", "R", "U", "D"],
        "addVoter": ["C", "R", "U", "D"],
        "votersMap": ["C", "R", "U", "D"],
        "cadreList": ["C", "R", "U", "D"],
        "addCadre": ["C", "R", "U", "D"],
        "cadreMap": ["C", "R", "U", "D"],
        "cadreTrackingList": ["C", "R", "U", "D"],
        "news": ["C", "R", "U", "D"],
        "userProfile": ["C", "R", "U", "D"],
        "roles": ["C", "R", "U", "D"],
        "bulkSms": ["C", "R", "U", "D"]
    }'::jsonb, 
    'Super Admin Complete system access', 
    -1
WHERE NOT EXISTS (
    SELECT 1 FROM role WHERE role_name = 'SUPER_ADMIN'
);

INSERT INTO role (role_name, permission, role_permission, description, account_id) 
SELECT 
    'ELECTION_OFFICER', 
    31, 
    '{
        "electionsList": ["C", "R", "U", "D"],
        "appsBanner": ["C", "R", "U", "D"],
        "availability": ["C", "R", "U", "D"],
        "benefitScheme": ["C", "R", "U", "D"],
        "boothSlip": ["C", "R", "U", "D"],
        "boothType": ["C", "R", "U", "D"],
        "language": ["C", "R", "U", "D"],
        "party": ["C", "R", "U", "D"],
        "religion": ["C", "R", "U", "D"],
        "caste": ["C", "R", "U", "D"],
        "subCaste": ["C", "R", "U", "D"],
        "votersList": ["C", "R", "U", "D"],
        "addVoter": ["C", "R", "U", "D"],
        "votersMap": ["C", "R", "U", "D"],
        "cadreList": ["C", "R", "U", "D"],
        "addCadre": ["C", "R", "U", "D"],
        "cadreMap": ["C", "R", "U", "D"],
        "cadreTrackingList": ["C", "R", "U", "D"],
        "news": ["C", "R", "U", "D"],
        "userProfile": ["C", "R", "U", "D"],
        "roles": ["C", "R", "U", "D"],
        "bulkSms": ["C", "R", "U", "D"]
    }'::jsonb, 
    'Manage election operations', 
    -1
WHERE NOT EXISTS (
    SELECT 1 FROM role WHERE role_name = 'ELECTION_OFFICER'
);

INSERT INTO role (role_name, permission, role_permission, description, account_id) 
SELECT 
    'DATA_ENTRY_OPERATOR', 
    31, 
    '{
        "electionsList": ["C", "R", "U", "D"],
        "appsBanner": ["C", "R", "U", "D"],
        "availability": ["C", "R", "U", "D"],
        "benefitScheme": ["C", "R", "U", "D"],
        "boothSlip": ["C", "R", "U", "D"],
        "boothType": ["C", "R", "U", "D"],
        "language": ["C", "R", "U", "D"],
        "party": ["C", "R", "U", "D"],
        "religion": ["C", "R", "U", "D"],
        "caste": ["C", "R", "U", "D"],
        "subCaste": ["C", "R", "U", "D"],
        "votersList": ["C", "R", "U", "D"],
        "addVoter": ["C", "R", "U", "D"],
        "votersMap": ["C", "R", "U", "D"],
        "cadreList": ["C", "R", "U", "D"],
        "addCadre": ["C", "R", "U", "D"],
        "cadreMap": ["C", "R", "U", "D"],
        "cadreTrackingList": ["C", "R", "U", "D"],
        "news": ["C", "R", "U", "D"],
        "userProfile": ["C", "R", "U", "D"],
        "roles": ["C", "R", "U", "D"],
        "bulkSms": ["C", "R", "U", "D"]
    }'::jsonb, 
    'Basic data entry access', 
    -1
WHERE NOT EXISTS (
    SELECT 1 FROM role WHERE role_name = 'DATA_ENTRY_OPERATOR'
);

INSERT INTO role (role_name, permission, role_permission, description, account_id) 
SELECT 
    'BOOTH_MANAGER', 
    31, 
    '{
        "electionsList": ["C", "R", "U", "D"],
        "appsBanner": ["C", "R", "U", "D"],
        "availability": ["C", "R", "U", "D"],
        "benefitScheme": ["C", "R", "U", "D"],
        "boothSlip": ["C", "R", "U", "D"],
        "boothType": ["C", "R", "U", "D"],
        "language": ["C", "R", "U", "D"],
        "party": ["C", "R", "U", "D"],
        "religion": ["C", "R", "U", "D"],
        "caste": ["C", "R", "U", "D"],
        "subCaste": ["C", "R", "U", "D"],
        "votersList": ["C", "R", "U", "D"],
        "addVoter": ["C", "R", "U", "D"],
        "votersMap": ["C", "R", "U", "D"],
        "cadreList": ["C", "R", "U", "D"],
        "addCadre": ["C", "R", "U", "D"],
        "cadreMap": ["C", "R", "U", "D"],
        "cadreTrackingList": ["C", "R", "U", "D"],
        "news": ["C", "R", "U", "D"],
        "userProfile": ["C", "R", "U", "D"],
        "roles": ["C", "R", "U", "D"],
        "bulkSms": ["C", "R", "U", "D"]
    }'::jsonb, 
    'Booth management access', 
    -1
WHERE NOT EXISTS (
    SELECT 1 FROM role WHERE role_name = 'BOOTH_MANAGER'
);