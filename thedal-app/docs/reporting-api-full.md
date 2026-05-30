# Full Reporting API Reference

Comprehensive reference for all reporting & dashboard aggregates: core election dashboard, cadre dashboard, and poll‑day dashboards. Provides endpoints, parameters, payload schemas, recompute behavior, caching, rate limits, and integration notes.

---
## 1. Overview
All reporting slices follow a consistent pattern:
- GET endpoint returns last computed snapshot (cached 30s at HTTP layer via Cache-Control + ETag)
- POST recompute endpoint triggers synchronous regeneration (subject to rate limiting per slice + accountId + electionId)
- Scheduler refresh runs every 5 minutes (if AGGREGATION_ENABLED=true)

Time zone: All timestamps stored/returned in UTC unless otherwise stated. Poll-Day dashboards interpret date & hourly buckets in IST (Asia/Kolkata) but may still serialize timestamps with offset.

---
## 2. Endpoint Index (Quick Table)

| Slice | GET Path | POST Recompute | Params (R=Required, O=Optional) | Notes |
|-------|----------|----------------|---------------------------------|-------|
| Election Stats | /reporting/api/aggregates/election/{accountId}/{electionId} | /reporting/api/aggregates/election/{accountId}/{electionId}/recompute | accountId (R, path) electionId (R, path) | Core voter & age/gender buckets |
| Demographics | /reporting/api/aggregates/election/demographics/{accountId}/{electionId} | /reporting/api/aggregates/election/demographics/{accountId}/{electionId}/recompute | accountId (R) electionId (R) | casteJson / religionJson / languageJson / relationJson |
| Booth Progress | /reporting/api/aggregates/election/booth-progress/{accountId}/{electionId} | /reporting/api/aggregates/election/booth-progress/{accountId}/{electionId}/recompute | accountId (R) electionId (R) | boothProgressJson (map booth -> {total,voted}) |
| Party Polling | /reporting/api/aggregates/election/party-polling/{accountId}/{electionId} | /reporting/api/aggregates/election/party-polling/{accountId}/{electionId}/recompute | accountId (R) electionId (R) | partyCountsJson (partyId->count) |
| Feedback Issues | /reporting/api/aggregates/election/feedback-issues/{accountId}/{electionId} | /reporting/api/aggregates/election/feedback-issues/{accountId}/{electionId}/recompute | accountId (R) electionId (R) | issuesJson (issueType->count) |
| Contact Status | /reporting/api/aggregates/election/contact-status/{accountId}/{electionId} | /reporting/api/aggregates/election/contact-status/{accountId}/{electionId}/recompute | accountId (R) electionId (R) | contactStatusJson (status->count) |
| Cadre Dashboard | /api/reporting/cadre-dashboard?accountId=..&electionId=.. | /api/reporting/cadre-dashboard/recompute?accountId=..&electionId=.. | accountId (R, query) electionId (R, query) | Cadre engagement & profile update metrics |
| Poll-Day Hourly | /api/reporting/poll-day/hourly?accountId=..&electionId=..[&pollingDate=YYYY-MM-DD] | /api/reporting/poll-day/hourly/recompute?... | accountId (R) electionId (R) pollingDate (O, query) | hourlyJson bucketed 00-23 |
| Poll-Day Age Groups | /api/reporting/poll-day/age-groups?accountId=..&electionId=..[&pollingDate=YYYY-MM-DD] | /api/reporting/poll-day/age-groups/recompute?... | accountId (R) electionId (R) pollingDate (O) | ageGroupsJson groups with registered/voted/pct |
| Poll-Day Booth Summary | /api/reporting/poll-day/booth-summary?accountId=..&electionId=..[&pollingDate=YYYY-MM-DD] | /api/reporting/poll-day/booth-summary/recompute?... | accountId (R) electionId (R) pollingDate (O) | boothSummaryJson booth -> {total,voted,pct,lastVote} |
| Replica Lag (internal) | /internal/replica/lag | N/A | none | Replica replication delay diagnostics |

---
## 3. Shared Response Metadata
Common fields (per slice record / response wrapper):
- accountId
- electionId
- computedAt (OffsetDateTime string of first insertion)
- refreshedAt (OffsetDateTime string of last successful aggregation)
- freshnessSeconds (derived server-side age at response time)

HTTP Headers:
- ETag: epoch seconds (some controllers use milliseconds quoted) of refreshedAt; treat value as opaque string
- Cache-Control: public, max-age=30

Status Codes:
- 200 OK: Snapshot returned
- 202 Accepted: (rare) recompute triggered but row not yet persisted (stats slice only)
- 404 Not Found: No snapshot row yet (poll-day endpoints return empty object instead of 404 if absent)
- 429 Too Many Requests: Recompute throttled by rate limiter

