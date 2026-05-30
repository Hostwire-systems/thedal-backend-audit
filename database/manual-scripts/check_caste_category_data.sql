-- Check caste_category data for election 58
SELECT id, caste_category_name 
FROM caste_category 
WHERE account_id = 54 AND election_id = 58
ORDER BY caste_category_name;

-- Check how many voters have caste_category_id populated
SELECT 
    CASE 
        WHEN caste_category_id IS NULL THEN 'NULL'
        ELSE 'HAS_VALUE'
    END as category_status,
    COUNT(*) as voter_count
FROM _voters 
WHERE account_id = 54 AND election_id = 58
GROUP BY CASE WHEN caste_category_id IS NULL THEN 'NULL' ELSE 'HAS_VALUE' END;

-- Check actual caste category distribution
SELECT 
    cc.caste_category_name,
    COUNT(v.id) as voter_count
FROM _voters v
LEFT JOIN caste_category cc ON v.caste_category_id = cc.id
WHERE v.account_id = 54 AND v.election_id = 58
GROUP BY cc.caste_category_name
ORDER BY voter_count DESC;

-- Check if the issue is with the part-specific query
SELECT 
    v.part_no,
    cc.caste_category_name,
    COUNT(v.id) as voter_count
FROM _voters v
LEFT JOIN caste_category cc ON v.caste_category_id = cc.id
WHERE v.account_id = 54 AND v.election_id = 58
GROUP BY v.part_no, cc.caste_category_name
ORDER BY v.part_no, voter_count DESC
LIMIT 50;
