-- Check actual gender values in _voters table
SELECT DISTINCT gender, COUNT(*) as count
FROM _voters
WHERE account_id = 54 AND election_id = 58
GROUP BY gender
ORDER BY count DESC;

-- Check gender values for part 1 specifically
SELECT DISTINCT gender, COUNT(*) as count
FROM _voters
WHERE account_id = 54 AND election_id = 58 AND part_no = 1
GROUP BY gender
ORDER BY count DESC;

-- Check mobile counts by gender for part 1
SELECT gender, 
       COUNT(*) as total_voters,
       COUNT(CASE WHEN mobile_no IS NOT NULL AND length(trim(mobile_no))>0 THEN 1 END) as with_mobile
FROM _voters
WHERE account_id = 54 AND election_id = 58 AND part_no = 1
GROUP BY gender;
