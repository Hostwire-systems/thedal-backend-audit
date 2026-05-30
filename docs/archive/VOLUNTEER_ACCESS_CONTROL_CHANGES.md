# Volunteer Access Control Changes

## Summary
Updated the voter management system to allow volunteers **READ access to all voters** while restricting **WRITE operations (edit/update/delete)** to only voters in their assigned booths.

## Date
21 October 2025

## Changes Made

### 1. Modified Read Operations (GET APIs)
**File**: `VoterServiceImpl.java` and `VoterMongoService.java`

**Method**: `getEffectiveBoothNumbersFast()`

**Before**: 
- Only ADMIN and SUPER_ADMIN could view all voters
- Volunteers were restricted to viewing only voters from their assigned booths

**After**:
- **ALL roles** (including volunteers) can now view all voters
- No booth restrictions applied for READ operations

```java
private List<Integer> getEffectiveBoothNumbersFast(List<Integer> boothNumbers, Role userRole, Long userId) {
    // For READ operations: Allow all roles to see all booths (no restrictions)
    // Volunteers can now view all voters regardless of their assigned booths
    log.info("VOLUNTEER FILTER DEBUG - READ operation: Allowing access to all booths for all roles");
    return boothNumbers;
}
```

### 2. Added Write Operation Validation
**File**: `VoterServiceImpl.java`

**New Method**: `validateBoothAccessForWrite()`

This new validation method ensures that:
- **ADMIN and SUPER_ADMIN** can modify all voters (no restrictions)
- **Volunteers** can only modify voters from their assigned booths
- Throws `ACCESS_DENIED` exception if a volunteer tries to modify a voter outside their assigned booths

```java
private void validateBoothAccessForWrite(Integer voterBoothNumber, Role userRole, Long userId) {
    // ADMIN and SUPER_ADMIN can modify all voters
    if ("SUPER_ADMIN".equalsIgnoreCase(userRole.getRoleName()) || "ADMIN".equalsIgnoreCase(userRole.getRoleName())) {
        return;
    }
    
    // For all other roles, check volunteer booth assignments
    VolunteerEntity volunteer = volunteerRepository.findByUserEntity_Id(userId)
            .orElseThrow(() -> new ThedalException(ThedalError.VOLUNTEER_NOT_FOUND, HttpStatus.NOT_FOUND));

    List<Long> assignedBooths = volunteer.getAssignedBooth();
    
    boolean hasAccess = assignedBooths.stream()
            .anyMatch(booth -> booth.intValue() == voterBoothNumber.intValue());
    
    if (!hasAccess) {
        throw new ThedalException(
            ThedalError.ACCESS_DENIED, 
            HttpStatus.FORBIDDEN, 
            "You do not have permission to modify voters in booth " + voterBoothNumber
        );
    }
}
```

### 3. Protected Write Operations

The following methods now include booth access validation for volunteers:

#### a. Update Voter (`updateVoter`)
- Validates booth access before allowing any voter updates
- Only allows volunteers to update voters in their assigned booths

#### b. Delete Voter (`deleteById`)
- Validates booth access before voter deletion
- Only allows volunteers to delete voters in their assigned booths

#### c. Update Voting Status (`updateVoterVotingStatus`)
- Validates booth access before marking voter as voted/not-voted
- Only allows volunteers to update voting status for voters in their assigned booths

#### d. Bulk Update Voting Status (`markMultipleVotersAsVoted`)
- Validates booth access for each voter in the bulk request
- Skips voters that are not in volunteer's assigned booths (treats as "not found")

#### e. Update Voter Image (`updateVoterImage`)
- Validates booth access before allowing image upload
- Only allows volunteers to upload images for voters in their assigned booths

#### f. Remove Voter Image (`removeVoterImage`)
- Validates booth access before allowing image deletion
- Only allows volunteers to remove images for voters in their assigned booths

#### g. Map Family (`mapFamily`)
- Validates booth access before allowing family ID mapping
- Only allows volunteers to map families for voters in their assigned booths

#### h. Delete Family Mapping (`deleteFamilyId`)
- Validates booth access before allowing family ID deletion
- Only allows volunteers to remove family mappings for voters in their assigned booths

## Impact

### ✅ Benefits
1. **Better User Experience**: Volunteers can now search and view all voters across all booths
2. **Maintained Security**: Write operations are still restricted to assigned booths only
3. **Consistent Validation**: All write operations now have uniform booth access control
4. **Clear Error Messages**: Volunteers receive clear feedback when trying to modify voters outside their booths

### ⚠️ Security Considerations
- Read access is now open to all authenticated users with any role
- Write operations remain secured with booth-level access control
- ADMIN and SUPER_ADMIN maintain full access to all operations

## Testing Recommendations

### Test Cases for Volunteers:

1. **READ Operations** (Should succeed):
   - GET voters from all booths
   - Search voters across the entire election
   - View voter details from any booth

2. **WRITE Operations on Assigned Booths** (Should succeed):
   - Update voter in assigned booth
   - Delete voter in assigned booth
   - Update voting status for voter in assigned booth
   - Upload/remove voter image in assigned booth
   - Map/unmap family for voter in assigned booth

3. **WRITE Operations on Non-Assigned Booths** (Should fail with 403 FORBIDDEN):
   - Update voter in non-assigned booth
   - Delete voter in non-assigned booth
   - Update voting status for voter in non-assigned booth
   - Upload/remove voter image in non-assigned booth
   - Map/unmap family for voter in non-assigned booth

### Test Cases for ADMIN/SUPER_ADMIN:

1. **All Operations** (Should succeed):
   - Read voters from all booths
   - Write to voters in any booth
   - No restrictions on any operation

## Files Modified

1. `/thedal-app/src/main/java/com/thedal/thedal_app/voter/VoterServiceImpl.java`
   - Modified `getEffectiveBoothNumbersFast()` method
   - Added `validateBoothAccessForWrite()` method
   - Added booth validation to 8 write operation methods

2. `/thedal-app/src/main/java/com/thedal/thedal_app/voter/VoterMongoService.java`
   - Modified `getEffectiveBoothNumbersFast()` method

## Rollback Plan

If issues arise, revert the following:
1. Restore `getEffectiveBoothNumbersFast()` to previous implementation (with volunteer booth filtering)
2. Remove `validateBoothAccessForWrite()` method and its invocations
3. Test all volunteer operations thoroughly

## Notes

- MongoDB sync operations remain disabled (as per existing code comments)
- All debug logging has been retained for troubleshooting
- Error messages provide specific details about booth access restrictions
