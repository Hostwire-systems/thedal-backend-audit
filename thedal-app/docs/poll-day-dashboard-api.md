# Poll-Day Dashboard API Documentation

## Overview
Real-time election turnout analytics with India Standard Time (IST) support. All endpoints provide cached responses with ETag headers and support manual recompute with rate limiting.

**Base URL:** `http://your-server:8080/api/reporting/poll-day`

---

## Authentication
All endpoints require standard authentication headers as per your existing API setup.

---

## Common Response Headers
- **ETag:** `"1725350502"` - Timestamp for client-side caching
- **Cache-Control:** `public, max-age=30` - Browser caching directive

---

## Error Responses
- **429 Too Many Requests** - Rate limit exceeded (30 seconds between recomputes)
- **400 Bad Request** - Invalid parameters
- **404 Not Found** - No data available (returns empty entity)

---

## 1. Hourly Turnout API

### GET - Retrieve Hourly Turnout Data
```
GET /api/reporting/poll-day/hourly
```

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `accountId` | Long | Yes | - | Account identifier |
| `electionId` | Long | Yes | - | Election identifier |
| `pollingDate` | String | No | Current IST date | Date in YYYY-MM-DD format |

**Example Request:**
```bash
GET /api/reporting/poll-day/hourly?accountId=12&electionId=45&pollingDate=2025-09-03
```

**Response Format:**
```json
{
  "id": 1,
  "accountId": 12,
  "electionId": 45,
  "pollingDate": "2025-09-03",
  "hourlyJson": "{\"06\":{\"voted\":25},\"07\":{\"voted\":73},\"08\":{\"voted\":142},\"09\":{\"voted\":298},\"10\":{\"voted\":456},\"11\":{\"voted\":623},\"12\":{\"voted\":789},\"13\":{\"voted\":945},\"14\":{\"voted\":1089},\"15\":{\"voted\":1234},\"16\":{\"voted\":1367},\"17\":{\"voted\":1489},\"18\":{\"voted\":1598}}",
  "computedAt": "2025-09-03T08:15:00+05:30",
  "refreshedAt": "2025-09-03T14:30:00+05:30"
}
```

**Hourly JSON Structure:**
```json
{
  "00": {"voted": 0},    // 12:00 AM - 12:59 AM
  "01": {"voted": 0},    // 1:00 AM - 1:59 AM
  ...
  "06": {"voted": 25},   // 6:00 AM - 6:59 AM (polling starts)
  "07": {"voted": 73},   // 7:00 AM - 7:59 AM
  ...
  "18": {"voted": 1598}, // 6:00 PM - 6:59 PM (polling ends)
  ...
  "23": {"voted": 1598}  // 11:00 PM - 11:59 PM
}
```

### POST - Recompute Hourly Turnout
```
POST /api/reporting/poll-day/hourly/recompute
```

**Query Parameters:** Same as GET endpoint

**Rate Limit:** 1 request per 30 seconds per (accountId, electionId)

**Example Request:**
```bash
POST /api/reporting/poll-day/hourly/recompute?accountId=12&electionId=45
```

---

## 2. Age Group Turnout API

### GET - Retrieve Age Group Turnout Data
```
GET /api/reporting/poll-day/age-groups
```

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `accountId` | Long | Yes | - | Account identifier |
| `electionId` | Long | Yes | - | Election identifier |
| `pollingDate` | String | No | Current IST date | Date in YYYY-MM-DD format |

**Example Request:**
```bash
GET /api/reporting/poll-day/age-groups?accountId=12&electionId=45&pollingDate=2025-09-03
```

**Response Format:**
```json
{
  "id": 1,
  "accountId": 12,
  "electionId": 45,
  "pollingDate": "2025-09-03",
  "ageGroupsJson": "{\"18_30\":{\"registered\":1200,\"voted\":480,\"pct\":40.0},\"30_40\":{\"registered\":980,\"voted\":441,\"pct\":45.0},\"40_50\":{\"registered\":750,\"voted\":337,\"pct\":45.0},\"50_60\":{\"registered\":620,\"voted\":310,\"pct\":50.0},\"60_70\":{\"registered\":450,\"voted\":225,\"pct\":50.0},\"gt_70\":{\"registered\":200,\"voted\":80,\"pct\":40.0},\"unknown\":{\"registered\":50,\"voted\":15,\"pct\":30.0}}",
  "computedAt": "2025-09-03T08:15:00+05:30",
  "refreshedAt": "2025-09-03T14:30:00+05:30"
}
```

**Age Groups JSON Structure:**
```json
{
  "18_30": {
    "registered": 1200,  // Total registered voters in age group
    "voted": 480,        // Number who have voted
    "pct": 40.0         // Percentage turnout
  },
  "30_40": {
    "registered": 980,
    "voted": 441,
    "pct": 45.0
  },
  "40_50": {
    "registered": 750,
    "voted": 337,
    "pct": 45.0
  },
  "50_60": {
    "registered": 620,
    "voted": 310,
    "pct": 50.0
  },
  "60_70": {
    "registered": 450,
    "voted": 225,
    "pct": 50.0
  },
  "gt_70": {
    "registered": 200,
    "voted": 80,
    "pct": 40.0
  },
  "unknown": {
    "registered": 50,
    "voted": 15,
    "pct": 30.0
  }
}
```

