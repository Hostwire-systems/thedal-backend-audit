# Reporting Aggregates API

Base path: `/reporting/api/aggregates/election`

## Endpoints (GET)
| Slice | Path | Description |
|-------|------|-------------|
| Stats | `/{accountId}/{electionId}` | Core counts & age/gender buckets |
| Demographics | `/demographics/{accountId}/{electionId}` | Caste, religion, language, relation distributions |
| Booth Progress | `/booth-progress/{accountId}/{electionId}` | Per-booth total & voted counts (JSON) |
| Party Polling | `/party-polling/{accountId}/{electionId}` | Counts per party id (JSON) |
| Feedback Issues | `/feedback-issues/{accountId}/{electionId}` | Issue name -> count |
| Contact Status | `/contact-status/{accountId}/{electionId}` | Contact / verification flags summary |
| Cadre Dashboard | (separate base) `/api/reporting/cadre-dashboard?accountId=..&electionId=..` | Cadre engagement + attribute update counts + top/least performers |
| Poll-Day Hourly | (separate base) `/api/reporting/poll-day/hourly?accountId=..&electionId=..&pollingDate=..` | Hourly turnout counts for polling day (IST timezone) |
| Poll-Day Age Groups | (separate base) `/api/reporting/poll-day/age-groups?accountId=..&electionId=..&pollingDate=..` | Age group polled percentages for polling day |
| Poll-Day Booth Summary | (separate base) `/api/reporting/poll-day/booth-summary?accountId=..&electionId=..&pollingDate=..` | Booth-wise turnout summary with last vote timestamp |

All GET responses include:
- `computedAt`: initial creation timestamp
- `refreshedAt`: last recompute timestamp
- `freshnessSeconds`: age of data (seconds)
- HTTP headers: `ETag` (epoch seconds of `refreshedAt`), `Cache-Control: public, max-age=30`

## Recompute Endpoints (POST)
`POST` each slice with suffix `/recompute`:
- Stats: `/{accountId}/{electionId}/recompute`
- Demographics: `/demographics/{accountId}/{electionId}/recompute`
- Booth Progress: `/booth-progress/{accountId}/{electionId}/recompute`
- Party Polling: `/party-polling/{accountId}/{electionId}/recompute`
- Feedback Issues: `/feedback-issues/{accountId}/{electionId}/recompute`
- Contact Status: `/contact-status/{accountId}/{electionId}/recompute`
- Cadre Dashboard: `POST /api/reporting/cadre-dashboard/recompute?accountId=..&electionId=..`
- Poll-Day Hourly: `POST /api/reporting/poll-day/hourly/recompute?accountId=..&electionId=..&pollingDate=..`
- Poll-Day Age Groups: `POST /api/reporting/poll-day/age-groups/recompute?accountId=..&electionId=..&pollingDate=..`
- Poll-Day Booth Summary: `POST /api/reporting/poll-day/booth-summary/recompute?accountId=..&electionId=..&pollingDate=..`

Rate limit: default 1 request / 30s per (slice, accountId, electionId). 429 returned if exceeded. Configure via env var `REPORTING_RECOMPUTE_MIN_INTERVAL_SEC`.

## Sample GET (Party Polling)
```
GET /reporting/api/aggregates/election/party-polling/12/45
200 OK
ETag: "1725350502"
Cache-Control: public, max-age=30
{
  "accountId":12,
  "electionId":45,
  "partyCountsJson":"{\"1\":123,\"2\":88,\"unknown\":17}",
  "computedAt":"2025-09-03T13:54:12Z",
  "refreshedAt":"2025-09-03T14:00:00Z",
  "freshnessSeconds":42
}
```

## Sample POST Recompute (Demographics)
```
POST /reporting/api/aggregates/election/demographics/12/45/recompute
200 OK
{
  "accountId":12,
  "electionId":45,
  "casteJson":"{...}",
  "religionJson":"{...}",
  "languageJson":"{...}",
  "relationJson":"{...}",
  "computedAt":"2025-09-03T13:54:12Z",
  "refreshedAt":"2025-09-03T14:02:05Z",
  "freshnessSeconds":0
}
```

