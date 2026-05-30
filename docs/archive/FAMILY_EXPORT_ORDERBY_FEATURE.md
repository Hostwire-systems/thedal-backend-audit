# Family Voter Card Export - Order By Feature

## Overview
Added a new `orderBy` parameter to the Family Voter Card Export API that allows exporting voters in two different ordering modes:
1. **Family-based ordering** (default) - Groups families together with page breaks
2. **Serial number ordering** (new) - Orders voters by their serial numbers in continuous flow

## Changes Made

### 1. Database Schema Change
**File:** `FamilyVoterCardExportJob.java`

Added new column to store ordering preference:
```java
@Column(name = "order_by", length = 20)
private String orderBy = "family"; // ordering preference: "family" or "serial"
```

**Migration Required:**
```sql
ALTER TABLE family_voter_card_export_job 
ADD COLUMN order_by VARCHAR(20) DEFAULT 'family';
```

### 2. API Endpoint Update
**File:** `FamilyVoterCardExportController.java`

Added new optional parameter with validation:
```java
@PostMapping("/export-jobs")
public ResponseEntity<ThedalResponse<FamilyVoterCardExportJob>> createJob(
    // ... existing parameters ...
    @RequestParam(required = false, defaultValue = "family") String orderBy
)
```

**Validation:**
- Only accepts `"family"` or `"serial"` values
- Returns `INVALID_INPUT` error for other values
- Defaults to `"family"` if not provided (backward compatible)

### 3. Export Logic Implementation
**File:** `FamilyVoterCardExportAsyncService.java`

#### Sorting Logic:
```java
// For orderBy = "serial"
voters.sort(Comparator
    .comparing(v -> v.getSerialNo() == null ? 0L : v.getSerialNo())
    .thenComparing(v -> v.getPageNumber() == null ? 0 : v.getPageNumber())
    .thenComparing(VoterEntity::getVoterId));

// For orderBy = "family" (default)
voters.sort(Comparator
    .comparing(v -> v.getFamilySequenceNumber() == null ? 0 : v.getFamilySequenceNumber())
    .thenComparing(VoterEntity::getVoterId));
```

#### Rendering Logic:
```java
if (job.getPartNo() != null && "family".equals(orderBy)) {
    // Family grouping with page breaks
    pdf = htmlPdfRenderer.renderWithFamilyPageBreaks(models, columns);
} else {
    // Continuous flow (no family page breaks)
    pdf = htmlPdfRenderer.render(models, columns);
}
```

## API Usage

### Example 1: Export by Family Sequence (Default/Existing Behavior)
```bash
POST /api/v1/families/ANY_FAMILY_ID/election/123/voter-cards/export-jobs
Query Parameters:
  - accountId: 1
  - partNo: 5
  - columns: 2
  - orderBy: family  # Optional, this is the default
```

**Result:**
- Voters grouped by families
- Ordered by `family_sequence_number`
- Page breaks between each family
- Family members stay together

### Example 2: Export by Serial Number (New Feature)
```bash
POST /api/v1/families/ANY_FAMILY_ID/election/123/voter-cards/export-jobs
Query Parameters:
  - accountId: 1
  - partNo: 5
  - columns: 2
  - orderBy: serial  # NEW parameter
```

**Result:**
- Voters ordered by `serial_no` (original voter list order)
- No family-based page breaks
- Continuous flow through the entire part
- Matches the physical voter list sequence

## Behavior Matrix

| Scenario | orderBy | partNo | Sorting | Page Breaks | Use Case |
|----------|---------|--------|---------|-------------|----------|
| Single Family | family | null | voterId | None | Print single family voter cards |
| Single Family | serial | null | serialNo | None | Print single family in serial order |
| Part-wide | family | provided | family_sequence_number | Between families | Distribute cards family-wise |
| Part-wide | serial | provided | serialNo | None (continuous) | Print part in voter list order |

## Database Impact

### New Column
- **Table:** `family_voter_card_export_job`
- **Column:** `order_by` VARCHAR(20) DEFAULT 'family'
- **Values:** 'family' or 'serial'

### Indexes Used
Existing indexes are sufficient:
- For family ordering: Uses `family_sequence_number` (already indexed via family queries)
- For serial ordering: Uses `serial_no` (already indexed for performance)

## Backward Compatibility

✅ **Fully Backward Compatible**
- Default value is `"family"` (existing behavior)
- All existing API calls work without changes
- No breaking changes to response structure
- Old jobs without `orderBy` field will be treated as `"family"`

## Testing Checklist

### Unit Tests Needed
- [ ] Validate `orderBy` parameter accepts only "family" or "serial"
- [ ] Verify default value is "family" when not provided
- [ ] Test sorting logic for both modes
- [ ] Test rendering selection logic

### Integration Tests Needed
- [ ] Export single family with `orderBy=family`
- [ ] Export single family with `orderBy=serial`
- [ ] Export part with `orderBy=family` (verify page breaks)
- [ ] Export part with `orderBy=serial` (verify no family page breaks)
- [ ] Verify sorting order matches expected sequence
- [ ] Test with families having null serial numbers
- [ ] Test with large parts (near MAX_EXPORT_SIZE)

### Manual Testing Scenarios
1. **Scenario A:** Part export with family ordering
   - Create job with `partNo=5&orderBy=family`
   - Verify families are grouped with page breaks
   - Confirm order by family_sequence_number

2. **Scenario B:** Part export with serial ordering
   - Create job with `partNo=5&orderBy=serial`
   - Verify continuous flow (no family page breaks)
   - Confirm order matches physical voter list
   - Check that family members appear at their serial positions

3. **Scenario C:** Invalid orderBy value
   - Try `orderBy=invalid`
   - Verify returns INVALID_INPUT error

## Performance Considerations

- ✅ No additional database queries
- ✅ Sorting happens in memory (already done for existing feature)
- ✅ No impact on export time
- ✅ Same MAX_EXPORT_SIZE limit (2000 voters) applies
- ✅ Rendering method selection is lightweight

## Documentation Updates Needed

- [ ] Update API documentation with new `orderBy` parameter
- [ ] Add examples for both ordering modes
- [ ] Update Swagger/OpenAPI specs
- [ ] Document use cases for each ordering mode

## Future Enhancements

Possible future improvements:
1. Add `orderBy=booth` to order by booth/part numbers across parts
2. Add custom sort fields (e.g., by name, age, etc.)
3. Add option to control page breaks independently of ordering
4. Support for composite ordering (e.g., "family,serial")

## Related Files Modified

1. `FamilyVoterCardExportJob.java` - Entity with new field
2. `FamilyVoterCardExportController.java` - API parameter handling
3. `FamilyVoterCardExportAsyncService.java` - Business logic implementation

## Migration Script

```sql
-- Add the new column with default value
ALTER TABLE family_voter_card_export_job 
ADD COLUMN IF NOT EXISTS order_by VARCHAR(20) DEFAULT 'family';

-- Verify the change
SELECT COUNT(*) FROM family_voter_card_export_job WHERE order_by IS NULL;
-- Should return 0

-- Verify default value works
SELECT order_by, COUNT(*) 
FROM family_voter_card_export_job 
GROUP BY order_by;
```

## Notes

- The feature maintains the existing 2 or 3 column layout options
- Both ordering modes work with Tamil text rendering
- S3 upload and job tracking remain unchanged
- Error handling and retry logic remain the same
