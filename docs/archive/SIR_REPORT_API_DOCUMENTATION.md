# SIR Report API Documentation
**Version:** 1.0  
**Base URL:** `/api/voter/sir-report`  
**Authentication:** Required (Bearer Token)

---

## Overview
The SIR (Supplement, Inclusion, Rejection) Report API compares two voter Excel files (base and new) to identify:
- **Additions:** EPIC IDs in new file but not in base file
- **Deletions:** EPIC IDs in base file but not in new file
- **Shifts:** EPIC IDs in both files but with different part numbers

All processing is **asynchronous**.

---

## API Endpoints

### 1. Upload & Start Comparison
**Endpoint:** `POST /api/voter/sir-report/compare`  
**Description:** Upload two Excel files and start comparison process  
**Content-Type:** `multipart/form-data`

#### Request Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| baseFile | File | Yes | Base Excel file (.xlsx, .xls) |
| newFile | File | Yes | New Excel file (.xlsx, .xls) |
| electionId | Long | No | Election ID for filtering |

#### Request Example (Postman/Axios)
```javascript
// JavaScript/Axios
const formData = new FormData();
formData.append('baseFile', baseFile);
formData.append('newFile', newFile);
formData.append('electionId', '123');

axios.post('/api/voter/sir-report/compare', formData, {
  headers: {
    'Content-Type': 'multipart/form-data',
    'Authorization': 'Bearer YOUR_TOKEN'
  }
});
```

```bash
# cURL
curl -X POST "http://localhost:8080/api/voter/sir-report/compare" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "baseFile=@/path/to/base.xlsx" \
  -F "newFile=@/path/to/new.xlsx" \
  -F "electionId=123"
```

#### Response
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "message": "Comparison started successfully"
}
```

---

### 2. Check Job Status
**Endpoint:** `GET /api/voter/sir-report/{jobId}/status`  
**Description:** Check the progress of a comparison job

#### Request Example
```javascript
// JavaScript/Axios
axios.get(`/api/voter/sir-report/${jobId}/status`, {
  headers: { 'Authorization': 'Bearer YOUR_TOKEN' }
});
```

```bash
# cURL
curl -X GET "http://localhost:8080/api/voter/sir-report/550e8400-e29b-41d4-a716-446655440000/status" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Response
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "progress": 65,
  "message": "Comparing files..."
}
```

**Status Values:**
- `PROCESSING` - Job is in progress
- `COMPLETED` - Job finished successfully
- `FAILED` - Job failed with error

---

### 3. Get Summary
**Endpoint:** `GET /api/voter/sir-report/{jobId}/summary`  
**Description:** Get summary counts after comparison completes

#### Request Example
```javascript
// JavaScript/Axios
axios.get(`/api/voter/sir-report/${jobId}/summary`, {
  headers: { 'Authorization': 'Bearer YOUR_TOKEN' }
});
```

```bash
# cURL
curl -X GET "http://localhost:8080/api/voter/sir-report/550e8400-e29b-41d4-a716-446655440000/summary" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Response (When Completed)
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "summary": {
    "totalBaseRecords": 10000,
    "totalNewRecords": 10150,
    "additions": 200,
    "deletions": 50,
    "shifts": 30
  },
  "processedAt": "2025-11-26T10:30:00",
  "errorMessage": null
}
```

#### Response (When Processing)
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "summary": null,
  "processedAt": null,
  "errorMessage": null
}
```

#### Response (When Failed)
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "FAILED",
  "summary": null,
  "processedAt": "2025-11-26T10:30:00",
  "errorMessage": "Invalid file format"
}
```

---

### 4. Get Detailed Records
**Endpoint:** `GET /api/voter/sir-report/{jobId}/details`  
**Description:** Get paginated detailed records by type

#### Query Parameters
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| type | String | Yes | - | ADDITIONS, DELETIONS, or SHIFTS |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 50 | Page size |

#### Request Examples
```javascript
// Get Additions
axios.get(`/api/voter/sir-report/${jobId}/details`, {
  params: { type: 'ADDITIONS', page: 0, size: 50 },
  headers: { 'Authorization': 'Bearer YOUR_TOKEN' }
});

// Get Deletions
axios.get(`/api/voter/sir-report/${jobId}/details`, {
  params: { type: 'DELETIONS', page: 0, size: 50 },
  headers: { 'Authorization': 'Bearer YOUR_TOKEN' }
});

