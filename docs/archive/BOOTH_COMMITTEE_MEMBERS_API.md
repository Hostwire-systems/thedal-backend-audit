# Booth Committee Members API Documentation

## Overview
This document describes the **Booth Committee Members** feature added to the Part Manager API. Each part/booth can now store up to **15 committee members** with their name, designation, and mobile number.

---

## 🎯 **Feature Summary**

### **What's New:**
- ✅ Each part can have **up to 15 booth committee members**
- ✅ Each member has: **name** (required), **designation** (required), **mobile number** (optional)
- ✅ Stored as **JSON** in PostgreSQL for flexibility
- ✅ Full validation with detailed error messages
- ✅ Works with both `multipart/form-data` and `application/json` content types

---

## 📊 **Data Structure**

### **BoothCommitteeMemberDTO**
```json
{
  "name": "John Doe",              // Required, 1-100 characters, alphanumeric + spaces
  "designation": "President",       // Required, 1-50 characters
  "mobileNumber": "9876543210"      // Optional, exactly 10 digits if provided
}
```

### **Full Part Manager Request Example**
```json
{
  "partNo": "123",
  "partNameEnglish": "Part Name",
  "partNameL1": "பகுதி பெயர்",
  "schoolName": "School Name",
  "pincode": "600001",
  "partLat": 13.0827,
  "partLong": 80.2707,
  "schoolLat": 13.0827,
  "schoolLong": 80.2707,
  "boothVulnerability": "HIGH",
  "partCaptainName": "Captain Name",
  "captainDesignation": "Captain",
  "captainMobileNo": "9876543210",
  "bloName": "BLO Name",
  "bloDesignation": "BLO",
  "bloMobileNumber": "9876543211",
  "bla2Name": "BLA2 Name",
  "bla2Designation": "BLA2",
  "bla2MobileNumber": "9876543212",
  "boothCommitteeMembers": [
    {
      "name": "John Doe",
      "designation": "President",
      "mobileNumber": "9876543210"
    },
    {
      "name": "Jane Smith",
      "designation": "Secretary",
      "mobileNumber": "9876543211"
    },
    {
      "name": "Bob Johnson",
      "designation": "Treasurer",
      "mobileNumber": "9876543212"
    }
  ]
}
```

---

## 🔌 **API Endpoints**

### **1. Create Part with Committee Members**

#### **Endpoint:**
```
POST /elections/partmanager/{electionId}
```

#### **Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

#### **Request Body:**
```json
{
  "partNo": "123",
  "partNameEnglish": "Ward 1 Booth A",
  "boothCommitteeMembers": [
    {
      "name": "John Doe",
      "designation": "President",
      "mobileNumber": "9876543210"
    },
    {
      "name": "Jane Smith",
      "designation": "Secretary",
      "mobileNumber": ""
    }
  ]
}
```

#### **Response (200 OK):**
```json
{
  "success": "PARTMANAGER_CREATED",
  "data": {
    "id": 1,
    "partNo": "123",
    "partNameEnglish": "Ward 1 Booth A",
    "boothCommitteeMembers": [
      {
        "name": "John Doe",
        "designation": "President",
        "mobileNumber": "9876543210"
      },
      {
        "name": "Jane Smith",
        "designation": "Secretary",
        "mobileNumber": null
      }
    ]
  }
}
```

---

### **2. Update Part with Committee Members**

#### **Endpoint:**
```
PUT /elections/partmanager/{electionId}/{partManagerId}
```

#### **Behavior:**
- ✅ If `boothCommitteeMembers` **not included** → **Keeps existing members**
- ✅ If `boothCommitteeMembers = []` → **Clears all members**
- ✅ If `boothCommitteeMembers = [{...}]` → **Replaces with new members**

#### **Example 1: Update members**
```json
{
  "partNo": "123",
  "partNameEnglish": "Ward 1 Booth A",
  "boothCommitteeMembers": [
    {
      "name": "Updated Name",
      "designation": "Vice President",
      "mobileNumber": "9999999999"
    }
  ]
}
```

#### **Example 2: Clear all members**
```json
{
  "partNo": "123",
  "partNameEnglish": "Ward 1 Booth A",
  "boothCommitteeMembers": []
}
```

#### **Example 3: Keep existing members**
```json
{
  "partNo": "123",
  "partNameEnglish": "Updated Name"
  // boothCommitteeMembers NOT included → Existing members remain unchanged
}
```

