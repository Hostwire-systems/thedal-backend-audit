# Voter Static Fields API Documentation

## Overview
APIs for managing voter static field configurations (enable/disable fields per election).

**Base URL:** `http://localhost:8080`

---

## 1. Get All Field Statuses
**GET** `/api/elections/{electionId}/static-fields`

### Request
```bash
curl "http://localhost:8080/api/elections/58/static-fields" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Response
```json
{
  "status": "success",
  "data": [
    {
      "fieldName": "MOBILE_NUMBER",
      "fieldLabel": "Mobile Number",
      "fieldCategory": "contact",
      "status": true
    },
    {
      "fieldName": "RELIGION",
      "fieldLabel": "Religion", 
      "fieldCategory": "demographics",
      "status": true
    },
    {
      "fieldName": "CASTE",
      "fieldLabel": "Caste",
      "fieldCategory": "demographics", 
      "status": false
    },
    {
      "fieldName": "BENEFIT_SCHEMES",
      "fieldLabel": "Benefit Schemes",
      "fieldCategory": "schemes",
      "status": true
    }
  ]
}
```

---

## 2. Get Field Statuses by Category
**GET** `/api/elections/{electionId}/static-fields/by-category`

### Response
```json
{
  "status": "success",
  "data": {
    "contact": [
      {
        "fieldName": "MOBILE_NUMBER",
        "fieldLabel": "Mobile Number",
        "fieldCategory": "contact",
        "status": true
      },
      {
        "fieldName": "EMAIL_ID",
        "fieldLabel": "Email ID", 
        "fieldCategory": "contact",
        "status": true
      }
    ],
    "demographics": [
      {
        "fieldName": "RELIGION",
        "fieldLabel": "Religion",
        "fieldCategory": "demographics",
        "status": true
      },
      {
        "fieldName": "CASTE",
        "fieldLabel": "Caste", 
        "fieldCategory": "demographics",
        "status": false
      }
    ]
  }
}
```

---

## 3. Update Single Field Status
**PUT** `/api/elections/{electionId}/static-fields/field/{fieldName}/status?status=true`

### Request
```bash
curl "http://localhost:8080/api/elections/58/static-fields/field/CASTE/status?status=false" \
  -X PUT \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Response
```json
{
  "status": "success",
  "message": "Field status updated successfully"
}
```

---

## 4. Bulk Update Field Statuses
**PUT** `/api/elections/{electionId}/static-fields/bulk-update`

### Request Body
```json
{
  "fieldStatuses": [
    {
      "fieldName": "CASTE",
      "fieldLabel": "Caste",
      "fieldCategory": "demographics",
      "status": false
    },
    {
      "fieldName": "BENEFIT_SCHEMES", 
      "fieldLabel": "Benefit Schemes",
      "fieldCategory": "schemes",
      "status": true
    }
  ]
}
```

### cURL Example
```bash
curl "http://localhost:8080/api/elections/58/static-fields/bulk-update" \
  -X PUT \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  --data '{
    "fieldStatuses": [
      {
        "fieldName": "CASTE",
        "fieldLabel": "Caste", 
        "fieldCategory": "demographics",
        "status": false
      }
    ]
  }'
```

### Response
```json
{
  "status": "success", 
  "message": "Field statuses updated successfully"
}
```

---

## 5. Get Enabled Field Names Only
**GET** `/api/elections/{electionId}/static-fields/enabled`

### Response
```json
{
  "status": "success",
  "data": [
    "MOBILE_NUMBER",
    "EMAIL_ID", 
    "RELIGION",
    "BENEFIT_SCHEMES"
  ]
}
```

---

## 6. Check Single Field Status
**GET** `/api/elections/{electionId}/static-fields/field/{fieldName}/status`

### Request
```bash
curl "http://localhost:8080/api/elections/58/static-fields/field/CASTE/status" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Response
```json
{
  "status": "success",
  "data": false
}
```

---

## 7. Initialize Default Fields for Election
**POST** `/api/elections/{electionId}/static-fields/initialize`

### Request
```bash
curl "http://localhost:8080/api/elections/58/static-fields/initialize" \
  -X POST \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Response
