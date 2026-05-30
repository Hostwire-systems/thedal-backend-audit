-- =====================================================
-- Subscription Manager Migration Script
-- Created: December 8, 2025
-- Description: Create subscription_module and user_subscription tables
--              and seed initial module data from the application menu
-- =====================================================

-- Create subscription_module table
CREATE TABLE IF NOT EXISTS subscription_module (
    id BIGSERIAL PRIMARY KEY,
    module_key VARCHAR(100) UNIQUE NOT NULL,
    module_name VARCHAR(200) NOT NULL,
    module_description VARCHAR(500),
    parent_module_id BIGINT,
    display_order INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    icon_name VARCHAR(100),
    route_path VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_parent_module FOREIGN KEY (parent_module_id) 
        REFERENCES subscription_module(id) ON DELETE CASCADE
);

-- Create user_subscription table
CREATE TABLE IF NOT EXISTS user_subscription (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    module_id BIGINT NOT NULL,
    has_access BOOLEAN NOT NULL DEFAULT true,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    granted_by VARCHAR(100),
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES _user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_module FOREIGN KEY (module_id) REFERENCES subscription_module(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_module UNIQUE (user_id, module_id)
);

-- Create indexes for better performance
CREATE INDEX idx_subscription_module_parent ON subscription_module(parent_module_id);
CREATE INDEX idx_subscription_module_active ON subscription_module(is_active);
CREATE INDEX idx_subscription_module_key ON subscription_module(module_key);
CREATE INDEX idx_user_subscription_user ON user_subscription(user_id);
CREATE INDEX idx_user_subscription_module ON user_subscription(module_id);
CREATE INDEX idx_user_subscription_access ON user_subscription(has_access);
CREATE INDEX idx_user_subscription_expires ON user_subscription(expires_at);

-- =====================================================
-- SEED INITIAL MODULE DATA
-- =====================================================

-- Insert parent modules (main menu items)
INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by) VALUES
('static-dashboard', 'Dashboard', 'Static Dashboard with overview and analytics', NULL, 1, 'TeamOutlined', '/static-dashboard', 'system'),
('election-manager', 'Election Manager', 'Manage elections and related configurations', NULL, 2, 'BarChartOutlined', NULL, 'system'),
('part-manager', 'Part Manager', 'Manage parts, sections, and booth information', NULL, 3, 'DatabaseOutlined', NULL, 'system'),
('voter-manager', 'Voter Manager', 'Manage voter data and information', NULL, 4, 'DatabaseOutlined', NULL, 'system'),
('family-manager', 'Family Manager', 'Manage families and family captains', NULL, 5, 'TeamOutlined', NULL, 'system'),
('cadre-manager', 'Cadre Manager', 'Manage cadre members and tracking', NULL, 6, 'TeamOutlined', NULL, 'system'),
('campaign-manager', 'Campaign Manager', 'Manage communication and campaigns', NULL, 7, 'AppstoreOutlined', NULL, 'system'),
('poll-day-manager', 'Poll Day Manager', 'Manage poll day activities and voting', NULL, 8, 'BarChartOutlined', '/poll-day-manager', 'system'),
('survey-manager', 'Survey Manager', 'Manage survey forms and submissions', NULL, 9, 'BarChartOutlined', NULL, 'system'),
('member-manager', 'Member Manager', 'Manage organization members', NULL, 10, 'AppstoreOutlined', NULL, 'system'),
('report', 'Report', 'View and generate reports', NULL, 11, 'BarChartOutlined', '/report', 'system'),
('settings', 'Settings', 'Application and user settings', NULL, 12, 'SettingOutlined', NULL, 'system');

-- Insert Election Manager submodules
INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'elections', 'Your Elections', 'Manage your elections', id, 1, 'BarChartOutlined', '/elections', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'app-banner', 'App Banner', 'Manage application banners', id, 2, 'CodeSandboxOutlined', '/app-banner', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'voterHistory', 'Voting History', 'View voting history', id, 3, 'HistoryOutlined', '/voterHistory', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'availability', 'Voter Category', 'Manage voter categories', id, 4, 'UserOutlined', '/availability', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'booth-slip', 'Voter Slip', 'Manage voter slips', id, 5, 'FileTextOutlined', '/booth-slip', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'parties', 'Party', 'Manage political parties', id, 6, 'FlagOutlined', '/parties', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'religion', 'Religion', 'Manage religion data', id, 7, 'UserAddOutlined', '/religion', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'caste-category', 'Caste Category', 'Manage caste categories', id, 8, 'UserAddOutlined', '/caste-category', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'caste', 'Caste', 'Manage caste data', id, 9, 'UserAddOutlined', '/caste', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'sub-caste', 'Sub-Caste', 'Manage sub-caste data', id, 10, 'UserAddOutlined', '/sub-caste', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'language', 'Language', 'Manage language settings', id, 11, 'TranslationOutlined', '/language', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'benefit-scheme', 'Schemes', 'Manage benefit schemes', id, 12, 'DatabaseOutlined', '/benefit-scheme', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'feedback', 'Feedback', 'View user feedback', id, 13, 'CommentOutlined', '/feedback', 'system'
FROM subscription_module WHERE module_key = 'election-manager';

