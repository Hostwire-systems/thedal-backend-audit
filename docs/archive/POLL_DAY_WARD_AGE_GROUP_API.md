# Poll Day Ward Age Group Turnout API

## Overview
New API endpoint for ward/part-specific age group voter turnout analysis with year-over-year historical comparison. This API is specifically designed for the Poll Day Dashboard's age group chart showing ward-level data.

## API Endpoint

### Base Path
`/api/reporting/poll-day/ward-age-groups`

---

## Endpoints

### 1. GET - Retrieve Ward Age Group Turnout

**URL:** `GET /api/reporting/poll-day/ward-age-groups`

**Description:** Retrieves age group turnout statistics for a specific ward (part number) with historical comparison.

**Parameters:**
- `accountId` (required) - Long - Account ID
- `electionId` (required) - Long - Election ID
- `partNumber` (required) - String - Ward/Part number (e.g., "7")
- `pollingDate` (optional) - String - Date in format YYYY-MM-DD (defaults to today in Asia/Kolkata timezone)

**Response:** `PollDayWardAgeGroupTurnout` object

**Response Headers:**
- `ETag` - Entity tag for caching
- `Cache-Control: public, max-age=30` - Cache for 30 seconds

**Example Request:**
```
GET /api/reporting/poll-day/ward-age-groups?accountId=1&electionId=100&partNumber=7&pollingDate=2025-10-14
```

**Example Response:**
```json
{
  "id": 1,
  "accountId": 1,
  "electionId": 100,
  "partNumber": "7",
  "pollingDate": "2025-10-14",
  "ageGroupBreakdownJson": "{\"18_21\":{\"total_voters\":150,\"polled_2025\":135,\"polled_2024\":120,\"did_not_vote\":15,\"is_first_time\":true},\"22_25\":{\"total_voters\":200,\"polled_2025\":180,\"polled_2024\":170,\"did_not_vote\":20,\"is_first_time\":false},\"26_35\":{\"total_voters\":350,\"polled_2025\":315,\"polled_2024\":300,\"did_not_vote\":35,\"is_first_time\":false},\"36_45\":{\"total_voters\":300,\"polled_2025\":270,\"polled_2024\":260,\"did_not_vote\":30,\"is_first_time\":false},\"46_59\":{\"total_voters\":250,\"polled_2025\":225,\"polled_2024\":210,\"did_not_vote\":25,\"is_first_time\":false},\"expired\":{\"total_voters\":250,\"polled_2025\":0,\"polled_2024\":0,\"did_not_vote\":250,\"is_first_time\":false},\"overall\":{\"total_voters\":1500,\"polled_2025\":1125,\"percentage\":75.0}}",
  "computedAt": "2025-10-14T10:00:00Z",
  "refreshedAt": "2025-10-14T10:00:00Z"
}
```

---

### 2. POST - Recompute Ward Age Group Turnout

**URL:** `POST /api/reporting/poll-day/ward-age-groups/recompute`

**Description:** Forces recomputation of age group turnout statistics for a specific ward. Rate-limited to prevent abuse.

**Parameters:**
- `accountId` (required) - Long - Account ID
- `electionId` (required) - Long - Election ID
- `partNumber` (required) - String - Ward/Part number
- `pollingDate` (optional) - String - Date in format YYYY-MM-DD

**Response:** 
- `200 OK` - Freshly computed `PollDayWardAgeGroupTurnout` object
- `429 TOO_MANY_REQUESTS` - Rate limit exceeded

**Example Request:**
```
POST /api/reporting/poll-day/ward-age-groups/recompute?accountId=1&electionId=100&partNumber=7
```

---

## JSON Response Structure

### Age Group Breakdown JSON
The `ageGroupBreakdownJson` field contains:

```json
{
  "18_21": {
    "total_voters": 150,
    "polled_2025": 135,
    "polled_2024": 120,
    "did_not_vote": 15,
    "is_first_time": true
  },
  "22_25": {
    "total_voters": 200,
    "polled_2025": 180,
    "polled_2024": 170,
    "did_not_vote": 20,
    "is_first_time": false
  },
  "26_35": {
    "total_voters": 350,
    "polled_2025": 315,
    "polled_2024": 300,
    "did_not_vote": 35,
    "is_first_time": false
  },
  "36_45": {
    "total_voters": 300,
    "polled_2025": 270,
    "polled_2024": 260,
    "did_not_vote": 30,
    "is_first_time": false
  },
  "46_59": {
    "total_voters": 250,
    "polled_2025": 225,
    "polled_2024": 210,
    "did_not_vote": 25,
    "is_first_time": false
  },
  "expired": {
    "total_voters": 250,
    "polled_2025": 0,
    "polled_2024": 0,
    "did_not_vote": 250,
    "is_first_time": false
  },
  "overall": {
    "total_voters": 1500,
    "polled_2025": 1125,
    "percentage": 75.0
  }
}
```

### Age Group Categories
- `18_21` - First-time voters (ages 18-21)
- `22_25` - Young voters (ages 22-25)
- `26_35` - Young adults (ages 26-35)
- `36_45` - Mature voters (ages 36-45)
- `46_59` - Senior voters (ages 46-59)
- `expired` - Voters aged 60+ (considered expired/inactive)
- `overall` - Aggregated statistics across all age groups

