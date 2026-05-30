package com.thedal.thedal_app.election.dtos;

/**
 * DTO for PartManager statistics - used for fast dashboard counts
 * without loading full PartManager data
 */
public class PartManagerStatsDTO {
    
    private Long totalCount;
    private Long highVulnerabilityCount;
    private Long mediumVulnerabilityCount;
    private Long lowVulnerabilityCount;
    
    // Default constructor
    public PartManagerStatsDTO() {
    }

    // Main constructor
    public PartManagerStatsDTO(Long totalCount, Long highVulnerabilityCount, 
                              Long mediumVulnerabilityCount, Long lowVulnerabilityCount) {
        this.totalCount = totalCount != null ? totalCount : 0L;
        this.highVulnerabilityCount = highVulnerabilityCount != null ? highVulnerabilityCount : 0L;
        this.mediumVulnerabilityCount = mediumVulnerabilityCount != null ? mediumVulnerabilityCount : 0L;
        this.lowVulnerabilityCount = lowVulnerabilityCount != null ? lowVulnerabilityCount : 0L;
    }
    
    // Getters and Setters
    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getHighVulnerabilityCount() {
        return highVulnerabilityCount;
    }

    public void setHighVulnerabilityCount(Long highVulnerabilityCount) {
        this.highVulnerabilityCount = highVulnerabilityCount;
    }

    public Long getMediumVulnerabilityCount() {
        return mediumVulnerabilityCount;
    }

    public void setMediumVulnerabilityCount(Long mediumVulnerabilityCount) {
        this.mediumVulnerabilityCount = mediumVulnerabilityCount;
    }

    public Long getLowVulnerabilityCount() {
        return lowVulnerabilityCount;
    }

    public void setLowVulnerabilityCount(Long lowVulnerabilityCount) {
        this.lowVulnerabilityCount = lowVulnerabilityCount;
    }
}