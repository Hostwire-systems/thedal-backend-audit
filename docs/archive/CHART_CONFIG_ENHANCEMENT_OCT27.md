# Chart Configuration Enhancement - October 27, 2025

## Overview
Added support for enhanced chart configuration features including drag-and-drop ordering, chart resizing, and free-form positioning in the Poll Day Dashboard.

## Changes Made

### 1. ChartConfig DTO Enhancement
**File:** `thedal-app/src/main/java/com/thedal/thedal_app/report/pollday/dto/ChartConfig.java`

#### New Fields Added:
- `viewType` (String, optional): Chart visualization type - "bar", "line", or "table"
- `order` (Integer, optional): Order index for drag-and-drop sorting
- `width` (Integer, optional): Chart width in pixels (default: 600)
- `height` (Integer, optional): Chart height in pixels (default: 450)
- `x` (Integer, optional): X position for free-form positioning (default: 0)
- `y` (Integer, optional): Y position for free-form positioning (default: 0)

### 2. Validation Logic Enhancement
**File:** `thedal-app/src/main/java/com/thedal/thedal_app/report/pollday/PollDayChartConfigService.java`

#### New Validation Rules:
- `viewType`: Must be "bar", "line", or "table" (case-insensitive) if provided
- `width`: Minimum 100 pixels if provided
- `height`: Minimum 100 pixels if provided
- `x`: Cannot be negative if provided
- `y`: Cannot be negative if provided
- `order`: No validation (any integer value accepted)

All new fields are optional and will not break existing configurations.

### 3. Documentation Update
**File:** `POLL_DAY_CHART_CONFIG_API.md`

Updated documentation to include:
- New field descriptions in JSONB structure
- Updated TypeScript interfaces
- Enhanced example with new fields
- New validation rules section

## API Example

### Request with New Fields:
```json
POST /api/reporting/poll-day/chart-config

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
```

### Response:
```json
{
  "success": true,
  "message": "Chart configuration saved successfully",
  "data": {
    "id": 123,
    "accountId": 54,
    "electionId": 58,
    "charts": [...],
    "createdAt": "2025-10-27T10:30:00Z",
    "updatedAt": "2025-10-27T10:30:00Z"
  }
}
```

## Backward Compatibility

✅ **Fully Backward Compatible**
- All new fields are optional
- Existing configurations without these fields will continue to work
- No database migration required (JSONB is schema-less)
- Frontend can provide defaults (600x450, position 0,0, viewType "bar")

## Frontend Integration

### TypeScript Interface (Updated):
```typescript
interface ChartConfig {
  id: string;
  selectedParts: number[];
  viewType?: "bar" | "line" | "table";
  customTitle?: string;
  chartColor?: string;
  order?: number;          // for drag-and-drop ordering
  width?: number;          // for chart resize (default: 600)
  height?: number;         // for chart resize (default: 450)
  x?: number;             // for free-form positioning (default: 0)
  y?: number;             // for free-form positioning (default: 0)
}
```

### Default Values (Frontend):
```typescript
const defaultChartConfig = {
  viewType: "bar",
  order: 0,
  width: 600,
  height: 450,
  x: 0,
  y: 0
};
```

## Use Cases Enabled

1. **Drag-and-Drop Ordering**: Use `order` field to maintain chart sequence
2. **Chart Resizing**: Users can resize charts using `width` and `height`
3. **Free-form Positioning**: Full-screen dashboard mode using `x` and `y` coordinates
4. **View Type Switching**: Toggle between bar, line, and table visualizations
5. **Custom Layouts**: Combine all features for personalized dashboard layouts

## Testing

### Validation Tests:

#### 1. Valid Request with All Fields:
```bash
curl -X POST "http://localhost:8080/api/reporting/poll-day/chart-config" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "accountId": 54,
    "electionId": 58,
    "charts": [
      {
        "chartId": "1",
        "selectedParts": [1, 2],
        "customTitle": "Test Chart",
        "chartColor": "#FF0000",
        "viewType": "bar",
        "order": 0,
        "width": 800,
        "height": 600,
        "x": 100,
        "y": 50
      }
    ]
  }'
```

#### 2. Invalid viewType (Should Fail):
```bash
curl -X POST "http://localhost:8080/api/reporting/poll-day/chart-config" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "accountId": 54,
    "electionId": 58,
    "charts": [
      {
        "chartId": "1",
        "selectedParts": [1],
        "viewType": "invalid"
      }
    ]
  }'

# Expected: 400 Bad Request
# Error: "Invalid viewType: invalid. Must be 'bar', 'line', or 'table'"
```

#### 3. Invalid Dimensions (Should Fail):
```bash
curl -X POST "http://localhost:8080/api/reporting/poll-day/chart-config" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "accountId": 54,
    "electionId": 58,
    "charts": [
      {
        "chartId": "1",
        "selectedParts": [1],
        "width": 50,
        "height": 50
      }
    ]
  }'

# Expected: 400 Bad Request
# Error: "Chart width must be at least 100 pixels"
```

#### 4. Backward Compatibility (Should Work):
```bash
curl -X POST "http://localhost:8080/api/reporting/poll-day/chart-config" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "accountId": 54,
    "electionId": 58,
    "charts": [
      {
        "chartId": "1",
        "selectedParts": [1, 2, 3]
      }
    ]
  }'

# Expected: 200 OK (works without new fields)
```

## Deployment Notes

1. **No Migration Required**: The changes are code-only
2. **No Breaking Changes**: Existing configurations remain valid
3. **Build Required**: Rebuild the application with Maven
4. **Testing**: Test all validation scenarios before deploying to production
5. **Frontend Update**: Update frontend to utilize new fields

## Build Commands

```bash
cd thedal-app
mvn clean compile
mvn spring-boot:run
```

## Files Modified

1. ✅ `ChartConfig.java` - Added 6 new optional fields
2. ✅ `PollDayChartConfigService.java` - Added validation for new fields
3. ✅ `POLL_DAY_CHART_CONFIG_API.md` - Updated documentation

## Status

✅ **Implementation Complete** - Ready for testing and deployment
- All code changes implemented
- Validation logic added
- Documentation updated
- No compilation errors
- Backward compatible

## Next Steps

1. Build the application: `mvn clean compile`
2. Run tests to verify no regressions
3. Test the enhanced API with various payloads
4. Update frontend to use the new fields
5. Deploy to staging for integration testing