---

### **3. Get Part with Committee Members**

#### **Endpoint:**
```
GET /elections/partmanager/{electionId}/{partId}
```

#### **Response:**
```json
{
  "id": 1,
  "partNo": "123",
  "partNameEnglish": "Ward 1 Booth A",
  "boothCommitteeMembers": [
    {
      "name": "John Doe",
      "designation": "President",
      "mobileNumber": "9876543210"
    }
  ]
}
```

---

## ✅ **Validation Rules**

### **1. Array Size**
- **Min:** 0 entries (empty array allowed)
- **Max:** 15 entries
- **Error:** `"Booth committee members cannot exceed 15 entries"`

### **2. Name Field**
- **Required:** Yes
- **Length:** 1-100 characters
- **Pattern:** Alphanumeric + spaces only (`^[a-zA-Z0-9\s]+$`)
- **Examples:**
  - ✅ Valid: `"John Doe"`, `"Rajesh Kumar 123"`, `"A B C"`
  - ❌ Invalid: `""`, `"John@Doe"`, `"Name#123"`, `null`
- **Error:** `"Entry at index 2: name is required"` or `"Entry at index 2: name must contain only letters, numbers, and spaces"`

### **3. Designation Field**
- **Required:** Yes
- **Length:** 1-50 characters
- **Examples:**
  - ✅ Valid: `"President"`, `"Secretary"`, `"Treasurer"`, `"Member"`
  - ❌ Invalid: `""`, `null`
- **Error:** `"Entry at index 2: designation is required"`

### **4. Mobile Number Field**
- **Required:** No (optional)
- **Pattern:** Exactly 10 digits (`^[0-9]{10}$`)
- **Empty String Handling:** Treated as `null`
- **Examples:**
  - ✅ Valid: `"9876543210"`, `null`, `""`
  - ❌ Invalid: `"123"`, `"abcd123456"`, `"98765432101"` (11 digits)
- **Error:** `"Entry at index 2: mobile number must be exactly 10 digits"`

---

## ❌ **Error Responses**

### **Error 1: Too Many Members**
```json
{
  "success": "FAILURE",
  "error": {
    "code": 50000,
    "message": "Booth committee members cannot exceed 15 entries"
  }
}
```

### **Error 2: Validation Failures**
```json
{
  "success": "FAILURE",
  "error": {
    "code": 50000,
    "message": "Booth committee members validation failed:\nEntry at index 0: name is required\nEntry at index 1: mobile number must be exactly 10 digits\nEntry at index 2: designation must be between 1 and 50 characters"
  }
}
```

### **Error 3: Invalid Name Pattern**
```json
{
  "success": "FAILURE",
  "error": {
    "code": 50000,
    "message": "Entry at index 0: name must contain only letters, numbers, and spaces"
  }
}
```

---

## 🧪 **Testing Examples**

### **Test 1: Create with Valid Members**
```bash
curl -X POST "http://localhost:8080/elections/partmanager/58" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "partNo": "123",
    "partNameEnglish": "Test Part",
    "boothCommitteeMembers": [
      {
        "name": "Test User 1",
        "designation": "President",
        "mobileNumber": "9876543210"
      }
    ]
  }'
```

### **Test 2: Create with Empty Array**
```bash
curl -X POST "http://localhost:8080/elections/partmanager/58" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "partNo": "124",
    "partNameEnglish": "Test Part 2",
    "boothCommitteeMembers": []
  }'
```

### **Test 3: Update - Clear Members**
```bash
curl -X PUT "http://localhost:8080/elections/partmanager/58/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "partNo": "123",
    "partNameEnglish": "Updated Part",
    "boothCommitteeMembers": []
  }'
```

### **Test 4: Update - Keep Existing (field not included)**
```bash
curl -X PUT "http://localhost:8080/elections/partmanager/58/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "partNo": "123",
    "partNameEnglish": "Updated Name Only"
  }'
```

