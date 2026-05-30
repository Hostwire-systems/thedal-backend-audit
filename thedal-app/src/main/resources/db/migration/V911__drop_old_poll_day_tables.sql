-- Migration: V911 - Drop unused poll day dashboard tables
-- These tables are no longer used after consolidating to part-wise polling API

-- Drop old poll day tables
DROP TABLE IF EXISTS poll_day_hourly_turnout CASCADE;
DROP TABLE IF EXISTS poll_day_age_group_turnout CASCADE;
DROP TABLE IF EXISTS poll_day_booth_summary CASCADE;
DROP TABLE IF EXISTS poll_day_ward_age_group_turnout CASCADE;

-- Verification comment
-- After this migration, only poll_day_part_wise_polling table remains for poll day dashboard functionality