-- Insert Part Manager submodules
INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'boothType', 'Vulnerability', 'Manage booth vulnerability', id, 1, 'ToolOutlined', '/boothType', 'system'
FROM subscription_module WHERE module_key = 'part-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'part-list', 'Part List', 'View parts list', id, 2, 'FileTextOutlined', '/part-list', 'system'
FROM subscription_module WHERE module_key = 'part-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'add-part', 'Add Part', 'Add new part', id, 3, 'UserAddOutlined', '/add-part', 'system'
FROM subscription_module WHERE module_key = 'part-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'part-map', 'Part Map', 'View part map', id, 4, 'DatabaseOutlined', '/part-map', 'system'
FROM subscription_module WHERE module_key = 'part-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'section-list', 'Section List', 'View sections list', id, 5, 'FileTextOutlined', '/section-list', 'system'
FROM subscription_module WHERE module_key = 'part-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'add-section', 'Add Section', 'Add new section', id, 6, 'UserAddOutlined', '/add-section', 'system'
FROM subscription_module WHERE module_key = 'part-manager';

-- Insert Voter Manager submodules
INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'voterslist', 'Voters List', 'View voters list', id, 1, 'DatabaseOutlined', '/voterslist', 'system'
FROM subscription_module WHERE module_key = 'voter-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'add-voter', 'Add Voter', 'Add new voter', id, 2, 'UserAddOutlined', '/add-voter', 'system'
FROM subscription_module WHERE module_key = 'voter-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'pdf-photo-processing', 'PDF Photo Processing', 'Process PDF photos', id, 3, 'FileImageOutlined', '/pdf-photo-processing', 'system'
FROM subscription_module WHERE module_key = 'voter-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'voters-map', 'Voters Map', 'View voters on map', id, 4, 'HeatMapOutlined', '/voters-map', 'system'
FROM subscription_module WHERE module_key = 'voter-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'duplicate-voters', 'Double Entry Voters', 'View duplicate voters', id, 5, 'UserOutlined', '/duplicate-voters', 'system'
FROM subscription_module WHERE module_key = 'voter-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'new-voters', 'Enroll Voter', 'Enroll new voters', id, 6, 'UserAddOutlined', '/new-voters', 'system'
FROM subscription_module WHERE module_key = 'voter-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'aadhaar-verify', 'Aadhaar Verified Data', 'View Aadhaar verified data', id, 7, 'SafetyOutlined', '/aadhaar-verify', 'system'
FROM subscription_module WHERE module_key = 'voter-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'sir', 'SIR', 'Special Identity Records', id, 8, 'FileTextOutlined', '/sir', 'system'
FROM subscription_module WHERE module_key = 'voter-manager';

-- Insert Family Manager submodules
INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'family', 'Family', 'Manage families', id, 1, 'TeamOutlined', '/family', 'system'
FROM subscription_module WHERE module_key = 'family-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'family-captain-list', 'Family Captain List', 'View family captain list', id, 2, 'TeamOutlined', '/family-captain-list', 'system'
FROM subscription_module WHERE module_key = 'family-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'create-family-captain', 'Add Family Captain', 'Add new family captain', id, 3, 'UserAddOutlined', '/create-family-captain', 'system'
FROM subscription_module WHERE module_key = 'family-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'family-captain-map', 'Family Captain Map', 'View family captain map', id, 4, 'HeatMapOutlined', '/family-captain-map', 'system'
FROM subscription_module WHERE module_key = 'family-manager';

-- Insert Cadre Manager submodules
INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'cadre-list', 'Cadre List', 'View cadre list', id, 1, 'TeamOutlined', '/cadre-list', 'system'
FROM subscription_module WHERE module_key = 'cadre-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'add-cadre', 'Add Cadre', 'Add new cadre', id, 2, 'UserAddOutlined', '/add-cadre', 'system'
FROM subscription_module WHERE module_key = 'cadre-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'cadre-map', 'Cadre Map', 'View cadre map', id, 3, 'HeatMapOutlined', '/cadre-map', 'system'
FROM subscription_module WHERE module_key = 'cadre-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'cadre-tracking-list', 'Cadre Tracking List', 'View cadre tracking', id, 4, 'FieldTimeOutlined', '/cadre-tracking-list', 'system'
FROM subscription_module WHERE module_key = 'cadre-manager';

