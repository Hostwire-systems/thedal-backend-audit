# Bulk Field Management API Documentation

## Overview
This document describes the 8 new bulk field management APIs that allow enabling/disabling and requiring/making optional all static and dynamic fields for an election in a single operation.

**Feature Date**: January 2025  
**Backend Implementation**: Spring Boot REST APIs

---

## Static Field Bulk APIs

Base Path: `/api/elections/{electionId}/static-fields`

### 1. Enable All Static Fields

**Endpoint**: `PUT /api/elections/{electionId}/static-fields/enable-all`

**Description**: Sets the status to `true` (enabled) for all static voter fields in the specified election. This operation updates all default static field definitions (100+ fields across categories like basic info, contact, address, family, etc.).

**Request**:
```http
PUT /api/elections/{electionId}/static-fields/enable-all
Authorization: Bearer {token}
```

**Response**:
```json
{
  "success": true,
  "message": "SUCCESS",
  "data": "Enabled 120 static fields"
}
```

**Use Case**: Quickly enable all voter fields at the start of an election or after previously disabling many fields.

---

### 2. Disable All Static Fields

**Endpoint**: `PUT /api/elections/{electionId}/static-fields/disable-all`

**Description**: Sets the status to `false` (disabled) for all static voter fields in the specified election.

**Request**:
```http
PUT /api/elections/{electionId}/static-fields/disable-all
Authorization: Bearer {token}
```

**Response**:
```json
{
  "success": true,
  "message": "SUCCESS",
  "data": "Disabled 120 static fields"
}
```

**Use Case**: Hide all fields and then selectively enable only the ones needed for a specific election scenario.

---

### 3. Make All Static Fields Required

**Endpoint**: `PUT /api/elections/{electionId}/static-fields/require-all`

**Description**: Sets the mandatory flag to `true` (required) for all static voter fields in the specified election.

**Request**:
```http
PUT /api/elections/{electionId}/static-fields/require-all
Authorization: Bearer {token}
```

**Response**:
```json
{
  "success": true,
  "message": "SUCCESS",
  "data": "Made 120 static fields required"
}
```

**Use Case**: Enforce data completeness by making all voter information mandatory during data entry.

---

### 4. Make All Static Fields Optional

**Endpoint**: `PUT /api/elections/{electionId}/static-fields/optional-all`

**Description**: Sets the mandatory flag to `false` (optional) for all static voter fields in the specified election.

**Request**:
```http
PUT /api/elections/{electionId}/static-fields/optional-all
Authorization: Bearer {token}
```

**Response**:
```json
{
  "success": true,
  "message": "SUCCESS",
  "data": "Made 120 static fields optional"
}
```

**Use Case**: Relax data entry requirements when partial voter information is acceptable.

---

## Dynamic Field Bulk APIs

Base Path: `/api/dynamic-fields/election/{electionId}`

### 5. Enable All Dynamic Fields

**Endpoint**: `PUT /api/dynamic-fields/election/{electionId}/enable-all`

**Description**: Sets the status to `true` (enabled) for all custom dynamic fields in the specified election (maximum 5 fields per election).

**Request**:
```http
PUT /api/dynamic-fields/election/{electionId}/enable-all
Authorization: Bearer {token}
```

**Response**:
```json
{
  "success": true,
  "message": "SUCCESS",
  "data": "Enabled 5 dynamic fields"
}
```

**Use Case**: Quickly enable all custom fields after configuration or testing.

---

### 6. Disable All Dynamic Fields

**Endpoint**: `PUT /api/dynamic-fields/election/{electionId}/disable-all`

**Description**: Sets the status to `false` (disabled) for all custom dynamic fields in the specified election.

**Request**:
```http
PUT /api/dynamic-fields/election/{electionId}/disable-all
Authorization: Bearer {token}
```

**Response**:
```json
{
  "success": true,
  "message": "SUCCESS",
  "data": "Disabled 5 dynamic fields"
}
```

**Use Case**: Temporarily hide all custom fields without deleting their definitions.

---

### 7. Make All Dynamic Fields Required

**Endpoint**: `PUT /api/dynamic-fields/election/{electionId}/require-all`

**Description**: Sets the mandatory flag to `true` (required) for all custom dynamic fields in the specified election.

**Request**:
```http
PUT /api/dynamic-fields/election/{electionId}/require-all
Authorization: Bearer {token}
```

**Response**:
```json
{
  "success": true,
  "message": "SUCCESS",
  "data": "Made 5 dynamic fields required"
}
```

**Use Case**: Enforce data completeness for all custom fields during critical data collection phases.

---

### 8. Make All Dynamic Fields Optional

**Endpoint**: `PUT /api/dynamic-fields/election/{electionId}/optional-all`

**Description**: Sets the mandatory flag to `false` (optional) for all custom dynamic fields in the specified election.

**Request**:
```http
PUT /api/dynamic-fields/election/{electionId}/optional-all
Authorization: Bearer {token}
```

**Response**:
```json
{
  "success": true,
  "message": "SUCCESS",
  "data": "Made 5 dynamic fields optional"
}
```

**Use Case**: Relax validation requirements for custom fields to allow partial data entry.

---

## Implementation Details

### Service Layer (StaticFieldStatusService.java)

```java
@Transactional
public int enableAllFields(Long accountId, Long electionId) {
    // Iterates through all default static field definitions
    // Calls updateFieldStatus(accountId, electionId, fieldName, true)
    // Returns count of updated fields
}
```

**Key Features**:
- Transactional - ensures atomicity of bulk updates
- Only updates fields that need changing (e.g., only enables currently disabled fields)
- Returns integer count for UI feedback
- Comprehensive logging for debugging

### Service Layer (DynamicFieldService.java)