---
## 4. Slices Detail
### 4.1 Election Stats
GET /reporting/api/aggregates/election/{accountId}/{electionId}
Path Params: accountId (Long), electionId (Long)
Example Response:
{
  "accountId": 12,
  "electionId": 45,
  "totalVoters": 52340,
  "male": 26000,
  "female": 25500,
  "transgender": 40,
  "age18To30": 8200,
  "age30To40": 11000,
  "age40To50": 14000,
  "age50To60": 12000,
  "age60To70": 7130,
  "ageGreaterThan70": 470,
  "firstTimeVoters": 1200,
  "seniorCitizens": 2400,
  "superSeniors": 180,
  "computedAt": "2025-09-03T13:54:12Z",
  "refreshedAt": "2025-09-03T14:00:00Z",
  "freshnessSeconds": 25
}
POST /reporting/api/aggregates/election/{accountId}/{electionId}/recompute

Sample Recompute:
Request: POST /reporting/api/aggregates/election/12/45/recompute
Response 200:
{
  "accountId":12,
  "electionId":45,
  "totalVoters":52340,
  "refreshedAt":"2025-09-03T14:05:33Z",
  "freshnessSeconds":0
}

### 4.2 Demographics
GET /reporting/api/aggregates/election/demographics/{accountId}/{electionId}
Path Params: accountId, electionId
Fields: casteJson, religionJson, languageJson, relationJson (all stringified JSON maps)
- relationJson: Counts by relation type (e.g., "father", "husband", "mother")
Example (abridged):
{
  "accountId":12,
  "electionId":45,
  "casteJson":"{\"SC\":1200,\"ST\":900,\"OBC\":4100,\"GEN\":3000}",
  "religionJson":"{\"hindu\":8500,\"muslim\":650}",
  "languageJson":"{\"tamil\":5200,\"telugu\":3100}",
  "relationJson":"{\"father\":4500,\"husband\":2800,\"mother\":1200}",
  "computedAt":"2025-09-03T13:54:12Z",
  "refreshedAt":"2025-09-03T14:02:05Z"
}
POST /reporting/api/aggregates/election/demographics/{accountId}/{electionId}/recompute

Sample Recompute Response:
{
  "accountId":12,
  "electionId":45,
  "casteJson":"{...}",
  "religionJson":"{...}",
  "languageJson":"{...}",
  "relationJson":"{...}",
  "refreshedAt":"2025-09-03T14:06:10Z",
  "freshnessSeconds":0
}

### 4.3 Booth Progress
GET /reporting/api/aggregates/election/booth-progress/{accountId}/{electionId}
Path Params: accountId, electionId
boothProgressJson: JSON map boothNumber -> { total, voted }
POST /reporting/api/aggregates/election/booth-progress/{accountId}/{electionId}/recompute

Sample Recompute Response:
{
  "accountId":12,
  "electionId":45,
  "boothProgressJson":"{...updated...}",
  "refreshedAt":"2025-09-03T14:07:00Z",
  "freshnessSeconds":0
}

### 4.4 Party Polling
GET /reporting/api/aggregates/election/party-polling/{accountId}/{electionId}
Path Params: accountId, electionId
partyCountsJson: JSON map partyId (string) -> count
POST /reporting/api/aggregates/election/party-polling/{accountId}/{electionId}/recompute

Sample Recompute Response:
{
  "accountId":12,
  "electionId":45,
  "partyCountsJson":"{\"1\":130,\"2\":90,\"unknown\":17}",
  "refreshedAt":"2025-09-03T14:07:42Z",
  "freshnessSeconds":0
}

### 4.5 Feedback Issues
GET /reporting/api/aggregates/election/feedback-issues/{accountId}/{electionId}
Path Params: accountId, electionId
issuesJson: JSON map issueType -> count
POST /reporting/api/aggregates/election/feedback-issues/{accountId}/{electionId}/recompute

Sample Recompute Response:
{
  "accountId":12,
  "electionId":45,
  "issueCountsJson":"{\"road\":16,\"water\":9,\"electricity\":4}",
  "refreshedAt":"2025-09-03T14:08:05Z",
  "freshnessSeconds":0
}

### 4.6 Contact Status
GET /reporting/api/aggregates/election/contact-status/{accountId}/{electionId}
Path Params: accountId, electionId
contactStatusJson keys (sample): has_mobile, no_mobile, mobile_verified_true, mobile_verified_false, aadhaar_verified_true, aadhaar_verified_false, member_verified_true, member_verified_false
POST /reporting/api/aggregates/election/contact-status/{accountId}/{electionId}/recompute

Sample Recompute Response:
{
  "accountId":12,
  "electionId":45,
  "contactStatusJson":"{\"has_mobile\":40000,\"no_mobile\":12340,...}",
  "refreshedAt":"2025-09-03T14:08:40Z",
  "freshnessSeconds":0
}

### 4.7 Cadre Dashboard
GET /api/reporting/cadre-dashboard?accountId=12&electionId=45
Fields: totalCadres, cadresLogged, cadresNotLogged, boothsAssigned, totalMobileUpdated, totalDobUpdated, totalPartyUpdated, totalCasteUpdated, totalReligionUpdated, totalLanguageUpdated, top10Cadres (JSON array), least10Cadres (JSON array), computedAt, refreshedAt.
POST /api/reporting/cadre-dashboard/recompute?accountId=12&electionId=45
Notes:
- "Logged" = any voter create/update activity (current definition)
- Performance arrays ordered by total_voter_created desc/asc