### POST - Recompute Age Group Turnout
```
POST /api/reporting/poll-day/age-groups/recompute
```

**Query Parameters:** Same as GET endpoint

---

## 3. Booth Summary API

### GET - Retrieve Booth Summary Data
```
GET /api/reporting/poll-day/booth-summary
```

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `accountId` | Long | Yes | - | Account identifier |
| `electionId` | Long | Yes | - | Election identifier |
| `pollingDate` | String | No | Current IST date | Date in YYYY-MM-DD format |

**Example Request:**
```bash
GET /api/reporting/poll-day/booth-summary?accountId=12&electionId=45&pollingDate=2025-09-03
```

**Response Format:**
```json
{
  "id": 1,
  "accountId": 12,
  "electionId": 45,
  "pollingDate": "2025-09-03",
  "boothSummaryJson": "{\"12\":{\"total\":450,\"voted\":180,\"pct\":40.0,\"lastVote\":\"2025-09-03T14:25:00\"},\"15\":{\"total\":380,\"voted\":152,\"pct\":40.0,\"lastVote\":\"2025-09-03T14:28:00\"},\"18\":{\"total\":520,\"voted\":234,\"pct\":45.0,\"lastVote\":\"2025-09-03T14:30:00\"}}",
  "computedAt": "2025-09-03T08:15:00+05:30",
  "refreshedAt": "2025-09-03T14:30:00+05:30"
}
```

**Booth Summary JSON Structure:**
```json
{
  "12": {
    "total": 450,                           // Total registered voters in booth
    "voted": 180,                          // Number who have voted
    "pct": 40.0,                          // Percentage turnout
    "lastVote": "2025-09-03T14:25:00"     // Timestamp of last vote (can be null)
  },
  "15": {
    "total": 380,
    "voted": 152,
    "pct": 40.0,
    "lastVote": "2025-09-03T14:28:00"
  },
  "18": {
    "total": 520,
    "voted": 234,
    "pct": 45.0,
    "lastVote": "2025-09-03T14:30:00"
  }
}
```

### POST - Recompute Booth Summary
```
POST /api/reporting/poll-day/booth-summary/recompute
```

**Query Parameters:** Same as GET endpoint

---

## Frontend Integration Examples

### JavaScript/React Example

```javascript
// API Service Class
class PollDayAPI {
  constructor(baseUrl = 'http://your-server:8080/api/reporting/poll-day') {
    this.baseUrl = baseUrl;
  }

  async getHourlyTurnout(accountId, electionId, pollingDate = null) {
    const params = new URLSearchParams({
      accountId: accountId.toString(),
      electionId: electionId.toString()
    });
    
    if (pollingDate) {
      params.append('pollingDate', pollingDate);
    }

    const response = await fetch(`${this.baseUrl}/hourly?${params}`);
    return response.json();
  }

  async getAgeGroupTurnout(accountId, electionId, pollingDate = null) {
    const params = new URLSearchParams({
      accountId: accountId.toString(),
      electionId: electionId.toString()
    });
    
    if (pollingDate) {
      params.append('pollingDate', pollingDate);
    }

    const response = await fetch(`${this.baseUrl}/age-groups?${params}`);
    return response.json();
  }

  async getBoothSummary(accountId, electionId, pollingDate = null) {
    const params = new URLSearchParams({
      accountId: accountId.toString(),
      electionId: electionId.toString()
    });
    
    if (pollingDate) {
      params.append('pollingDate', pollingDate);
    }

    const response = await fetch(`${this.baseUrl}/booth-summary?${params}`);
    return response.json();
  }

  async recomputeHourly(accountId, electionId, pollingDate = null) {
    const params = new URLSearchParams({
      accountId: accountId.toString(),
      electionId: electionId.toString()
    });
    
    if (pollingDate) {
      params.append('pollingDate', pollingDate);
    }

    const response = await fetch(`${this.baseUrl}/hourly/recompute?${params}`, {
      method: 'POST'
    });
    
    if (response.status === 429) {
      throw new Error('Rate limit exceeded. Please wait 30 seconds.');
    }
    
    return response.json();
  }
}

// Usage Example
const pollDayAPI = new PollDayAPI();

// Get current day hourly data
pollDayAPI.getHourlyTurnout(12, 45)
  .then(data => {
    const hourlyData = JSON.parse(data.hourlyJson);
    console.log('Votes at 2 PM:', hourlyData['14'].voted);
  });

// Get age group breakdown
pollDayAPI.getAgeGroupTurnout(12, 45)
  .then(data => {
    const ageGroups = JSON.parse(data.ageGroupsJson);
    console.log('Youth turnout (18-30):', ageGroups['18_30'].pct + '%');
  });

// Get booth-wise data
pollDayAPI.getBoothSummary(12, 45)
  .then(data => {
    const booths = JSON.parse(data.boothSummaryJson);
    Object.keys(booths).forEach(boothNum => {
      console.log(`Booth ${boothNum}: ${booths[boothNum].pct}% turnout`);
    });
  });
```