// Get Shifts
axios.get(`/api/voter/sir-report/${jobId}/details`, {
  params: { type: 'SHIFTS', page: 0, size: 50 },
  headers: { 'Authorization': 'Bearer YOUR_TOKEN' }
});
```

```bash
# cURL - Get Additions
curl -X GET "http://localhost:8080/api/voter/sir-report/550e8400-e29b-41d4-a716-446655440000/details?type=ADDITIONS&page=0&size=50" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Response for ADDITIONS/DELETIONS
```json
{
  "content": [
    {
      "epicNumber": "ABC1234567",
      "partNo": 123,
      "voterNameEn": "John Doe",
      "serialNo": 456,
      "sectionNo": 1,
      "houseNoEn": "12/A",
      "age": 35,
      "gender": "M"
    },
    {
      "epicNumber": "XYZ9876543",
      "partNo": 124,
      "voterNameEn": "Jane Smith",
      "serialNo": 789,
      "sectionNo": 2,
      "houseNoEn": "45/B",
      "age": 28,
      "gender": "F"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 50,
    "offset": 0
  },
  "totalElements": 200,
  "totalPages": 4,
  "last": false,
  "first": true,
  "numberOfElements": 50,
  "empty": false
}
```

#### Response for SHIFTS
```json
{
  "content": [
    {
      "epicNumber": "ABC1234567",
      "oldPartNo": 123,
      "newPartNo": 125,
      "voterNameEn": "John Doe",
      "serialNo": 456,
      "sectionNo": 1,
      "houseNoEn": "12/A",
      "age": 35,
      "gender": "M"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 50,
    "offset": 0
  },
  "totalElements": 30,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 30,
  "empty": false
}
```

---

### 5. List All Comparisons
**Endpoint:** `GET /api/voter/sir-report/list`  
**Description:** Get paginated list of all comparison jobs

#### Query Parameters
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| electionId | Long | No | - | Filter by election ID |
| page | Integer | No | 0 | Page number (0-indexed) |
| size | Integer | No | 20 | Page size |

#### Request Example
```javascript
// JavaScript/Axios
axios.get('/api/voter/sir-report/list', {
  params: { electionId: 123, page: 0, size: 20 },
  headers: { 'Authorization': 'Bearer YOUR_TOKEN' }
});
```

```bash
# cURL
curl -X GET "http://localhost:8080/api/voter/sir-report/list?electionId=123&page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Response
```json
{
  "content": [
    {
      "jobId": "550e8400-e29b-41d4-a716-446655440000",
      "baseFileName": "voters_base_2024.xlsx",
      "newFileName": "voters_new_2025.xlsx",
      "status": "COMPLETED",
      "additions": 200,
      "deletions": 50,
      "shifts": 30,
      "createdAt": "2025-11-26T10:30:00",
      "completedAt": "2025-11-26T10:32:00"
    },
    {
      "jobId": "660e8400-e29b-41d4-a716-446655440001",
      "baseFileName": "voters_oct_2024.xlsx",
      "newFileName": "voters_nov_2024.xlsx",
      "status": "PROCESSING",
      "additions": null,
      "deletions": null,
      "shifts": null,
      "createdAt": "2025-11-26T11:00:00",
      "completedAt": null
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0
  },
  "totalElements": 45,
  "totalPages": 3,
  "last": false,
  "first": true,
  "numberOfElements": 20,
  "empty": false
}
```

---

### 6. Delete Comparison
**Endpoint:** `DELETE /api/voter/sir-report/{jobId}`  
**Description:** Delete a comparison job and all related data  
**Authorization:** SUPER_ADMIN or ADMIN only

#### Request Example
```javascript
// JavaScript/Axios
axios.delete(`/api/voter/sir-report/${jobId}`, {
  headers: { 'Authorization': 'Bearer YOUR_TOKEN' }
});
```

```bash
# cURL
curl -X DELETE "http://localhost:8080/api/voter/sir-report/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Response
```json
"Comparison deleted successfully"
```

---

## Excel File Requirements

### Supported Formats
- `.xlsx` (Excel 2007+)
- `.xls` (Excel 97-2003)

### Required Columns
The Excel files must contain these columns (case-insensitive, flexible naming):

| Column | Alternative Names | Required | Description |
|--------|------------------|----------|-------------|
| EPIC Number | EPIC, ELECTORS PHOTO IDENTITY CARD (EPIC) NO. | **Yes** | Unique voter ID |
| Part No | PART_NO, PART NUMBER | **Yes** | Polling booth part number |
| Name | NAME_ENGLISH, VOTER_NAME_EN | No | Voter name in English |
| Serial No | SL NO, SLNO IN PART, SERIAL_NO | No | Serial number in part |
| Section No | SECTION_NO | No | Section number |
| House No | HOUSE_NO | No | House number |
| Age | AGE | No | Voter age |
| Gender | SEX | No | Gender (M/F) |

### Sample Excel Structure
```
| EPIC NUMBER | PART NO | NAME         | AGE | GENDER | HOUSE NO |
|-------------|---------|--------------|-----|--------|----------|
| ABC1234567  | 123     | John Doe     | 35  | M      | 12/A     |
| XYZ9876543  | 124     | Jane Smith   | 28  | F      | 45/B     |
```

---

## Error Responses

### 400 Bad Request
```json
{
  "message": "Both base file and new file are required"
}
```

```json
{
  "message": "Only Excel files (.xlsx, .xls) are supported"
}
```

```json
"Invalid type. Use: ADDITIONS, DELETIONS, or SHIFTS"
```

### 404 Not Found
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Job not found or error: Job not found: 550e8400-e29b-41d4-a716-446655440000"
}
```

### 500 Internal Server Error
```json
{
  "message": "Error starting comparison: Invalid file format"
}
```

---

## Typical Frontend Flow

### Step 1: Upload Files
```javascript
const uploadFiles = async (baseFile, newFile, electionId) => {
  const formData = new FormData();
  formData.append('baseFile', baseFile);
  formData.append('newFile', newFile);
  if (electionId) formData.append('electionId', electionId);
  
  const response = await axios.post('/api/voter/sir-report/compare', formData);
  return response.data.jobId;
};
```

### Step 2: Poll for Status
```javascript
const pollStatus = async (jobId) => {
  const interval = setInterval(async () => {
    const response = await axios.get(`/api/voter/sir-report/${jobId}/status`);
    
    if (response.data.status === 'COMPLETED') {
      clearInterval(interval);
      // Fetch summary
      fetchSummary(jobId);
    } else if (response.data.status === 'FAILED') {
      clearInterval(interval);
      // Handle error
      console.error('Job failed');
    }
    // Update progress bar
    setProgress(response.data.progress);
  }, 3000); // Poll every 3 seconds
};
```

### Step 3: Display Summary
```javascript
const fetchSummary = async (jobId) => {
  const response = await axios.get(`/api/voter/sir-report/${jobId}/summary`);
  const { additions, deletions, shifts } = response.data.summary;
  
  // Display counts in UI
  console.log(`Additions: ${additions}, Deletions: ${deletions}, Shifts: ${shifts}`);
};
```

### Step 4: Show Details (When User Clicks)
```javascript
const fetchDetails = async (jobId, type, page = 0) => {
  const response = await axios.get(`/api/voter/sir-report/${jobId}/details`, {
    params: { type, page, size: 50 }
  });
  
  return response.data; // Returns paginated data
};
```

---

## React Component Example

```jsx
import React, { useState } from 'react';
import axios from 'axios';

