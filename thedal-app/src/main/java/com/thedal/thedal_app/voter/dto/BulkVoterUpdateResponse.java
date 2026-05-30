package com.thedal.thedal_app.voter.dto;

import java.util.List;

import com.thedal.thedal_app.voter.VoterEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class BulkVoterUpdateResponse {
	
//	private List<VoterEntity> updatedVoters; 
//    private List<String> updatedVotersIds; 
//    private List<String> alreadyVotedVotersIds; 
//    private List<String> notFoundVotersIds;  
//    private int totalVoters; 
//    private int alreadyVotedCount;
//    private int updatedVotersCount;

	private List<VoterEntity> updatedVoters;
    private List<String> updatedVotersIds;
    private List<String> alreadyVotedVotersIds;
    private List<String> notFoundVotersIds;
    private int totalRequests;
    private int alreadyVotedCount;
    private int updatedVotersCount;

    // Constructor, getters, setters
    public BulkVoterUpdateResponse(
            List<VoterEntity> updatedVoters, 
            List<String> updatedVotersIds,
            List<String> alreadyVotedVotersIds, 
            List<String> notFoundVotersIds, 
            int totalRequests,
            int alreadyVotedCount, 
            int updatedVotersCount) {
        this.updatedVoters = updatedVoters;
        this.updatedVotersIds = updatedVotersIds;
        this.alreadyVotedVotersIds = alreadyVotedVotersIds;
        this.notFoundVotersIds = notFoundVotersIds;
        this.totalRequests = totalRequests;
        this.alreadyVotedCount = alreadyVotedCount;
        this.updatedVotersCount = updatedVotersCount;
    }

    
}
