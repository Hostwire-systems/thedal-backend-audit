# Election Data Merge API

This document describes the election-to-election voter data merge endpoints exposed by the backend. These APIs allow performing a dry-run analysis and then enqueuing an asynchronous merge job that copies selected voter data fields from a *source* election into an existing *target* election for voters whose EPIC numbers match.

Base path (all endpoints):
```
/api/elections/{targetElectionId}/merge
```
`{targetElectionId}`: Long – ID of the destination (target) election that will receive updates.

> NOTE: Authentication / account context is currently mocked in controller (placeholders `currentAccountId()` / `currentUserId()`). Real deployment should replace with security context.

## Supported Merge Fields
Enum: `MergeField`

| Field | UI Label (suggested) | Type | Merge Rule | Missing Ref Behavior | Status |
|-------|----------------------|------|------------|----------------------|--------|
| VOTER_HISTORY | Voter History | Set<Reference> | Replace set with resolvable subset (exact name match, case-insensitive) | Unresolved names listed in missing refs | Supported |
| MOBILE_NUMBER | Mobile Number | String | Overwrite if source non-null and different | N/A | Supported |
| WHATSAPP_NUMBER | WhatsApp Number | String | Overwrite if source non-null and different | N/A | Supported |
| LOCATION | Address & Geo | Composite | Copy differing components (address, pincode, part/voter lat/long) | N/A | Supported |
| DATE_OF_BIRTH | Date of Birth | Date | Overwrite if source non-null and different | N/A | Supported |
| EMAIL_ID | Email | String | Overwrite if source non-null and different | N/A | Supported |
| RELIGION | Religion | Reference | Replace with target ref by name | Missing names recorded | Supported |
| CASTE_CATEGORY | Caste Category | Reference | Replace with target ref by name | Missing names recorded | Supported |
| CASTE | Caste | Reference | Replace with target ref by name | Missing names recorded | Supported |
| SUB_CASTE | Sub Caste | Reference | Replace with target ref by name | Missing names recorded | Supported |
| PARTY | Party | Reference | Replace with target ref by name | Missing names recorded | Supported |
| VOTER_CATEGORY | Voter Category / Availability | Reference | Replace with target ref by name | Missing names recorded | Supported |
| LANGUAGE | Languages | Set<Reference> | Replace set with resolvable subset | Missing names recorded | Supported |
| FEEDBACK | Feedback Issues | Set<Reference> | Replace set with resolvable subset | Missing names recorded | Supported |
| AADHAAR_NUMBER | Aadhaar Number | String | Overwrite if source non-null and different | N/A | Supported |
| PAN_NUMBER | PAN Number | String | Overwrite if source non-null and different | N/A | Supported |
| MEMBERSHIP_NUMBER | Membership Number | (Unused) | Not merged | Adds UNSUPPORTED entry | Unsupported |
| REMARKS | Remarks | String | Overwrite if source non-null and different | N/A | Supported |
| FAMILY_MAPPING | Family Mapping | Relationship | Not processed | N/A | Skipped |
| FRIENDS_MAPPING | Friends Mapping | Relationship | Not processed | N/A | Skipped |

Rules summary:
- Reference resolution is case-insensitive after trim.
- Null source never overwrites non-null target.
- Set fields: after resolving names, only overwrite if membership differs.
- Unsupported / skipped fields should be disabled in UI.

Frontend selection guidance:
- Show only Supported fields selectable. Display Unsupported/Skipped disabled with tooltip.
- Avoid sending duplicates; backend does not dedupe.

## 1. Dry Run Merge Analysis
```
POST /api/elections/{targetElectionId}/merge/dry-run
```
Run an in-memory analysis without persisting changes. Validates selection and reports stats & feasibility.

### Request Body (JSON)
```
{
  "sourceElectionId": 1234,          // Long, required (must differ from targetElectionId)
  "fields": ["MOBILE_NUMBER", "RELIGION", "CASTE"], // Array<MergeField>, required (non-empty)
  "dryRun": true                     // Boolean (ignored; endpoint enforces dry run)
}
```
Constraints:
- `sourceElectionId` ≠ `targetElectionId`.
- `fields` must be non-empty.

### Successful Response (HTTP 200)
Wrapper: `ThedalResponse` with `successCode` = generic SUCCESS.
`data` payload shape (class `MergeDryRunResultDTO`):
```
{
  "dryRun": true,
  "sourceElectionId": 111,
  "targetElectionId": 222,
  "selectedFields": ["RELIGION", "CASTE"],
  "votersMatched": 9500,                 // EPIC present in both elections
  "votersAffected": 4200,                // Upper-bound voters with at least one field change
  "missingEpicInTargetCount": 1200,      // Source EPICs not in target
  "missingEpicSample": ["ABC1234", "XYZ7777"],
  "fieldStats": {
     "RELIGION": {"willUpdate": 1500},
     "CASTE": {"willUpdate": 800}
  },
  "fieldAvailability": {
     "RELIGION": {"status": "PARTIAL", "missingNames": ["NewReligion1"]},
     "MEMBERSHIP_NUMBER": {"status": "UNSUPPORTED", "reason": "Not a voter field"}
  },
  "warnings": ["Membership Number not merged: field not present on voters"],
  "canProceed": true,
  "estimatedRuntimeSeconds": 0,          // Placeholder currently 0
  "generatedAt": "2025-09-03T10:15:30Z"
}
```
Notes:
- `votersAffected` is a rough upper bound (current logic picks max per-field update count).
- `fieldAvailability` only includes entries for partial/unsupported fields.
- For reference fields, `missingNames` lists values present in source but not defined in target reference tables.