### Field Descriptions
- `total_voters` - Total registered voters in this age group
- `polled_<YEAR>` - Number of voters who voted in the specified year
- `did_not_vote` - Number of voters who did not vote in current polling date
- `is_first_time` - Boolean indicating if this is a first-time voter group (18-21)
- `percentage` - Overall turnout percentage (only in overall section)

---

## Features

✅ **Ward/Part-Specific Analysis** - Filter by specific ward (part_number)  
✅ **Fine-Grained Age Brackets** - 18-21, 22-25, 26-35, 36-45, 46-59, 60+  
✅ **Historical Comparison** - Year-over-year comparison (2024 vs 2025)  
✅ **First-Time Voter Identification** - Special flag for 18-21 age group  
✅ **Expired Voter Tracking** - Separate category for voters 60+  
✅ **Non-Voter Tracking** - Count of voters who didn't vote  
✅ **Overall Statistics** - Aggregated turnout percentage  
✅ **Rate Limiting** - Prevents abuse of recompute endpoint  
✅ **ETag Caching** - Efficient HTTP caching support  
✅ **IST Timezone** - All dates in Asia/Kolkata timezone  

---

## Database Schema

**Table:** `poll_day_ward_age_group_turnout`

```sql
CREATE TABLE poll_day_ward_age_group_turnout (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    part_number VARCHAR(50) NOT NULL,
    polling_date DATE NOT NULL,
    age_group_breakdown_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    computed_at TIMESTAMPTZ,
    refreshed_at TIMESTAMPTZ,
    CONSTRAINT uq_poll_day_ward_age_group UNIQUE (account_id, election_id, part_number, polling_date)
);
```

**Indexes:**
- `idx_poll_day_ward_age_account_election_part_date` - Composite index for query optimization
- `idx_poll_day_ward_age_polling_date` - Index on polling_date

---

## Migration

**File:** `V908__reporting_poll_day_ward_age_group.sql`

Run this migration to create the necessary database table and indexes.

---

## Implementation Notes

### SQL Query Logic
The computation uses a CTE to categorize voters by age group and then aggregates:
1. Voters are categorized into age brackets
2. Historical votes are tracked by year (extracted from voted_timestamp)
3. Non-voters are calculated by excluding current day voters
4. Overall percentage is computed across all age groups

### Year Detection
- Current year is extracted from the `pollingDate` parameter
- Previous year is calculated as `currentYear - 1`
- Vote year is extracted from `voted_timestamp` in IST timezone

### Performance
- Indexed queries for fast retrieval
- JSONB storage for flexible schema
- Rate limiting on recompute to prevent abuse
- 30-second HTTP cache to reduce database load

---

## Backward Compatibility

✅ **Existing APIs Not Affected** - This is a new endpoint with no changes to existing poll day APIs:
- `/api/reporting/poll-day/booth-summary` - unchanged
- `/api/reporting/poll-day/hourly` - unchanged
- `/api/reporting/poll-day/age-groups` - unchanged

---

## Usage Example for Chart

For the chart shown (Ward no: 7, Overall Polled percentage: 75%):

```javascript
const response = await fetch(
  '/api/reporting/poll-day/ward-age-groups?accountId=1&electionId=100&partNumber=7&pollingDate=2025-10-14'
);
const data = await response.json();
const breakdown = JSON.parse(data.ageGroupBreakdownJson);

// Chart data
const ageGroups = ['18-21', '22-25', '26-35', '36-45', '46-59'];
const totalVoters = ageGroups.map(key => breakdown[key.replace('-', '_')].total_voters);
const polled2024 = ageGroups.map(key => breakdown[key.replace('-', '_')].polled_2024);
const polled2025 = ageGroups.map(key => breakdown[key.replace('-', '_')].polled_2025);
const didNotVote = ageGroups.map(key => breakdown[key.replace('-', '_')].did_not_vote);

// Overall percentage
const overallPercentage = breakdown.overall.percentage; // 75.0
```

---

## Testing

### Test GET Endpoint
```bash
curl -X GET "http://localhost:8080/api/reporting/poll-day/ward-age-groups?accountId=1&electionId=100&partNumber=7"
```

### Test Recompute
```bash
curl -X POST "http://localhost:8080/api/reporting/poll-day/ward-age-groups/recompute?accountId=1&electionId=100&partNumber=7"
```

---

## Related Files

### Java Classes (in thedal-app)
- `com.thedal.thedal_app.report.pollday.PollDayWardAgeGroupTurnout.java` - Entity class
- `com.thedal.thedal_app.report.pollday.PollDayWardAgeGroupTurnoutRepository.java` - JPA repository
- `com.thedal.thedal_app.report.pollday.PollDayWardAgeGroupService.java` - Business logic service
- `com.thedal.thedal_app.report.pollday.PollDayWardAgeGroupController.java` - REST controller

### SQL Migration
- `thedal-app/src/main/resources/db/migration/V908__reporting_poll_day_ward_age_group.sql` - Database migration

---

## Support

For issues or questions, contact the development team.