## Contact Status JSON Keys
- `has_mobile`
- `no_mobile`
- `mobile_verified_true` / `_false`
- `aadhaar_verified_true` / `_false`
- `member_verified_true` / `_false`

## Operational Notes
- Scheduler runs every 5 minutes (cron in `ElectionDashboardAggregationScheduler`).
- All aggregates currently recompute full data set; optimization (delta mode) pending.
- Feature toggle: `AGGREGATION_ENABLED` (true by default) disables scheduled runs.
- Cadre dashboard aggregation added (table `cadre_dashboard_stats`, migration V906). Scheduler invokes after other slices.

## Cadre Dashboard Semantics
| Field | Meaning | Source Logic |
|-------|---------|--------------|
| totalCadres | Count of volunteers | `volunteers` table rows filtered by account & election |
| cadresLogged | Volunteers with voter activity | Rows in `volunteer_vs_voter_report` where (created+updated) > 0 |
| cadresNotLogged | totalCadres - cadresLogged (floored at 0) | Derived |
| boothsAssigned | Sum of assigned booth list lengths | `volunteer_entity_assigned_booth` join volunteers |
| total_*_updated | Aggregated attribute update counters | SUM of corresponding columns in `volunteer_vs_voter_report` |
| top10Cadres | JSON array of top 10 by `total_voter_created` | Query ORDER BY created DESC LIMIT 10 |
| least10Cadres | JSON array of bottom 10 by `total_voter_created` | Query ORDER BY created ASC LIMIT 10 |
| totalLanguageUpdated | Placeholder always 0 (awaiting language capture) | N/A |

Example GET:
```
GET /api/reporting/cadre-dashboard?accountId=12&electionId=45
200 OK
{
  "accountId":12,
  "electionId":45,
  "totalCadres":42,
  "cadresLogged":18,
  "cadresNotLogged":24,
  "boothsAssigned":57,
  "totalMobileUpdated":120,
  "totalDobUpdated":30,
  "totalPartyUpdated":15,
  "totalCasteUpdated":9,
  "totalReligionUpdated":11,
  "totalLanguageUpdated":0,
  "top10Cadres":"[{\"userId\":101,\"value\":25},...]",
  "least10Cadres":"[{\"userId\":115,\"value\":0},...]",
  "computedAt":"2025-09-03T14:05:00Z",
  "refreshedAt":"2025-09-03T14:05:00Z",
  "refreshedAt":"2025-09-03T14:05:00Z"
}
```

### Cadre Dashboard Assumptions & Open Questions
1. "Logged" definition = any voter create/update (may refine to time window or session tracking later).
2. Performance metric uses voters created (not updated) for ordering; can switch to created+updated or separate engagement score.
3. Language updates not yet captured; field reserved for future adoption in source tables.
4. No pagination for performance lists (fixed size 10 + 10); could expose variable N later.
5. Rate limiting identical to other slices (shared `RecomputeRateLimiter`).

## Future Enhancements (planned)
- Delta recompute using max(updated_at) watermark per election.
- Authorization guard restricting recompute to privileged roles.
- Temporal trend tables (if UI requires time series graphs).
- Optional Redis caching layer for high-traffic slices.

## Troubleshooting
| Symptom | Possible Cause | Action |
|---------|----------------|--------|
| 404 on GET | No aggregate row yet | Trigger POST /recompute or wait for scheduler |
| 429 on POST | Rate limit | Retry after interval or raise `REPORTING_RECOMPUTE_MIN_INTERVAL_SEC` |
| Stale freshnessSeconds | Scheduler disabled | Check `AGGREGATION_ENABLED` env |

## Environment Variables
| Name | Purpose | Default |
|------|---------|---------|
| `AGGREGATION_ENABLED` | Enable scheduled aggregation | true |
| `REPORTING_RECOMPUTE_MIN_INTERVAL_SEC` | Min seconds between recomputes per key | 30 |

## Poll-Day Dashboard

The poll-day dashboard provides real-time turnout analytics for active polling dates, using India Standard Time (IST, UTC+05:30) for hourly bucketing and date calculations.

### Endpoints