const SirReportUpload = () => {
  const [baseFile, setBaseFile] = useState(null);
  const [newFile, setNewFile] = useState(null);
  const [jobId, setJobId] = useState(null);
  const [status, setStatus] = useState(null);
  const [progress, setProgress] = useState(0);
  const [summary, setSummary] = useState(null);

  const handleUpload = async () => {
    const formData = new FormData();
    formData.append('baseFile', baseFile);
    formData.append('newFile', newFile);
    
    try {
      const response = await axios.post('/api/voter/sir-report/compare', formData);
      setJobId(response.data.jobId);
      pollStatus(response.data.jobId);
    } catch (error) {
      console.error('Upload failed:', error);
    }
  };

  const pollStatus = (jobId) => {
    const interval = setInterval(async () => {
      const response = await axios.get(`/api/voter/sir-report/${jobId}/status`);
      setStatus(response.data.status);
      setProgress(response.data.progress);
      
      if (response.data.status === 'COMPLETED') {
        clearInterval(interval);
        fetchSummary(jobId);
      } else if (response.data.status === 'FAILED') {
        clearInterval(interval);
      }
    }, 3000);
  };

  const fetchSummary = async (jobId) => {
    const response = await axios.get(`/api/voter/sir-report/${jobId}/summary`);
    setSummary(response.data.summary);
  };

  return (
    <div>
      <input type="file" onChange={(e) => setBaseFile(e.target.files[0])} />
      <input type="file" onChange={(e) => setNewFile(e.target.files[0])} />
      <button onClick={handleUpload}>Upload & Compare</button>
      
      {status === 'PROCESSING' && (
        <div>
          <p>Progress: {progress}%</p>
          <progress value={progress} max="100" />
        </div>
      )}
      
      {summary && (
        <div>
          <h3>Comparison Results</h3>
          <p>Total Base: {summary.totalBaseRecords}</p>
          <p>Total New: {summary.totalNewRecords}</p>
          <p>Additions: {summary.additions}</p>
          <p>Deletions: {summary.deletions}</p>
          <p>Shifts: {summary.shifts}</p>
        </div>
      )}
    </div>
  );
};

export default SirReportUpload;
```

---

## Notes for Frontend Team

1. **File Upload:** Use `FormData` for multipart file upload
2. **Polling:** Poll status endpoint every 2-3 seconds until status is COMPLETED or FAILED
3. **Progress Bar:** Use the `progress` field (0-100) to show upload/processing progress
4. **Pagination:** All detail endpoints return paginated data with standard Spring Boot pagination format
5. **Authorization:** All endpoints require Bearer token in Authorization header
6. **Error Handling:** Handle 400, 404, and 500 errors appropriately

---

## Database Tables Created

If you need to query database directly:
- `sir_report_job` - Main job tracking
- `sir_report_addition` - Addition records
- `sir_report_deletion` - Deletion records
- `sir_report_shift` - Shift records

---

**Last Updated:** November 26, 2025  
**Contact:** Backend Team
