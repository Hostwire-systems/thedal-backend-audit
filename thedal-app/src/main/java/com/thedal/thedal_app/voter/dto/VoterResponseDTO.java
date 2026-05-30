package com.thedal.thedal_app.voter.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterMongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoterResponseDTO {

	private Page<VoterEntity> voters;
	//private Page<VoterMongo> voters1;
	private Page<VoterMongo> votersMongo;
    private GenderStatsDTO genderStats;
    private List<BoothGenderStatsDTO> boothGenderStats;
    private VerificationStatsDTO aadhaarStats;
    private VerificationStatsDTO membershipStats;
    private AddressedVoterStatsDTO addressedVoterStats;
    private List<BoothVerificationStatsDTO> boothAadhaarStats;
    private List<BoothVerificationStatsDTO> boothMembershipStats;
    

    public VoterResponseDTO(Page<VoterEntity> voters, GenderStatsDTO genderStats) {
        this.voters = voters;
        this.genderStats = genderStats;
    }

}