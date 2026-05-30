package com.thedal.thedal_app.voter.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public class FamilySummaryResponseDTO {
    private Page<FamilySummaryDTO> families;
    private GenderStatsDTO genderStats;
    private FamilyMappingStatsDTO familyMappingStats;
    private Long totalVotersCount; 
    
    public FamilySummaryResponseDTO() {}
    
    public FamilySummaryResponseDTO(Page<FamilySummaryDTO> families, GenderStatsDTO genderStats, Long totalVotersCount) {
        this.families = families;
        this.genderStats = genderStats;
        this.totalVotersCount=totalVotersCount;
    }
    
    public FamilySummaryResponseDTO(Page<FamilySummaryDTO> families, GenderStatsDTO genderStats, 
                                   FamilyMappingStatsDTO familyMappingStats, Long totalVotersCount) {
        this.families = families;
        this.genderStats = genderStats;
        this.familyMappingStats = familyMappingStats;
        this.totalVotersCount = totalVotersCount;
    }
    
    // Getters and Setters
    public Page<FamilySummaryDTO> getFamilies() { return families; }
    public void setFamilies(Page<FamilySummaryDTO> families) { this.families = families; }
    
    public GenderStatsDTO getGenderStats() { return genderStats; }
    public void setGenderStats(GenderStatsDTO genderStats) { this.genderStats = genderStats; }
    
    public FamilyMappingStatsDTO getFamilyMappingStats() { return familyMappingStats; }
    public void setFamilyMappingStats(FamilyMappingStatsDTO familyMappingStats) { 
        this.familyMappingStats = familyMappingStats; 
    }
    
    public Long getTotalVotersCount() { return totalVotersCount; }
    public void setTotalVotersCount(Long totalVotersCount) { this.totalVotersCount = totalVotersCount; }

    
}
