# Default Party API Documentation

## Overview
This API allows you to set and retrieve a default party for an election. The default party is stored at the election level, and only one default party can be set per election.

## Base URL
```
/elections
```

## Endpoints

### 1. Set Default Party

**Endpoint:** `POST /elections/{electionId}/default-party`

**Description:** Sets the default party for a specific election. If a default party already exists, it will be updated with the new selection.

**Path Parameters:**
- `electionId` (Long, required): The ID of the election

**Request Body:**
```json
{
  "partyId": 123
}
```

**Request Fields:**
- `partyId` (Long, required): The ID of the party to set as default

**Success Response (200 OK):**
```json
{
  "code": 200,
  "message": "Party updated successfully",
  "data": "Default party set successfully: Party Name"
}
```

**Error Responses:**

- **401 Unauthorized:** Account ID not found
```json
{
  "code": 401,
  "message": "Account ID not created"
}
```

- **404 Not Found:** Election not found
```json
{
  "code": 404,
  "message": "Election not found"
}
```

- **404 Not Found:** Party not found or doesn't belong to the election
```json
{
  "code": 404,
  "message": "Party not found"
}
```

- **500 Internal Server Error:** Server error during operation
```json
{
  "code": 500,
  "message": "Internal server error"
}
```

**Example cURL:**
```bash
curl -X POST "http://localhost:8080/elections/1/default-party" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"partyId": 123}'
```

---

### 2. Get Default Party

**Endpoint:** `GET /elections/{electionId}/default-party`

**Description:** Retrieves the default party information for a specific election.

**Path Parameters:**
- `electionId` (Long, required): The ID of the election

**Success Response (200 OK):**

When default party is set:
```json
{
  "code": 200,
  "message": "Party fetched successfully",
  "data": {
    "electionId": 1,
    "defaultPartyId": 123,
    "partyName": "Party Name",
    "partyShortName": "PN",
    "partyImage": "https://example.com/party-image.png",
    "partyColor": "#FF5733",
    "message": "Default party found"
  }
}
```

When no default party is set:
```json
{
  "code": 200,
  "message": "Party fetched successfully",
  "data": {
    "electionId": 1,
    "defaultPartyId": null,
    "partyName": null,
    "partyShortName": null,
    "partyImage": null,
    "partyColor": null,
    "message": "No default party set for this election"
  }
}
```

**Response Fields:**
- `electionId` (Long): The ID of the election
- `defaultPartyId` (Long, nullable): The ID of the default party
- `partyName` (String, nullable): The name of the default party
- `partyShortName` (String, nullable): The short name of the default party
- `partyImage` (String, nullable): URL to the party image
- `partyColor` (String, nullable): The party color (hex code)
- `message` (String): Status message

**Error Responses:**

- **401 Unauthorized:** Account ID not found
```json
{
  "code": 401,
  "message": "Account ID not created"
}
```

- **404 Not Found:** Election not found
```json
{
  "code": 404,
  "message": "Election not found"
}
```

- **500 Internal Server Error:** Server error during operation
```json
{
  "code": 500,
  "message": "Internal server error"
}
```

**Example cURL:**
```bash
curl -X GET "http://localhost:8080/elections/1/default-party" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Validation Rules

1. **Party Ownership:** The party must belong to the same election and account
2. **Election Ownership:** The election must belong to the current account
3. **Non-Deleted Elections:** The election must not be soft-deleted (isDeleted = false)
4. **Party Existence:** The party must exist in the database

---

## Database Schema

### Election Table Update
A new column `default_party_id` has been added to the `election` table:

```sql
ALTER TABLE election 
ADD COLUMN IF NOT EXISTS default_party_id BIGINT;
```

---

## Data Synchronization

The default party ID is automatically synchronized to MongoDB when:
- A default party is set
- A default party is updated

---

## Use Cases

1. **Setting a default party:** An admin sets a default party for display in election-related interfaces
2. **Updating a default party:** The admin changes the default party selection
3. **Retrieving default party:** The frontend retrieves the default party to display as the primary option
4. **Checking if default party exists:** The system checks if a default party is configured for an election

---

## Notes

- Only one default party can be set per election
- Setting a new default party will overwrite the previous selection
- The API uses transactional operations to ensure data consistency
- Both PostgreSQL and MongoDB are updated in the same transaction
- If MongoDB sync fails, the entire transaction is rolled back
