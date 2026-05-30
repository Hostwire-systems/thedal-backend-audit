package com.thedal.thedal_app.election.dtos;

import java.util.List;
import org.springframework.data.domain.Page;
import com.thedal.thedal_app.election.PartManager;

public class PartManagerPaginatedResponseDTO {
    private List<PartManagerResponseDTO> partManagers;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    private PartManagerStatsDTO stats;
    
    // Default constructor
    public PartManagerPaginatedResponseDTO() {
    }
    
    // Main constructor
    public PartManagerPaginatedResponseDTO(List<PartManagerResponseDTO> partManagers, 
                                          Page<PartManager> page, 
                                          PartManagerStatsDTO stats) {
        this.partManagers = partManagers;
        this.setPage(page);
        this.stats = stats;
    }
    
    // Helper method
    public void setPage(Page<PartManager> page) {
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.currentPage = page.getNumber();
        this.pageSize = page.getSize();
        this.hasNext = page.hasNext();
        this.hasPrevious = page.hasPrevious();
    }
    
    // All getters and setters
    public List<PartManagerResponseDTO> getPartManagers() { return partManagers; }
    public void setPartManagers(List<PartManagerResponseDTO> partManagers) { this.partManagers = partManagers; }
    
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
    
    public boolean isHasPrevious() { return hasPrevious; }
    public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    
    public PartManagerStatsDTO getStats() { return stats; }
    public void setStats(PartManagerStatsDTO stats) { this.stats = stats; }
}