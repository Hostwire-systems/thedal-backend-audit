-- Fix for missing expiry_at dates in _user table
-- This script will set expiry_at to 6 months from created_at for existing users
-- and 6 months from now for users without created_at

-- 1. First, check current state
SELECT 
    COUNT(*) as total_users,
    COUNT(expiry_at) as users_with_expiry,
    COUNT(created_at) as users_with_created_at
FROM _user;

-- 2. Update users who have created_at: set expiry to created_at + 6 months
UPDATE _user
SET expiry_at = created_at + INTERVAL '6 months'
WHERE created_at IS NOT NULL 
  AND expiry_at IS NULL;

-- 3. Update users without created_at: set expiry to NOW + 6 months
UPDATE _user
SET expiry_at = NOW() + INTERVAL '6 months'
WHERE created_at IS NULL 
  AND expiry_at IS NULL;

-- 4. Verify the fix
SELECT 
    COUNT(*) as total_users,
    COUNT(expiry_at) as users_with_expiry,
    COUNT(*) - COUNT(expiry_at) as users_without_expiry,
    MIN(expiry_at) as earliest_expiry,
    MAX(expiry_at) as latest_expiry
FROM _user;

-- 5. Sample check for the specific users from API
SELECT 
    user_id,
    first_name,
    last_name,
    mobile_number,
    created_at,
    expiry_at,
    is_active
FROM _user
WHERE user_id IN (3602, 3702, 3852, 3703, 3604, 3802, 3652, 3603, 4202, 4152)
ORDER BY user_id;
