# Poll Day Chart Configuration API - Implementation Summary

## Overview
This document summarizes the complete implementation of the Poll Day Chart Configuration API that allows users to save and retrieve their custom chart configurations for the Poll Day Dashboard.

## Implementation Date
October 15, 2025

## Files Created

### 1. Database Migration
**File:** `V912__poll_day_chart_configs.sql`
- Creates `poll_day_chart_configs` table with JSONB support
- Adds unique constraint on (account_id, election_id)
- Creates index for faster lookups
- Includes table and column comments

### 2. Entity
**File:** `PollDayChartConfig.java`
- JPA entity with JSONB support for storing chart configurations
- Auto-managed created_at and updated_at timestamps via @PrePersist and @PreUpdate
- Fields: id, accountId, electionId, charts (JSONB), createdAt, updatedAt

### 3. DTOs
**Files Created:**
- `ChartConfig.java` - Individual chart configuration (chartId, selectedParts[])
- `PollDayChartConfigRequest.java` - Request DTO with validation (1-4 charts required)
- `PollDayChartConfigResponse.java` - Response DTO with all fields
- `ApiResponse.java` - Generic API response wrapper with success/error helpers

### 4. Repository
**File:** `PollDayChartConfigRepository.java`
- Extends JpaRepository
- Custom methods:
  - `findByAccountIdAndElectionId()`
  - `deleteByAccountIdAndElectionId()`

### 5. Service
**File:** `PollDayChartConfigService.java`
- Business logic and validation
- Methods:
  - `saveChartConfig()` - Creates or updates configuration
  - `getChartConfig()` - Retrieves configuration
  - `deleteChartConfig()` - Deletes configuration
- Validation rules:
  - 1-4 charts required
  - Chart IDs must not be empty
  - selectedParts must be non-null array (empty = all parts)
  - Part numbers must be non-negative

### 6. Controller
**File:** `PollDayChartConfigController.java`
- REST endpoints:
  - `POST /api/reporting/poll-day/chart-config` - Save configuration
  - `GET /api/reporting/poll-day/chart-config` - Get configuration
  - `DELETE /api/reporting/poll-day/chart-config` - Delete configuration
- Proper error handling and HTTP status codes
- Request validation using @Valid

## API Endpoints

### Save Chart Configuration
```
POST /api/reporting/poll-day/chart-config
Content-Type: application/json
Authorization: Bearer <token>

Request Body:
{
  "accountId": 54,
  "electionId": 58,
  "charts": [
    {
      "chartId": "1",
      "selectedParts": [1, 2, 3],
      "customTitle": "Age Group Distribution",
      "chartColor": "#4F46E5",
      "viewType": "bar",
      "order": 0,
      "width": 800,
      "height": 450,
      "x": 0,
      "y": 0
    },
    {
      "chartId": "2",
      "selectedParts": [],
      "customTitle": "Polling Trends",
      "chartColor": "#10B981",
      "viewType": "line",
      "order": 1,
      "width": 600,
      "height": 400,
      "x": 850,
      "y": 0
    }
  ]
}

Response (200 OK):
{
  "success": true,
  "message": "Chart configuration saved successfully",
  "data": {
    "id": 123,
    "accountId": 54,
    "electionId": 58,
    "charts": [...],
    "createdAt": "2025-10-15T10:30:00Z",
    "updatedAt": "2025-10-15T10:30:00Z"
  }
}
```

### Get Chart Configuration
```
GET /api/reporting/poll-day/chart-config?accountId=54&electionId=58
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "Chart configuration retrieved successfully",
  "data": {
    "id": 123,
    "accountId": 54,
    "electionId": 58,
    "charts": [...],
    "createdAt": "2025-10-15T10:30:00Z",
    "updatedAt": "2025-10-15T12:45:00Z"
  }
}

Response (404 Not Found):
{
  "success": false,
  "message": "No chart configuration found for this election",
  "data": null
}
```

### Delete Chart Configuration
```
DELETE /api/reporting/poll-day/chart-config?accountId=54&electionId=58
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "Chart configuration deleted successfully",
  "data": null
}
```

## Business Rules Implemented

1. **Chart Limit**: Maximum 4 charts per election (validated)
2. **Part Selection**: Empty `selectedParts` array means "show all parts"
3. **Unique Config**: One configuration per account-election combination (database constraint)
4. **Update Behavior**: POST request overwrites existing configuration (upsert logic)
5. **Default Behavior**: API returns 404 if no configuration exists (frontend shows default)
6. **Validation**:
   - At least 1 chart required
   - Maximum 4 charts allowed
   - `selectedParts` must be non-null (can be empty array)
   - Part numbers must be non-negative integers
   - `chartId` must not be empty
7. **New Field Validations** (Added October 27, 2025):
   - `viewType`: Must be "bar", "line", or "table" if provided (case-insensitive)
   - `width`: Minimum 100 pixels if provided
   - `height`: Minimum 100 pixels if provided
   - `x`: Cannot be negative if provided
   - `y`: Cannot be negative if provided
   - `order`: No validation (any integer value)
   - All new fields are optional

## Database Schema

```sql
CREATE TABLE poll_day_chart_configs (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    election_id BIGINT NOT NULL,
    charts JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_account_election_chart_config UNIQUE(account_id, election_id)
);

CREATE INDEX idx_chart_config_account_election 
ON poll_day_chart_configs(account_id, election_id);
```