-- Insert Campaign Manager submodules
INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'communication', 'Communication Manager', 'Manage communications', id, 1, 'CommentOutlined', '/communication', 'system'
FROM subscription_module WHERE module_key = 'campaign-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'create-message', 'Create Campaign', 'Create new campaign', id, 2, 'CommentOutlined', '/create-message', 'system'
FROM subscription_module WHERE module_key = 'campaign-manager';

-- Insert Survey Manager submodules
INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'surveyForm', 'Survey Forms', 'Manage survey forms', id, 1, 'BarChartOutlined', '/surveyForm', 'system'
FROM subscription_module WHERE module_key = 'survey-manager';

-- Insert Member Manager submodules
INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'memberList', 'Members List', 'View members list', id, 1, 'TeamOutlined', '/memberList', 'system'
FROM subscription_module WHERE module_key = 'member-manager';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'add-member', 'Add Member', 'Add new member', id, 2, 'UserAddOutlined', '/add-member', 'system'
FROM subscription_module WHERE module_key = 'member-manager';

-- Insert Settings submodules
INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'profile', 'User Profile', 'Manage user profile', id, 1, 'ProfileOutlined', '/profile', 'system'
FROM subscription_module WHERE module_key = 'settings';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'authentication', 'Authentication', 'Authentication settings', id, 2, 'SafetyOutlined', '/authentication', 'system'
FROM subscription_module WHERE module_key = 'settings';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'role', 'Roles', 'Manage roles', id, 3, 'UserOutlined', '/role', 'system'
FROM subscription_module WHERE module_key = 'settings';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'slip-box', 'Slip Box', 'Manage slip box', id, 4, 'PrinterOutlined', '/slip-box', 'system'
FROM subscription_module WHERE module_key = 'settings';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'dynamic-fields', 'Dynamic Fields', 'Manage dynamic fields', id, 5, 'PrinterOutlined', '/dynamic-fields', 'system'
FROM subscription_module WHERE module_key = 'settings';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'fields-order', 'Voter Basic Info', 'Manage voter basic info fields', id, 6, 'PrinterOutlined', '/fields-order', 'system'
FROM subscription_module WHERE module_key = 'settings';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'download', 'Downloads', 'Download resources', id, 7, 'DownloadOutlined', '/download', 'system'
FROM subscription_module WHERE module_key = 'settings';

INSERT INTO subscription_module (module_key, module_name, module_description, parent_module_id, display_order, icon_name, route_path, created_by)
SELECT 'catalogue', 'Catalogue', 'View catalogue', id, 8, 'UserOutlined', NULL, 'system'
FROM subscription_module WHERE module_key = 'settings';

-- =====================================================
-- GRANT ALL MODULES TO SUPER_ADMIN AND ADMIN USERS
-- =====================================================

-- Grant all modules to users with SUPER_ADMIN or ADMIN role
INSERT INTO user_subscription (user_id, module_id, has_access, granted_at, granted_by)
SELECT 
    u.user_id,
    sm.id,
    true,
    CURRENT_TIMESTAMP,
    'system'
FROM _user u
CROSS JOIN subscription_module sm
WHERE u.role_id IN (
    SELECT id FROM role WHERE role_name IN ('SUPER_ADMIN', 'ADMIN')
)
ON CONFLICT (user_id, module_id) DO NOTHING;

-- =====================================================
-- VERIFICATION QUERIES (commented out, uncomment to run)
-- =====================================================

-- SELECT COUNT(*) as total_modules FROM subscription_module;
-- SELECT COUNT(*) as parent_modules FROM subscription_module WHERE parent_module_id IS NULL;
-- SELECT COUNT(*) as submodules FROM subscription_module WHERE parent_module_id IS NOT NULL;
-- SELECT COUNT(*) as total_subscriptions FROM user_subscription;
-- SELECT u.first_name, u.last_name, r.role_name, COUNT(us.id) as module_count
-- FROM _user u
-- JOIN role r ON u.role_id = r.id
-- LEFT JOIN user_subscription us ON u.user_id = us.user_id
-- WHERE r.role_name IN ('SUPER_ADMIN', 'ADMIN')
-- GROUP BY u.user_id, u.first_name, u.last_name, r.role_name;
