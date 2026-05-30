-- Check what part numbers exist in part_manager for election 58
SELECT DISTINCT part_no, 
       CASE 
           WHEN part_no ~ '^\d+$' THEN 'NUMERIC'
           ELSE 'NON-NUMERIC'
       END as type
FROM part_manager 
WHERE account_id = 54 AND election_id = 58 AND part_no IS NOT NULL
ORDER BY type, part_no;

-- Check if there's a part with the value 'string'
SELECT * FROM part_manager 
WHERE account_id = 54 AND election_id = 58 AND part_no = 'string';

-- Check voter count by part_no to see if any non-numeric parts have voters
SELECT v.part_no, COUNT(*) as voter_count,
       CASE 
           WHEN CAST(v.part_no AS TEXT) ~ '^\d+$' THEN 'NUMERIC'
           ELSE 'NON-NUMERIC'
       END as type
FROM _voters v
WHERE v.account_id = 54 AND v.election_id = 58
GROUP BY v.part_no
HAVING CAST(v.part_no AS TEXT) !~ '^\d+$'
ORDER BY voter_count DESC;
