# Election Aggregates API - New Fields

## Overview
Added 5 new voter count fields to the Election Aggregates API to provide additional voter statistics.

## API Endpoint
```
GET /reporting/api/aggregates/election/{accountId}/{electionId}
```

## New Fields Added

### 1. `dateOfBirth` (Integer)
- **Description:** Count of voters who have date of birth data
- **SQL Query:** `SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.date_of_birth IS NOT NULL`
- **Use Case:** Track data completeness for voter date of birth

### 2. `startVoters` (Integer)
- **Description:** Count of voters marked as star voters
- **SQL Query:** `SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND v.star_number = true`
- **Use Case:** Identify and count high-priority or influential voters
- **Note:** Field name corrected from `startVoters` to `starVoters` in the codebase

### 3. `religionCount` (Integer)
- **Description:** Count of distinct religions in the election
- **SQL Query:** `SELECT COUNT(DISTINCT religion_id) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND religion_id IS NOT NULL`
- **Use Case:** Understand religious diversity in the electorate

### 4. `casteCount` (Integer)
- **Description:** Count of distinct castes in the election
- **SQL Query:** `SELECT COUNT(DISTINCT caste_id) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND caste_id IS NOT NULL`
- **Use Case:** Understand caste diversity in the electorate

### 5. `totalmobileCount` (Integer)
- **Description:** Count of voters who have mobile numbers
- **SQL Query:** `SELECT COUNT(*) FROM _voters v WHERE v.account_id=:a AND v.election_id=:e AND mobile_no IS NOT NULL AND length(trim(mobile_no))>0`
- **Use Case:** Track voter reachability and contact data completeness
- **Note:** Different from `distinctMobileCount` which counts unique mobile numbers

## Sample Response

```json
{
  "accountId": 54,
  "electionId": 58,
  "totalBooth": 5,
  "totalVoters": 6099,
  "totalFamily": 1074,
  "distinctPincodeCount": 6,
  "distinctMobileCount": 1316,
  "male": 3012,
  "female": 3077,
  "transgender": 10,
  "age18To30": 1009,
  "age30To40": 1215,
  "age40To50": 1528,
  "age50To60": 1106,
  "age60To70": 710,
  "ageGreaterThan70": 510,
  "firstTimeVoters": 243,
  "seniorCitizens": 710,
  "superSeniors": 510,
  
  // NEW FIELDS
  "dateOfBirth": 5500,
  "starVoters": 450,
  "religionCount": 5,
  "casteCount": 23,
  "totalMobileCount": 4800,
  
  "computedAt": "2025-09-03T21:03:21.895934Z",
  "refreshedAt": "2025-10-14T10:20:03.013920Z",
  "freshnessSeconds": 34
}
```

## Database Changes

### Migration Script: `add_voter_count_fields_to_election_stats.sql`

New columns added to `election_dashboard_stats` table:
- `date_of_birth_count` (INTEGER, NOT NULL, DEFAULT 0)
- `star_voters_count` (INTEGER, NOT NULL, DEFAULT 0)
- `religion_count` (INTEGER, NOT NULL, DEFAULT 0)
- `caste_count` (INTEGER, NOT NULL, DEFAULT 0)
- `total_mobile_count` (INTEGER, NOT NULL, DEFAULT 0)

## Files Modified

### Entity Classes:
1. `thedal-reporting-app/src/main/java/com/thedal/reporting/election/ElectionDashboardStats.java`
2. `thedal-app/src/main/java/com/thedal/thedal_app/report/aggregates/ElectionDashboardStats.java`

### Service Classes:
1. `thedal-reporting-app/src/main/java/com/thedal/reporting/election/ElectionStatsAggregationService.java`
2. `thedal-app/src/main/java/com/thedal/thedal_app/report/aggregates/ElectionStatsAggregationService.java`

## Recompute API

To recompute the stats with new fields:
```
POST /reporting/api/aggregates/election/{accountId}/{electionId}/recompute
```

This will:
1. Query the database for all voter counts
2. Calculate the 5 new fields
3. Update the `election_dashboard_stats` table
4. Return the updated stats immediately

## Testing

After running the migration script:

1. **Run the migration:**
   ```sql
   -- Execute: add_voter_count_fields_to_election_stats.sql
   ```

2. **Recompute stats:**
   ```bash
   POST https://your-api.com/reporting/api/aggregates/election/54/58/recompute
   ```

3. **Verify response:**
   ```bash
   GET https://your-api.com/reporting/api/aggregates/election/54/58
   ```

## Notes

- All new fields default to 0 if no data exists
- The fields are computed during recompute operation
- Existing aggregations remain valid and will include these fields once recomputed
- The `totalMobileCount` counts ALL voters with mobile numbers (may include duplicates)
- The `distinctMobileCount` (existing field) counts unique mobile numbers only
- **Senior citizens** (`seniorCitizens`) includes all voters aged 60 and above (both 60-69 and 70+)
- Field names corrected: `startVoters` → `starVoters`, `totalmobileCount` → `totalMobileCount`