```json
{
  "status": "success",
  "message": "Default static fields initialized successfully"
}
```

---

## 8. Available Static Fields

| Field Name | Field Label | Category | Description |
|-----------|-------------|----------|-------------|
| `MOBILE_NUMBER` | Mobile Number | contact | Primary contact number |
| `WHATSAPP_NUMBER` | WhatsApp Number | contact | WhatsApp contact |
| `EMAIL_ID` | Email ID | contact | Email address |
| `DATE_OF_BIRTH` | Date of Birth | personal | Birth date |
| `RELIGION` | Religion | demographics | Religious affiliation |
| `CASTE_CATEGORY` | Caste Category | demographics | SC/ST/OBC/General |
| `CASTE` | Caste | demographics | Specific caste |
| `SUB_CASTE` | Sub Caste | demographics | Sub-caste details |
| `PARTY` | Political Party | political | Party preference |
| `VOTER_CATEGORY` | Voter Category | political | Category classification |
| `LANGUAGE` | Languages | personal | Known languages |
| `BENEFIT_SCHEMES` | Benefit Schemes | schemes | Enrolled schemes |
| `VOTER_HISTORY` | Election History | political | Previous elections |
| `FEEDBACK` | Feedback Issues | feedback | Reported issues |
| `AADHAAR_NUMBER` | Aadhaar Number | identity | Aadhaar ID |
| `PAN_NUMBER` | PAN Number | identity | PAN card number |
| `REMARKS` | Remarks | notes | Additional notes |
| `FAMILY_MAPPING` | Family Mapping | relationships | Has family connections |
| `FRIENDS_MAPPING` | Friends Network | relationships | Has friend connections |

---

## 9. Your Original cURL Command (Fixed)

```bash
curl "http://localhost:8080/api/elections/58/static-fields/bulk-update" \
  -X PUT \
  -H "Accept: */*" \
  -H "Accept-Language: en-US,en;q=0.9,ar;q=0.8" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJhY2NvdW50SWQiOjU0LCJyb2xlSWQiOjEsIlBlcm1pc3Npb24iOjMxLCJ1c2VySWQiOjU0LCJwd2RWZXJzaW9uIjoxLCJpYXQiOjE3NTg5NzI0NzcsImV4cCI6MTc2MTU2NDQ3N30.H248e8OkkhF26IfaPqguuXX2ym7DUqYwTT8VJ6K00e4" \
  -H "Content-Type: application/json" \
  --data-raw '{
    "fieldStatuses": [
      {
        "fieldName": "CASTE",
        "fieldLabel": "Caste",
        "fieldCategory": "demographics", 
        "status": false
      }
    ]
  }'
```

---

## 10. Error Responses

### Unauthorized
```json
{
  "status": "error",
  "code": 401,
  "message": "Account id not found, unauthorized access."
}
```

### Field Not Found
```json
{
  "status": "error", 
  "code": 400,
  "message": "Invalid field name: INVALID_FIELD"
}
```

### Server Error
```json
{
  "status": "error",
  "code": 500, 
  "message": "An unexpected error occurred"
}
```

---

## 11. Frontend Integration Examples

### JavaScript/React
```javascript
// Get all field statuses
const getFieldStatuses = async (electionId) => {
  const response = await fetch(`/api/elections/${electionId}/static-fields`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return response.json();
};

// Update single field
const updateFieldStatus = async (electionId, fieldName, status) => {
  const response = await fetch(
    `/api/elections/${electionId}/static-fields/field/${fieldName}/status?status=${status}`,
    {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  return response.json();
};

// Bulk update
const bulkUpdateFields = async (electionId, fieldUpdates) => {
  const response = await fetch(
    `/api/elections/${electionId}/static-fields/bulk-update`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ fieldStatuses: fieldUpdates })
    }
  );
  return response.json();
};
```