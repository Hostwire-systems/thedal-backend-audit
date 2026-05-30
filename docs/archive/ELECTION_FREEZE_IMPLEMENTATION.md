## Election Freeze Implementation Guide

### Backend Protection

#### 1. Add Annotation to Write Operations

Apply `@CheckElectionNotFrozen` annotation to all methods that modify data related to an election.

**Example - VoterService:**
```java
import com.thedal.thedal_app.election.ElectionFreezeInterceptor.CheckElectionNotFrozen;

@Service
public class VoterService {
    
    // Read operation - no annotation needed
    public VoterDTO getVoter(Long voterId, Long electionId) {
        // ...
    }
    
    // Write operation - add annotation
    @CheckElectionNotFrozen(electionIdParamIndex = 1) // electionId is 2nd parameter (index 1)
    public VoterDTO updateVoter(Long voterId, Long electionId, VoterUpdateRequest request) {
        // This will throw ELECTION_FROZEN error if election is frozen
        // ...
    }
    
    // Write operation where electionId is first parameter
    @CheckElectionNotFrozen(electionIdParamIndex = 0) // electionId is 1st parameter (index 0)
    public VoterDTO createVoter(Long electionId, VoterCreateRequest request) {
        // ...
    }
    
    // Write operation with custom message
    @CheckElectionNotFrozen(
        electionIdParamIndex = 0,
        message = "Cannot add family members. Election is frozen."
    )
    public void addFamilyMembers(Long electionId, List<FamilyMemberDTO> members) {
        // ...
    }
}
```

#### 2. Methods to Annotate

Apply `@CheckElectionNotFrozen` to:

**Voter Operations:**
- `updateVoter()`
- `createVoter()`
- `deleteVoter()`
- `bulkUpdateVoters()`
- `importVoters()`
- `updateVoterPhoto()`
- `addFamilyMembers()`
- `updateContactStatus()`
- `addVoterActivity()`

**Booth/Part Operations:**
- `updateBooth()`
- `createBooth()`
- `assignBoothToVolunteer()`
- `updatePartManager()`

**Party/Candidate Operations:**
- `createParty()`
- `updateParty()`
- `deleteParty()`
- `createCandidate()`
- `updateCandidate()`

**Template/File Operations:**
- `uploadBannerImage()`
- `updateTemplate()`
- `deleteFile()`

**Volunteer Operations:**
- `assignVolunteer()`
- `updateVolunteerAssignment()`

**Settings:**
- `updateElectionSettings()`
- `updateFieldConfiguration()`

#### 3. Controllers to Update

Make sure controllers pass electionId to service methods:

```java
@RestController
@RequestMapping("/elections/{electionId}/voters")
public class VoterController {
    
    @PutMapping("/{voterId}")
    public ResponseEntity<?> updateVoter(
            @PathVariable Long electionId,
            @PathVariable Long voterId,
            @RequestBody VoterUpdateRequest request) {
        // Service method will check if election is frozen
        return voterService.updateVoter(voterId, electionId, request);
    }
}
```

### Frontend Protection

#### 1. Add Freeze Status to Election Context/Store

**Example - React Context:**
```typescript
// src/contexts/ElectionContext.tsx
interface ElectionContextType {
  currentElection: Election | null;
  isElectionFrozen: boolean;
  // ...
}

export const ElectionProvider = ({ children }) => {
  const [currentElection, setCurrentElection] = useState<Election | null>(null);
  
  const isElectionFrozen = useMemo(() => 
    currentElection?.isFrozen === true, 
    [currentElection]
  );
  
  return (
    <ElectionContext.Provider value={{ currentElection, isElectionFrozen, ... }}>
      {children}
    </ElectionContext.Provider>
  );
};
```

#### 2. Disable UI Components When Frozen

**Example - Voter Form:**
```typescript
import { useElection } from '@/contexts/ElectionContext';

export const VoterForm = () => {
  const { isElectionFrozen } = useElection();
  
  return (
    <form>
      <input 
        disabled={isElectionFrozen}
        placeholder="Voter Name"
      />
      
      <button 
        disabled={isElectionFrozen}
        onClick={handleSave}
      >
        {isElectionFrozen ? 'Election is Frozen' : 'Save Voter'}
      </button>
      
      {isElectionFrozen && (
        <Alert variant="warning">
          This election is frozen. No modifications allowed.
        </Alert>
      )}
    </form>
  );
};
```

#### 3. Hide Action Buttons When Frozen

```typescript
export const VoterListActions = ({ voter }) => {
  const { isElectionFrozen } = useElection();
  
  return (
    <div>
      <button onClick={handleView}>View</button>
      <button onClick={handleExport}>Export</button>
      
      {!isElectionFrozen && (
        <>
          <button onClick={handleEdit}>Edit</button>
          <button onClick={handleDelete}>Delete</button>
        </>
      )}
    </div>
  );
};
```

#### 4. API Error Handling

```typescript
// src/api/interceptors.ts
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.data?.code === 70247) { // ELECTION_FROZEN
      toast.error('Election is frozen. No modifications allowed.');
      // Optionally refresh election data to sync freeze status
    }
    return Promise.reject(error);
  }
);
```

#### 5. Global Freeze Indicator

```typescript
// Add visual indicator in header/navbar
export const ElectionHeader = () => {
  const { currentElection, isElectionFrozen } = useElection();
  
  return (
    <header>
      <h1>{currentElection?.electionName}</h1>
      {isElectionFrozen && (
        <Badge variant="warning" icon={<LockIcon />}>
          READ-ONLY MODE (Frozen)
        </Badge>
      )}
    </header>
  );
};
```

### Testing Checklist

- [ ] Test all write operations when election is frozen (should return error 70247)
- [ ] Test all read operations when election is frozen (should work normally)
- [ ] Test freeze/unfreeze functionality
- [ ] Verify UI elements are disabled/hidden when frozen
- [ ] Test error handling and user notifications
- [ ] Test that admins can still freeze/unfreeze
- [ ] Verify MongoDB sync includes isFrozen field
- [ ] Test with different user roles (volunteers should also respect freeze)

### Key Files Modified

1. `ElectionFreezeInterceptor.java` - AOP aspect for freeze checks
2. `ThedalError.java` - Added ELECTION_FROZEN error
3. `ElectionMongo.java` - Added isFrozen field
4. `ElectionService.java` - Added isFrozen mapping in DTOs
5. Frontend context/store - Added freeze status
6. UI components - Conditional rendering based on freeze status