### **Test 5: Validation Error - Too Many Members**
```bash
curl -X POST "http://localhost:8080/elections/partmanager/58" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "partNo": "125",
    "partNameEnglish": "Test Part",
    "boothCommitteeMembers": [
      {"name": "Member 1", "designation": "Role 1"},
      {"name": "Member 2", "designation": "Role 2"},
      {"name": "Member 3", "designation": "Role 3"},
      {"name": "Member 4", "designation": "Role 4"},
      {"name": "Member 5", "designation": "Role 5"},
      {"name": "Member 6", "designation": "Role 6"},
      {"name": "Member 7", "designation": "Role 7"},
      {"name": "Member 8", "designation": "Role 8"},
      {"name": "Member 9", "designation": "Role 9"},
      {"name": "Member 10", "designation": "Role 10"},
      {"name": "Member 11", "designation": "Role 11"},
      {"name": "Member 12", "designation": "Role 12"},
      {"name": "Member 13", "designation": "Role 13"},
      {"name": "Member 14", "designation": "Role 14"},
      {"name": "Member 15", "designation": "Role 15"},
      {"name": "Member 16", "designation": "Role 16"}
    ]
  }'
```

**Expected Error:**
```json
{
  "error": {
    "message": "Booth committee members cannot exceed 15 entries"
  }
}
```

---

## 🗄️ **Database Schema**

### **Column Details:**
```sql
ALTER TABLE part_manager 
ADD COLUMN booth_committee_members TEXT DEFAULT '[]';
```

- **Column Name:** `booth_committee_members`
- **Data Type:** `TEXT` (stores JSON)
- **Default Value:** `'[]'` (empty array)
- **Nullable:** Yes
- **Format:** JSON array of objects

### **Sample Data:**
```sql
SELECT id, part_no, booth_committee_members 
FROM part_manager 
WHERE id = 1;

-- Result:
-- id | part_no | booth_committee_members
-- 1  | 123     | [{"name":"John Doe","designation":"President","mobileNumber":"9876543210"}]
```

---

## 📝 **Frontend Implementation Guide**

### **1. Form Structure**
```typescript
interface BoothCommitteeMember {
  name: string;
  designation: string;
  mobileNumber?: string;
}

interface PartManagerForm {
  partNo: string;
  partNameEnglish: string;
  // ... other fields
  boothCommitteeMembers: BoothCommitteeMember[];
}
```

### **2. Add Member**
```typescript
const [members, setMembers] = useState<BoothCommitteeMember[]>([]);

const addMember = () => {
  if (members.length >= 15) {
    alert("Maximum 15 members allowed");
    return;
  }
  setMembers([...members, { name: "", designation: "", mobileNumber: "" }]);
};
```

### **3. Remove Member**
```typescript
const removeMember = (index: number) => {
  setMembers(members.filter((_, i) => i !== index));
};
```

### **4. Clear All Members**
```typescript
const clearAllMembers = () => {
  setMembers([]);
};
```

### **5. Update Part (Keep Existing)**
```typescript
// If you DON'T want to modify committee members, simply omit the field
const updateData = {
  partNo: formData.partNo,
  partNameEnglish: formData.partNameEnglish
  // boothCommitteeMembers NOT included → Keeps existing
};
```

### **6. Update Part (Clear Members)**
```typescript
// To clear all members, send empty array
const updateData = {
  partNo: formData.partNo,
  partNameEnglish: formData.partNameEnglish,
  boothCommitteeMembers: []  // Clear all
};
```

---

## ⚠️ **Important Notes**

1. **Whitespace Handling:** All fields are trimmed automatically
2. **Empty Mobile:** Empty string `""` is converted to `null`
3. **Case Sensitive:** Name pattern validation is case-insensitive for letters
4. **Duplicate Mobiles:** Allowed (no unique constraint)
5. **Update Behavior:** If field not provided, existing value is preserved
6. **GET Response:** Always returns array (never null)
7. **Backward Compatibility:** Old parts without members return `[]`

---

## 🚀 **Deployment Checklist**

- ✅ Run database migration: `add_booth_committee_members_to_part_manager.sql`
- ✅ Restart Spring Boot application
- ✅ Test POST endpoint with sample data
- ✅ Test PUT endpoint (update, clear, keep)
- ✅ Test GET endpoint
- ✅ Verify validation errors
- ✅ Test with 15 members (max limit)
- ✅ Test with 16 members (should fail)
- ✅ Update frontend forms
- ✅ Update API documentation

---

## 📞 **Support**

For issues or questions:
- Check validation error messages (they include index and specific error)
- Verify JSON format in database
- Check application logs for detailed errors
- Test with Postman/curl before frontend integration

---

**Last Updated:** December 3, 2025  
**Version:** 1.0  
**Status:** ✅ Production Ready
