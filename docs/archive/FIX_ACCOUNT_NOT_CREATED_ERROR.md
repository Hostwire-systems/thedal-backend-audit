# Fix for "Account not created for this account id" Error

## Issue Description
Multiple reporting APIs (e.g., `/reporting/api/aggregates/election/party-polling/{accountId}/{electionId}`) were failing with:
```json
{
    "status": "error",
    "code": 40401,
    "message": "Account not created for this account id"
}
```

## Root Cause
The issue was caused by **lazy loading** of the `AccountEntity` relationship in `UserEntity`:

1. When `JwtAuthenticationFilter` loaded the user via `userRepo.findById(userId)`, it only loaded the `UserEntity` object
2. The `@ManyToOne` relationship to `AccountEntity` was **lazy-loaded** (not fetched immediately)
3. When code later tried to access `user.getAccountEntity().getId()` in `RequestDetailsService.getCurrentAccountId()`, it encountered a **NullPointerException** because:
   - The Hibernate session was already closed (outside transaction context)
   - The `accountEntity` proxy couldn't be initialized
   - Or in some cases, the relationship was genuinely null

## Solution Applied

### 1. Added Eager Fetch Query in UserRepo
**File:** `UserRepo.java`

Added a new query method that uses `JOIN FETCH` to eagerly load both `accountEntity` and `role`:

```java
@Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.accountEntity LEFT JOIN FETCH u.role WHERE u.id = :id")
Optional<UserEntity> findByIdWithAccountAndRole(@Param("id") Long id);
```

**Why LEFT JOIN FETCH?**
- `LEFT JOIN` ensures we get the user even if `accountEntity` is null (edge case)
- `FETCH` tells Hibernate to load the related entities immediately in the same query
- This prevents lazy loading issues

### 2. Updated JwtAuthenticationFilter
**File:** `JwtAuthenticationFilter.java`

Changed from:
```java
UserEntity user = userRepo.findById(userId).orElseThrow(...)
```

To:
```java
UserEntity user = userRepo.findByIdWithAccountAndRole(userId).orElseThrow(...)
```

This ensures the `accountEntity` and `role` are loaded when the user is authenticated.

### 3. Added Defensive Null Check
**File:** `RequestDetailsService.java`

Enhanced `getCurrentAccountId()` with proper null checking and logging:

```java
public Long getCurrentAccountId() {
    UserEntity user = getCurrentUserFromRequest();
    if (user.getAccountEntity() == null) {
        log.error("AccountEntity is null for user ID: {}. User may not be properly associated with an account.", user.getId());
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }
    return user.getAccountEntity().getId();
}
```

**Benefits:**
- Better error logging with user ID
- Clearer error message for debugging
- Prevents NullPointerException
- Still throws the expected exception if account is genuinely missing

## Changes Summary

| File | Change Type | Description |
|------|-------------|-------------|
| `UserRepo.java` | Added Method | New `findByIdWithAccountAndRole()` query with eager fetching |
| `JwtAuthenticationFilter.java` | Modified | Use new eager-fetch query instead of `findById()` |
| `RequestDetailsService.java` | Enhanced | Add null check and better error logging in `getCurrentAccountId()` |

## Testing Instructions

1. **Restart the application** to load the new code
2. **Test the failing endpoint** with the same curl command:
   ```bash
   curl "http://localhost:8080/reporting/api/aggregates/election/party-polling/2558/146" \
     -H "Authorization: Bearer <your-token>"
   ```
3. **Verify the logs** - you should now see the user's accountId being properly resolved
4. **Expected result:** The API should return data instead of the error

## Additional Notes

- This fix applies to **all APIs** that use `requestDetails.getCurrentAccountId()`
- The eager fetching adds minimal overhead (one JOIN in the authentication query)
- This is a **safe change** - it makes existing behavior more robust without changing API contracts
- If a user genuinely has no account association, the error message is now more informative

## Rollback Instructions (if needed)

If you need to rollback:

1. In `JwtAuthenticationFilter.java`, change line ~103 back to:
   ```java
   UserEntity user = userRepo.findById(userId).orElseThrow(...)
   ```

2. In `RequestDetailsService.java`, simplify line ~47-53 back to:
   ```java
   public Long getCurrentAccountId() {
       return getCurrentUserFromRequest().getAccountEntity().getId();
   }
   ```

3. Remove the `findByIdWithAccountAndRole()` method from `UserRepo.java`

---
**Applied:** October 29, 2025  
**Build Status:** ✅ SUCCESS  
**Affected Components:** Authentication, User Management, All APIs using account context
