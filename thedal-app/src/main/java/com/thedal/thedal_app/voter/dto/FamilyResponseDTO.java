package com.thedal.thedal_app.voter.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FamilyResponseDTO {
    private Page<FamilyDTO> families;
    private GenderStatsDTO genderStats;
    private List<BoothGenderStatsDTO> boothGenderStats;
    private VerificationStatsDTO aadhaarStats;
    private VerificationStatsDTO membershipStats;
    private List<BoothVerificationStatsDTO> boothAadhaarStats;
    private List<BoothVerificationStatsDTO> boothMembershipStats;
    //private boolean isFamilyGrouped;

    public FamilyResponseDTO(Page<FamilyDTO> families, GenderStatsDTO genderStats) {
        this.families = families;
        this.genderStats = genderStats;
        //this.isFamilyGrouped = false;
    }
}