### Charts JSONB Structure
```json
[
  {
    "chartId": "string",
    "selectedParts": [1, 2, 3, ...],
    "customTitle": "string (optional)",
    "chartColor": "string (optional)",
    "viewType": "bar | line | table (optional)",
    "order": "number (optional) - for drag-and-drop ordering",
    "width": "number (optional) - chart width in pixels, default: 600",
    "height": "number (optional) - chart height in pixels, default: 450",
    "x": "number (optional) - x position for free-form positioning, default: 0",
    "y": "number (optional) - y position for free-form positioning, default: 0"
  }
]
```

## Testing

### Test Scenario 1: Save New Configuration
```bash
curl -X POST "http://localhost:8080/api/reporting/poll-day/chart-config" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "accountId": 54,
    "electionId": 58,
    "charts": [
      {"chartId": "1", "selectedParts": [1, 2, 3]},
      {"chartId": "2", "selectedParts": [4, 5]}
    ]
  }'
```

### Test Scenario 2: Get Saved Configuration
```bash
curl -X GET "http://localhost:8080/api/reporting/poll-day/chart-config?accountId=54&electionId=58" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Test Scenario 3: Update Existing Configuration
```bash
curl -X POST "http://localhost:8080/api/reporting/poll-day/chart-config" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "accountId": 54,
    "electionId": 58,
    "charts": [
      {"chartId": "1", "selectedParts": [1, 2, 3, 4, 5]},
      {"chartId": "2", "selectedParts": []},
      {"chartId": "3", "selectedParts": [10, 11, 12]}
    ]
  }'
```

### Test Scenario 4: Delete Configuration
```bash
curl -X DELETE "http://localhost:8080/api/reporting/poll-day/chart-config?accountId=54&electionId=58" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Test Scenario 5: Validation Errors
```bash
# Too many charts (should fail)
curl -X POST "http://localhost:8080/api/reporting/poll-day/chart-config" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "accountId": 54,
    "electionId": 58,
    "charts": [
      {"chartId": "1", "selectedParts": [1]},
      {"chartId": "2", "selectedParts": [2]},
      {"chartId": "3", "selectedParts": [3]},
      {"chartId": "4", "selectedParts": [4]},
      {"chartId": "5", "selectedParts": [5]}
    ]
  }'

# Expected Response (400 Bad Request):
{
  "success": false,
  "message": "Maximum 4 charts allowed",
  "data": null
}
```

## Frontend Integration Guide

### TypeScript Interfaces
```typescript
interface ChartConfig {
  chartId: string;
  id?: string;
  selectedParts: number[];
  customTitle?: string;
  chartColor?: string;
  viewType?: "bar" | "line" | "table";
  order?: number;          // For drag-and-drop ordering
  width?: number;          // Chart width in pixels (default: 600)
  height?: number;         // Chart height in pixels (default: 450)
  x?: number;              // X position for free-form positioning (default: 0)
  y?: number;              // Y position for free-form positioning (default: 0)
}

interface PollDayChartConfigRequest {
  accountId: number;
  electionId: number;
  charts: ChartConfig[];
}

interface PollDayChartConfigResponse {
  id: number;
  accountId: number;
  electionId: number;
  charts: ChartConfig[];
  createdAt: string;
  updatedAt: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}
```

### API Service Functions
```typescript
import axios from 'axios';

const BASE_URL = 'http://localhost:8080';

export const savePollDayChartConfig = (
  accountId: number,
  electionId: number,
  charts: ChartConfig[]
) =>
  axios.post<ApiResponse<PollDayChartConfigResponse>>(
    `${BASE_URL}/api/reporting/poll-day/chart-config`,
    { accountId, electionId, charts },
    { headers: authHeaders() }
  );

export const getPollDayChartConfig = (
  accountId: number,
  electionId: number
) =>
  axios.get<ApiResponse<PollDayChartConfigResponse>>(
    `${BASE_URL}/api/reporting/poll-day/chart-config`,
    {
      params: { accountId, electionId },
      headers: authHeaders()
    }
  );

export const deletePollDayChartConfig = (
  accountId: number,
  electionId: number
) =>
  axios.delete<ApiResponse<void>>(
    `${BASE_URL}/api/reporting/poll-day/chart-config`,
    {
      params: { accountId, electionId },
      headers: authHeaders()
    }
  );
```

## Next Steps

1. **Build and Deploy**: Compile the application with Maven
2. **Run Migration**: The V912 migration will run automatically on application startup
3. **Test Endpoints**: Use the curl commands above to test all scenarios
4. **Frontend Integration**: Implement the TypeScript interfaces and API calls
5. **Monitor Logs**: Check application logs for any issues during initial usage

## Additional Notes

- The API uses JSONB for flexible chart storage
- Timestamps are stored with timezone information (TIMESTAMPTZ)
- The unique constraint ensures one configuration per account-election pair
- Empty `selectedParts` arrays are explicitly allowed (meaning "all parts")
- All validation happens on the backend for security
- Proper error messages are returned for all failure scenarios

## Related Files

This implementation also fixed the part-wise polling API to return all-time data when no polling date is specified:
- Modified `PollDayPartWisePollingController.java` to pass null when no date provided
- Modified `PollDayPartWisePollingService.java` to handle null date (returns cumulative data)

## Status

✅ **Implementation Complete** - All components created and ready for testing