**GET Hourly Turnout:**
```
GET /api/reporting/poll-day/hourly?accountId=12&electionId=45&pollingDate=2025-09-03
```

**GET Age Group Turnout:**
```
GET /api/reporting/poll-day/age-groups?accountId=12&electionId=45&pollingDate=2025-09-03
```

**GET Booth Summary:**
```
GET /api/reporting/poll-day/booth-summary?accountId=12&electionId=45&pollingDate=2025-09-03
```

All GET endpoints:
- `pollingDate` parameter is optional (defaults to current IST date)
- Return empty entities if no data exists yet
- Include standard ETag and Cache-Control headers
- Support manual POST recompute with same rate limiting

### Data Structures

**Hourly Turnout JSON Format:**
```json
{
  "accountId": 12,
  "electionId": 45,
  "pollingDate": "2025-09-03",
  "hourlyJson": "{\"06\":{\"voted\":25},\"07\":{\"voted\":48},\"08\":{\"voted\":73},...,\"18\":{\"voted\":1245}}",
  "computedAt": "2025-09-03T08:15:00+05:30",
  "refreshedAt": "2025-09-03T08:15:00+05:30"
}
```

**Age Group Turnout JSON Format:**
```json
{
  "accountId": 12,
  "electionId": 45,
  "pollingDate": "2025-09-03",
  "ageGroupsJson": "{\"18_30\":{\"registered\":1200,\"voted\":240,\"pct\":20.0},\"30_40\":{\"registered\":980,\"voted\":294,\"pct\":30.0},...}",
  "computedAt": "2025-09-03T08:15:00+05:30",
  "refreshedAt": "2025-09-03T08:15:00+05:30"
}
```

**Booth Summary JSON Format:**
```json
{
  "accountId": 12,
  "electionId": 45,
  "pollingDate": "2025-09-03",
  "boothSummaryJson": "{\"12\":{\"total\":450,\"voted\":135,\"pct\":30.0,\"lastVote\":\"2025-09-03T08:12:00\"},\"15\":{\"total\":380,\"voted\":95,\"pct\":25.0,\"lastVote\":\"2025-09-03T08:10:00\"},...}",
  "computedAt": "2025-09-03T08:15:00+05:30",
  "refreshedAt": "2025-09-03T08:15:00+05:30"
}
```

### Age Group Definitions
- `18_30`: Ages 18-30 (inclusive)
- `30_40`: Ages 31-40 (inclusive)
- `40_50`: Ages 41-50 (inclusive)
- `50_60`: Ages 51-60 (inclusive)
- `60_70`: Ages 61-70 (inclusive)
- `gt_70`: Ages 71 and above
- `unknown`: NULL or invalid age values

### Technical Implementation
- Data sourced from `_voters` table using `has_voted` and `voted_timestamp` fields
- Hourly bucketing based on `EXTRACT(HOUR FROM voted_timestamp)` in IST
- Age calculations derived from existing `age` column
- Booth numbers from `booth_number` column
- All aggregations filtered by polling date range (start of day to start of next day)
- Scheduler runs every 5 minutes, aggregating for current IST date
- Tables: `poll_day_hourly_turnout`, `poll_day_age_group_turnout`, `poll_day_booth_summary`

### Performance Notes
- Uses optimized indexes on `(account_id, election_id, polling_date)` for fast lookups
- JSON serialization minimizes response size while maintaining flexibility
- Timestamp filtering leverages existing voter table indexes
- Manual recompute available for immediate data refresh during active polling

### Usage Examples

**Real-time hourly monitoring:**
```bash
curl "http://localhost:8080/api/reporting/poll-day/hourly?accountId=12&electionId=45"
# Returns current date turnout by hour (IST)
```

**Historical polling day analysis:**
```bash
curl "http://localhost:8080/api/reporting/poll-day/age-groups?accountId=12&electionId=45&pollingDate=2025-08-15"
# Returns age group breakdown for specific past election date
```

**Force refresh during active polling:**
```bash
curl -X POST "http://localhost:8080/api/reporting/poll-day/booth-summary/recompute?accountId=12&electionId=45"
# Immediately recomputes current date booth summary
```