Sample Recompute Response:
{
  "accountId":12,
  "electionId":45,
  "totalCadres":42,
  "cadresLogged":19,
  "cadresNotLogged":23,
  "refreshedAt":"2025-09-03T14:09:10Z",
  "freshnessSeconds":0
}

### 4.8 Poll-Day Hourly Turnout
GET /api/reporting/poll-day/hourly?accountId=12&electionId=45[&pollingDate=YYYY-MM-DD]
Hourly JSON: "{\"06\":{\"voted\":25},\"07\":{\"voted\":48},...}"
POST /api/reporting/poll-day/hourly/recompute?accountId=12&electionId=45[&pollingDate=YYYY-MM-DD]
IST aware: Buckets 00–23 represent local hour of voting day.

Sample Recompute Response:
{
  "accountId":12,
  "electionId":45,
  "pollingDate":"2025-09-03",
  "hourlyJson":"{...}",
  "refreshedAt":"2025-09-03T14:09:40+05:30",
  "freshnessSeconds":0
}

### 4.9 Poll-Day Age Groups
GET /api/reporting/poll-day/age-groups?accountId=12&electionId=45[&pollingDate=YYYY-MM-DD]
ageGroupsJson object of groups: 18_30, 30_40, 40_50, 50_60, 60_70, gt_70, unknown each -> { registered, voted, pct }
POST /api/reporting/poll-day/age-groups/recompute?accountId=12&electionId=45[&pollingDate=YYYY-MM-DD]

Sample Recompute Response:
{
  "accountId":12,
  "electionId":45,
  "pollingDate":"2025-09-03",
  "ageGroupsJson":"{...}",
  "refreshedAt":"2025-09-03T14:10:05+05:30",
  "freshnessSeconds":0
}

### 4.10 Poll-Day Booth Summary
GET /api/reporting/poll-day/booth-summary?accountId=12&electionId=45[&pollingDate=YYYY-MM-DD]
boothSummaryJson: boothNumber -> { total, voted, pct, lastVote }

### 4.11 Replica Lag (Internal Diagnostics)
GET /internal/replica/lag
Response: { "replicaLagSeconds": <long>, "source": "<details>" ... }
Use: Ops / monitoring only (not for public clients).
POST /api/reporting/poll-day/booth-summary/recompute?accountId=12&electionId=45[&pollingDate=YYYY-MM-DD]

Sample Recompute Response:
{
  "accountId":12,
  "electionId":45,
  "pollingDate":"2025-09-03",
  "boothSummaryJson":"{...}",
  "refreshedAt":"2025-09-03T14:10:30+05:30",
  "freshnessSeconds":0
}

---
## 5. Recompute Semantics & Rate Limiting
- Interval: REPORTING_RECOMPUTE_MIN_INTERVAL_SEC (default 30s) per (slice, accountId, electionId)
- 429 if attempted earlier
- Idempotent: multiple recompute calls within same second after first completion just return same snapshot
- UI Suggestion: Disable button until freshnessSeconds >= 15 or show countdown

---
## 6. Caching & ETags
Send If-None-Match with last ETag to get 304 when unchanged.
Example:
1. GET slice -> 200 ETag: "1725350502"
2. Within 30s GET + If-None-Match:"1725350502" -> 304 (empty body)
3. After recompute / expiry -> 200 new ETag

---
## 7. Environment Variables
| Name | Description | Default |
|------|-------------|---------|
| AGGREGATION_ENABLED | Enable scheduled 5‑min refresh cycle | true |
| REPORTING_RECOMPUTE_MIN_INTERVAL_SEC | Rate limit window seconds | 30 |

---
## 8. Time Zone Handling
- Poll-day endpoints interpret pollingDate in IST (Asia/Kolkata) and default to current IST date when absent.
- Hourly buckets: zero-padded "00".."23" (local IST hours).
- Other timestamps are OffsetDateTime (often UTC) — treat as zoned; convert client-side.
- Recommendation: unify display in IST for election operations UIs.

---
## 9. Front-End Integration Tips
- Parse JSON string fields only once (memoize parsed object in state)
- Compare ETag before re-render heavy charts
- Show freshnessSeconds; warn if > (scheduler interval * 2)
- Poll GET every 30–60s; POST only by operator action
- Gracefully handle 404 by showing "No data yet" and enabling recompute

---
## 10. Error Reference
| Status | Scenario | Mitigation |
|--------|----------|------------|
| 400 | Invalid path/query param format | Validate before request |
| 404 | Aggregate not yet computed | Trigger POST or wait scheduler |
| 429 | Rate limit on recompute | Wait interval & retry |

---
## 11. Change Log (Doc)
- v1.1 Added replica lag endpoint, clarified field names, param table (2025-09-04)
- v1.0 Initial unified reference combining core + cadre + poll-day (2025-09-03)

---
## 12. Future Extensions
- Delta-based recompute (watermarking)
- Role-based authorization on POST recompute
- Historical time-series tables for trend charts
- Redis/memcached layer for heavy traffic slices

---
End of document.