### Chart.js Integration Example

```javascript
// Hourly Turnout Line Chart
async function createHourlyChart(accountId, electionId) {
  const data = await pollDayAPI.getHourlyTurnout(accountId, electionId);
  const hourlyData = JSON.parse(data.hourlyJson);
  
  const ctx = document.getElementById('hourlyChart').getContext('2d');
  new Chart(ctx, {
    type: 'line',
    data: {
      labels: ['06:00', '07:00', '08:00', '09:00', '10:00', '11:00', '12:00', 
               '13:00', '14:00', '15:00', '16:00', '17:00', '18:00'],
      datasets: [{
        label: 'Votes Cast',
        data: [
          hourlyData['06'].voted, hourlyData['07'].voted, hourlyData['08'].voted,
          hourlyData['09'].voted, hourlyData['10'].voted, hourlyData['11'].voted,
          hourlyData['12'].voted, hourlyData['13'].voted, hourlyData['14'].voted,
          hourlyData['15'].voted, hourlyData['16'].voted, hourlyData['17'].voted,
          hourlyData['18'].voted
        ],
        borderColor: 'rgb(75, 192, 192)',
        tension: 0.1
      }]
    },
    options: {
      responsive: true,
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }
  });
}

// Age Group Doughnut Chart
async function createAgeGroupChart(accountId, electionId) {
  const data = await pollDayAPI.getAgeGroupTurnout(accountId, electionId);
  const ageGroups = JSON.parse(data.ageGroupsJson);
  
  const ctx = document.getElementById('ageGroupChart').getContext('2d');
  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['18-30', '31-40', '41-50', '51-60', '61-70', '70+'],
      datasets: [{
        data: [
          ageGroups['18_30'].pct,
          ageGroups['30_40'].pct,
          ageGroups['40_50'].pct,
          ageGroups['50_60'].pct,
          ageGroups['60_70'].pct,
          ageGroups['gt_70'].pct
        ],
        backgroundColor: [
          '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40'
        ]
      }]
    },
    options: {
      responsive: true,
      plugins: {
        title: {
          display: true,
          text: 'Turnout by Age Group (%)'
        }
      }
    }
  });
}
```

---

## Auto-Refresh Implementation

```javascript
class PollDayDashboard {
  constructor(accountId, electionId) {
    this.accountId = accountId;
    this.electionId = electionId;
    this.api = new PollDayAPI();
    this.refreshInterval = 30000; // 30 seconds
  }

  async startAutoRefresh() {
    // Initial load
    await this.refreshAllData();
    
    // Set up periodic refresh
    setInterval(async () => {
      await this.refreshAllData();
    }, this.refreshInterval);
  }

  async refreshAllData() {
    try {
      const [hourly, ageGroups, booths] = await Promise.all([
        this.api.getHourlyTurnout(this.accountId, this.electionId),
        this.api.getAgeGroupTurnout(this.accountId, this.electionId),
        this.api.getBoothSummary(this.accountId, this.electionId)
      ]);

      this.updateHourlyChart(hourly);
      this.updateAgeGroupChart(ageGroups);
      this.updateBoothTable(booths);
      
      // Update last refresh time
      document.getElementById('lastUpdate').textContent = 
        new Date().toLocaleTimeString('en-IN', { timeZone: 'Asia/Kolkata' });
        
    } catch (error) {
      console.error('Error refreshing data:', error);
    }
  }

  updateHourlyChart(data) {
    // Update existing Chart.js instance
  }

  updateAgeGroupChart(data) {
    // Update existing Chart.js instance
  }

  updateBoothTable(data) {
    // Update booth summary table
  }
}

// Initialize dashboard
const dashboard = new PollDayDashboard(12, 45);
dashboard.startAutoRefresh();
```

---

## Time Zone Notes

- All timestamps are in **India Standard Time (IST, UTC+05:30)**
- `pollingDate` parameter expects **YYYY-MM-DD** format in IST
- Hourly buckets (00-23) represent IST hours
- If `pollingDate` is omitted, current IST date is used
- Voting hours typically: **06:00 to 18:00 IST**

---

## Rate Limiting

- **Limit:** 1 recompute request per 30 seconds per (accountId, electionId)
- **Response:** 429 Too Many Requests if exceeded
- **Recommendation:** Use GET endpoints for frequent polling, POST only for manual refresh

---

## Error Handling Best Practices

```javascript
async function safeAPICall(apiFunction) {
  try {
    return await apiFunction();
  } catch (error) {
    if (error.status === 429) {
      console.warn('Rate limited. Using cached data.');
      return null;
    } else if (error.status === 404) {
      console.info('No data available yet.');
      return { empty: true };
    } else {
      console.error('API Error:', error);
      throw error;
    }
  }
}
```

This documentation provides everything your frontend team needs to integrate the poll-day dashboard APIs effectively!