#### Dry Run Response JSON Schema (informal)
```
ThedalResponse<MergeDryRunResultDTO> => {
  status: string,           // "success" | "error"
  code: number,
  message: string,
  data: {
    dryRun: boolean,
    sourceElectionId: number,
    targetElectionId: number,
    selectedFields: string[],
    votersMatched: number,
    votersAffected: number,
    missingEpicInTargetCount: number,
    missingEpicSample: string[],
    fieldStats: { [fieldName: string]: { willUpdate: number } },
    fieldAvailability: { [fieldName: string]: { status: "PARTIAL" | "UNSUPPORTED", missingNames?: string[], reason?: string } },
    warnings: string[],
    canProceed: boolean,
    estimatedRuntimeSeconds: number,
    generatedAt: string
  }
}
```

### Error Cases
- 400 INVALID_REQUEST: same source & target, or dry-run misuse.
- 400 MISSING_REQUIRED_FIELDS: empty field list.

## 2. Enqueue Merge Job
```
POST /api/elections/{targetElectionId}/merge
```
Queues an asynchronous merge job. Actual merging happens in background; endpoint returns a job ID.

### Request Body (JSON)
Same shape as dry run, but `dryRun` must be false or omitted:
```
{
  "sourceElectionId": 1234,
  "fields": ["RELIGION", "CASTE", "LANGUAGE"]
}
```
Validation:
- If `dryRun: true` is sent here, server returns 400 INVALID_REQUEST advising to use `/dry-run` endpoint.
- Concurrency: If a job with status PENDING or RUNNING already exists for the target election, returns 409 (BULK_UPLOAD_IN_PROGRESS error code reused).

### Successful Response (HTTP 202 Accepted)
#### Enqueue Request JSON Schema
```
{
  sourceElectionId: number,
  fields: string[]
}
```
`dryRun` must be omitted or false.

#### Enqueue Response Schema
```
ThedalResponse<{ jobId: string }>
```
```
{
  "status": "SUCCESS",
  "code": <numeric success code>,
  "message": "Success",
  "data": { "jobId": "4f5c0d7d-9b5b-4a5a-9d9d-7b1e0ac9ab12" }
}
```

## 3. Get Merge Job Status
```
GET /api/elections/{targetElectionId}/merge/jobs/{jobId}
```
Fetches current state or final result of a merge job.

### Successful Response (HTTP 200)
`data` contains serialized `MergeJobEntity` including runtime statistics (final stats embedded as JSON string in `resultStatsJson`).
```
{
  "status": "SUCCESS",
  "code": <numeric>,
  "message": "Success",
  "data": {
     "id": "4f5c0d7d-9b5b-4a5a-9d9d-7b1e0ac9ab12",
     "accountId": 10,
     "userId": 55,
     "sourceElectionId": 1234,
     "targetElectionId": 5678,
     "fields": ["RELIGION","CASTE"],
     "status": "RUNNING",               // PENDING | RUNNING | COMPLETED | FAILED | CANCELED
     "totalVoters": 10000,
     "processedVoters": 3200,
     "resultStatsJson": null,            // Filled once COMPLETED
     "errorMessage": null,               // Present if FAILED
     "createdAt": "2025-09-03T10:10:00Z",
     "startedAt": "2025-09-03T10:11:00Z",
     "finishedAt": null
  }
}
```
When `status` becomes COMPLETED:
- `resultStatsJson` holds a JSON object similar to:
```
{
  "totalSourceVoters": 10000,
  "matchedVoters": 9500,
  "updatedVoters": 4200,
  "fieldUpdateCounts": {"RELIGION":1500, "CASTE":800},
  "missingEpicInTargetSample": ["EPIC1","EPIC2"],
  "missingEpicInTargetCount": 1200,
  "missingReferenceCounts": {"RELIGION":1},
  "missingReferenceSamples": {"RELIGION":["NewReligion1"]},
  "missingReferenceTypes": ["RELIGION"],
  "updatedEpicSample": ["ABC1234", "XYZ7777"],
  "unmodifiedMatchedVoters": 5300,
  "durationSeconds": 27
}
```
If FAILED:
- `errorMessage` contains failure reason; `resultStatsJson` may be null or partial.

#### Job Status Response Schema (data)
```
{
  id: string,
  accountId: number,
  userId: number,
  sourceElectionId: number,
  targetElectionId: number,
  fields: string[],
  status: "PENDING" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELED",
  totalVoters: number | null,
  processedVoters: number,
  resultStatsJson: string | null,
  errorMessage: string | null,
  createdAt: string,
  startedAt: string | null,
  finishedAt: string | null
}
```

