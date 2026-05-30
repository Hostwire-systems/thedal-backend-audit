# Poll Day Export Consolidation

## Overview
Successfully updated the `/api/exports/{electionId}` endpoint to include Poll Day exports alongside Voter and Survey exports, providing a unified export job listing for the UI.

## Changes Made

### 1. ExportJobDTO Enhancement
**File**: `thedal-app/src/main/java/com/thedal/thedal_app/voter/ExportJobDTO.java`

Added new fields to support Poll Day export metadata:
- `chartType`: Chart type for poll day exports ("voterCount" or "familyCount")
- `format`: Export format for poll day ("excel" or "pdf")
- `rowCount`: Number of rows exported in poll day export

Added new constructor specifically for Poll Day exports to handle these additional fields.

### 2. ExportController Update
**File**: `thedal-app/src/main/java/com/thedal/thedal_app/voter/ExportController.java`

- Added optional `type` query parameter to filter by export type
- Updated API description to mention all three export types
- Pass type parameter to service methods

**Query Parameters**:
- `type` (optional): Filter by export type - "VOTER", "SURVEY", or "POLL_DAY"
- `status` (optional): Filter by status
- `startDate` (optional): Filter by start date (default: 30 days ago)
- `endDate` (optional): Filter by end date (default: now)
- `page`, `size` (optional): Pagination parameters

### 3. ExportService Enhancement
**File**: `thedal-app/src/main/java/com/thedal/thedal_app/voter/ExportService.java`

**Added Dependencies**:
- `PollDayExportJob` entity
- `PollDayExportJobRepository`

**Updated Methods**:
1. `getAllExportJobs()`: Now includes Poll Day exports with type filtering
2. `getAllExportJobsPaginated()`: Paginated version with Poll Day exports

**Added Helper Method**:
- `generatePollDayMessage()`: Generates status messages for poll day exports with row count and file expiration warnings

**Logic Flow**:
1. Check type parameter
2. Conditionally fetch Voter exports (if type is null or "VOTER")
3. Conditionally fetch Survey exports (if type is null or "SURVEY")
4. Conditionally fetch Poll Day exports (if type is null or "POLL_DAY")
5. Apply filters (status, date range) to all fetched jobs
6. Combine all jobs and sort by timeStarted (descending)
7. Apply pagination if requested
8. Return unified response with total count

## API Usage Examples

### Get All Export Types
```bash
GET /api/exports/58
```

### Get Only Poll Day Exports
```bash
GET /api/exports/58?type=POLL_DAY
```

### Get Only Voter Exports
```bash
GET /api/exports/58?type=VOTER
```

### Get Completed Exports (All Types)
```bash
GET /api/exports/58?status=COMPLETED
```

### Get Recent Poll Day Exports with Pagination
```bash
GET /api/exports/58?type=POLL_DAY&page=0&size=10
```

### Filter by Date Range
```bash
GET /api/exports/58?startDate=2025-11-01T00:00:00&endDate=2025-11-26T23:59:59
```

## Response Structure

```json
{
  "success": true,
  "message": "Export jobs fetched successfully",
  "data": {
    "exports": [
      {
        "jobId": 123,
        "electionId": 58,
        "type": "POLL_DAY",
        "formId": null,
        "formName": null,
        "chartType": "voterCount",
        "format": "excel",
        "rowCount": 1523,
        "status": "COMPLETED",
        "timeStarted": "2025-11-26T10:00:00",
        "timeCompleted": "2025-11-26T10:05:00",
        "downloadUrl": "https://s3.amazonaws.com/...",
        "message": "Export completed successfully. 1523 records exported."
      },
      {
        "jobId": 122,
        "electionId": 58,
        "type": "VOTER",
        "formId": null,
        "formName": null,
        "chartType": null,
        "format": null,
        "rowCount": null,
        "status": "COMPLETED",
        "timeStarted": "2025-11-25T15:30:00",
        "timeCompleted": "2025-11-25T15:35:00",
        "downloadUrl": "https://s3.amazonaws.com/...",
        "message": "Export completed successfully."
      },
      {
        "jobId": 121,
        "electionId": 58,
        "type": "SURVEY",
        "formId": 5,
        "formName": "Voter Feedback Survey",
        "chartType": null,
        "format": null,
        "rowCount": null,
        "status": "COMPLETED",
        "timeStarted": "2025-11-24T12:00:00",
        "timeCompleted": "2025-11-24T12:03:00",
        "downloadUrl": "https://s3.amazonaws.com/...",
        "message": "Export completed successfully."
      }
    ],
    "totalCount": 45
  }
}
```

## Field Mapping by Export Type

| Field | VOTER | SURVEY | POLL_DAY |
|-------|-------|--------|----------|
| jobId | ✅ | ✅ | ✅ |
| electionId | ✅ | ✅ | ✅ |
| type | "VOTER" | "SURVEY" | "POLL_DAY" |
| formId | null | ✅ | null |
| formName | null | ✅ | null |
| chartType | null | null | ✅ ("voterCount"/"familyCount") |
| format | null | null | ✅ ("excel"/"pdf") |
| rowCount | null | null | ✅ |
| status | ✅ | ✅ | ✅ |
| timeStarted | ✅ | ✅ | ✅ |
| timeCompleted | ✅ | ✅ | ✅ |
| downloadUrl | ✅ | ✅ | ✅ |
| message | ✅ | ✅ | ✅ |

## Status Messages for Poll Day Exports

- **PENDING**: "Export is queued for processing."
- **RUNNING**: "Export is in progress. Please check back later."
- **COMPLETED**: "Export completed successfully. {rowCount} records exported." (+ expiration warning if older than 23 hours)
- **FAILED**: "Export failed: {errorMessage}" or "Export failed: Please try again."

## UI Integration Notes

1. **Type Segregation**: Use the `type` field to display different icons or badges for each export type
2. **Conditional Fields**: Check `type` before accessing type-specific fields:
   - `formId`, `formName` for SURVEY exports
   - `chartType`, `format`, `rowCount` for POLL_DAY exports
3. **Filtering**: Use the `type` query parameter to show only specific export types
4. **Sorting**: All exports are sorted by `timeStarted` in descending order (most recent first)
5. **Pagination**: Use `page` and `size` parameters for large result sets

## Build Status
✅ Successfully compiled with Maven
- Build time: 26.348 seconds
- No errors, only standard warnings

## Testing Checklist
- [ ] Test fetching all export types without filters
- [ ] Test type filter for each export type (VOTER, SURVEY, POLL_DAY)
- [ ] Test status filter (PENDING, RUNNING, COMPLETED, FAILED)
- [ ] Test date range filtering
- [ ] Test pagination with mixed export types
- [ ] Verify proper sorting by timeStarted
- [ ] Check field values for each export type
- [ ] Verify download URLs are accessible
- [ ] Test with different election IDs
