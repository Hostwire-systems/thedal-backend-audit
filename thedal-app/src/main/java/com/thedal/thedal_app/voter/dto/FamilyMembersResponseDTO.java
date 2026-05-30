package com.thedal.thedal_app.voter.dto;

import com.thedal.thedal_app.voter.VoterEntity;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.UUID;

public class FamilyMembersResponseDTO {
    private UUID familyId;
    private Integer totalMemberCount;
    private List<VoterEntity> members;
    
    // Pagination metadata
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
    
    public FamilyMembersResponseDTO() {}
    
    // Constructor for non-paginated response (backward compatibility)
    public FamilyMembersResponseDTO(UUID familyId, Integer totalMemberCount, List<VoterEntity> members) {
        this.familyId = familyId;
        this.totalMemberCount = totalMemberCount;
        this.members = members;
        // Set pagination defaults for backward compatibility
        this.currentPage = 0;
        this.pageSize = members.size();
        this.totalElements = members.size();
        this.totalPages = 1;
        this.first = true;
        this.last = true;
        this.hasNext = false;
        this.hasPrevious = false;
    }
    
    // Constructor for paginated response
    public FamilyMembersResponseDTO(UUID familyId, Integer totalMemberCount, Page<VoterEntity> page) {
        this.familyId = familyId;
        this.totalMemberCount = totalMemberCount;
        this.members = page.getContent();
        this.currentPage = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
        this.hasNext = page.hasNext();
        this.hasPrevious = page.hasPrevious();
    }
    
    // Getters and Setters
    public UUID getFamilyId() { return familyId; }
    public void setFamilyId(UUID familyId) { this.familyId = familyId; }
    
    public Integer getTotalMemberCount() { return totalMemberCount; }
    public void setTotalMemberCount(Integer totalMemberCount) { this.totalMemberCount = totalMemberCount; }
    
    public List<VoterEntity> getMembers() { return members; }
    public void setMembers(List<VoterEntity> members) { this.members = members; }
    
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    
    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }
    
    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
    
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
    
    public boolean isHasPrevious() { return hasPrevious; }
    public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    
    // Backward compatibility - deprecated method names
    @Deprecated
    public Integer getMemberCount() { return totalMemberCount; }
    @Deprecated
    public void setMemberCount(Integer memberCount) { this.totalMemberCount = memberCount; }
}