#### Final Stats JSON (resultStatsJson) Schema
```
{
  totalSourceVoters: number,
  matchedVoters: number,
  updatedVoters: number,
  fieldUpdateCounts: { [fieldName: string]: number },
  missingEpicInTargetSample?: string[],
  missingEpicInTargetCount: number,
  missingReferenceCounts?: { [refType: string]: number },
  missingReferenceSamples?: { [refType: string]: string[] },
  missingReferenceTypes?: string[],
  updatedEpicSample: string[],
  unmodifiedMatchedVoters: number,
  durationSeconds?: number
}
```

### Recommended Frontend Flow
1. User selects source election + target (implicit path) + fields.
2. POST dry-run.
3. Render stats; disable Proceed if `canProceed` false or no `fieldStats`.
4. POST enqueue; store jobId.
5. Poll job status every 3–5s until COMPLETED/FAILED.
6. On COMPLETED parse `resultStatsJson` and show summary.
7. On FAILED show `errorMessage` and allow new dry run.

Progress = processedVoters / totalVoters (guard null).

UI Edge Cases:
- No differences: `fieldStats` empty -> show informational message.
- Many missing references: show first 10–15 + count.
- Concurrency 409: disable enqueue until job finishes.

## Response Wrapper (`ThedalResponse`)
All endpoints return a wrapper (simplified view):
```
{
  "status": "SUCCESS" | "ERROR",
  "code": <numeric>,
  "message": "...",
  "data": <payload or null>
}
```

## Reference Merge Rules Summary
- Only voters whose EPIC exists in both elections are considered; no new voter creation.
- Scalar fields copied if source value is non-null and differs (target value overwritten).
- Location treated as composite: any difference across address/pincode/lat-long triggers update.
- Reference sets (LANGUAGE, FEEDBACK, VOTER_HISTORY) replaced with the resolvable subset of source set; missing names recorded.
- Unsupported / skipped fields appear in warnings and/or `fieldAvailability`.
- Job concurrency limited to one active (PENDING/RUNNING) job per target election.

## Error Handling & Codes

| Scenario | HTTP | Error Enum | Code | Frontend Action |
|----------|------|-----------|------|-----------------|
| Source == Target (dry run) | 400 | INVALID_REQUEST | 40412 | Show validation error |
| Empty field list | 400 | MISSING_REQUIRED_FIELDS | 40523 | Ask user to select fields |
| dryRun flag on enqueue | 400 | INVALID_REQUEST | 40412 | Remove dryRun; call /dry-run instead |
| Concurrent job | 409 | BULK_UPLOAD_IN_PROGRESS | 70227 | Inform user & poll existing job |
| JobId not found | 404 | JOB_NOT_FOUND | 60782 | Stop polling, surface error |
| Background failure | 200 (then FAILED status) | (varies) | code in error response | Show failure panel |

Error response shape: `{"status":"error","code":<number>,"message":"...","data":null}`.

## Field Value Constraints
- Enum values uppercase snake case.
- No duplicates in `fields` array.
- Use only defined values (skip unsupported fields on UI side).

## Future Optional Endpoints
- GET `/api/elections/{targetElectionId}/merge/jobs/latest`
- DELETE `/api/elections/{targetElectionId}/merge/jobs/{jobId}` (cancel)
- GET `/api/elections/{targetElectionId}/merge/fields` (advertise supported list)

## Frontend Test Matrix
- Dry run scalar only
- Dry run reference only (with some missing names)
- Include unsupported field (verify UNSUPPORTED in response)
- Enqueue during existing job (expect 409)
- Poll to COMPLETED and parse stats
- Simulate FAILED (backend error) handling

## Example cURL
Dry run:
```
curl -X POST \
  -H "Content-Type: application/json" \
  https://<host>/api/elections/5678/merge/dry-run \
  -d '{"sourceElectionId":1234,"fields":["RELIGION","CASTE","LANGUAGE"]}'
```
Enqueue merge:
```
curl -X POST \
  -H "Content-Type: application/json" \
  https://<host>/api/elections/5678/merge \
  -d '{"sourceElectionId":1234,"fields":["RELIGION","CASTE","LANGUAGE"]}'
```
Check status:
```
curl https://<host>/api/elections/5678/merge/jobs/4f5c0d7d-9b5b-4a5a-9d9d-7b1e0ac9ab12
```

## Future Enhancements (Suggested)
- Security: Replace placeholders with actual authenticated account/user context.
- Pagination / filtering on job listing (if listing endpoint added later).
- Real-time progress via WebSocket or SSE.
- Implement family & friends mapping merge logic.
- More precise `votersAffected` calculation (distinct voter count changed, not max heuristic).
- Dedicated success/error codes for merge domain.
- Cancel job endpoint (`PATCH` / `DELETE`).

---
Generated: 2025-09-03
Updated: 2025-09-03 (expanded with schemas & flow)