```java
@Transactional
public int enableAllFields(Long accountId, Long electionId) {
    // Fetches all dynamic fields for the election
    // Iterates and updates status property
    // Returns count of updated fields
}
```

**Key Features**:
- Validates election ownership before updates
- Works with existing dynamic field entities (max 5 per election)
- Idempotent - safe to call multiple times

### Controller Layer

Both controllers follow the same pattern:
- Extract accountId from request context
- Validate authorization
- Call service method
- Return ThedalResponse with success message and count

### Error Handling

All endpoints handle:
- **401 Unauthorized**: Missing or invalid account ID
- **403 Forbidden**: Election doesn't belong to account
- **500 Internal Server Error**: Database or unexpected errors

---

## Frontend Integration Guide

### UI Components

**Static Fields Page**:
```html
<button @click="enableAllStaticFields">Enable All</button>
<button @click="disableAllStaticFields">Disable All</button>
<button @click="requireAllStaticFields">Make All Required</button>
<button @click="optionalAllStaticFields">Make All Optional</button>
```

**Dynamic Fields Page**:
```html
<button @click="enableAllDynamicFields">Enable All</button>
<button @click="disableAllDynamicFields">Disable All</button>
<button @click="requireAllDynamicFields">Make All Required</button>
<button @click="optionalAllDynamicFields">Make All Optional</button>
```

### Example JavaScript/TypeScript Implementation

```javascript
async function enableAllStaticFields(electionId) {
  try {
    const response = await axios.put(
      `/api/elections/${electionId}/static-fields/enable-all`,
      {},
      { headers: { Authorization: `Bearer ${token}` } }
    );
    
    // Show success toast with count
    toast.success(response.data.data); // "Enabled 120 static fields"
    
    // Refresh field list to show updated status
    await fetchStaticFields(electionId);
  } catch (error) {
    toast.error('Failed to enable all static fields');
    console.error(error);
  }
}

async function requireAllDynamicFields(electionId) {
  try {
    const response = await axios.put(
      `/api/dynamic-fields/election/${electionId}/require-all`,
      {},
      { headers: { Authorization: `Bearer ${token}` } }
    );
    
    toast.success(response.data.data); // "Made 5 dynamic fields required"
    await fetchDynamicFields(electionId);
  } catch (error) {
    toast.error('Failed to make all dynamic fields required');
    console.error(error);
  }
}
```

### Confirmation Dialogs

Consider adding confirmation for destructive operations:

```javascript
async function disableAllStaticFields(electionId) {
  const confirmed = await confirmDialog({
    title: 'Disable All Static Fields?',
    message: 'This will hide all voter fields from the UI. You can re-enable them later.',
    confirmText: 'Disable All',
    cancelText: 'Cancel'
  });
  
  if (!confirmed) return;
  
  // Proceed with API call...
}
```

---

## Testing Scenarios

### Manual Testing Checklist

**Static Fields**:
- [ ] Enable all fields → Verify all ~120 fields are enabled in database
- [ ] Disable all fields → Verify all fields are disabled
- [ ] Require all fields → Verify all mandatory flags are true
- [ ] Optional all fields → Verify all mandatory flags are false
- [ ] Check returned counts match actual database updates

**Dynamic Fields**:
- [ ] Create 3 dynamic fields for an election
- [ ] Enable all → Verify all 3 are enabled
- [ ] Disable all → Verify all 3 are disabled
- [ ] Require all → Verify all 3 are required
- [ ] Optional all → Verify all 3 are optional

**Edge Cases**:
- [ ] Call enable-all when all fields already enabled (should return count 0)
- [ ] Call disable-all when all fields already disabled (should return count 0)
- [ ] Test with election that has 0 dynamic fields (should return count 0)
- [ ] Test authorization with wrong accountId (should return 403)

### Postman/Swagger Testing

**Test Setup**:
1. Create an election
2. Initialize static fields (if not already done)
3. Create 2-3 dynamic fields

**Test Sequence**:
```
1. PUT /api/elections/{electionId}/static-fields/enable-all
   Expected: 200 OK, "Enabled X static fields"

2. PUT /api/elections/{electionId}/static-fields/disable-all
   Expected: 200 OK, "Disabled X static fields"

3. PUT /api/dynamic-fields/election/{electionId}/require-all
   Expected: 200 OK, "Made 3 dynamic fields required"

4. GET /api/dynamic-fields/election/{electionId}
   Expected: All fields have required=true

5. PUT /api/dynamic-fields/election/{electionId}/optional-all
   Expected: 200 OK, "Made 3 dynamic fields optional"

6. GET /api/dynamic-fields/election/{electionId}
   Expected: All fields have required=false
```

---

## Database Impact

### Static Fields
- **Table**: `static_field_status`
- **Columns Updated**: `status`, `mandatory`
- **Rows Affected**: ~120 per election
- **Transaction**: All updates in single transaction

### Dynamic Fields
- **Table**: `dynamic_fields`
- **Columns Updated**: `status`, `required`
- **Rows Affected**: 0-5 per election
- **Transaction**: All updates in single transaction

**Performance**: Bulk operations complete in <1 second for typical field counts.

---

## Changelog

### January 2025
- **v1.0.0**: Initial release
  - Added 4 static field bulk APIs
  - Added 4 dynamic field bulk APIs
  - Implemented transactional service layer
  - Added comprehensive logging
  - Created API documentation

---

## Support

For issues or questions, contact the backend development team or create a ticket in the project management system.

**Related Documentation**:
- [Static Field Management API](./VOTER_FIELDS_API.md)
- [Dynamic Field API Documentation](./DYNAMIC_FIELDS_API.md)
- [Election Management API](./ELECTION_API.md)
