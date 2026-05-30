package com.thedal.thedal_app.voter;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
class BulkVoterUpdateResponses {
	
//	private List<VoterEntity> updatedVoters; 
//    private int totalVoters; 
//    private int alreadyVotedCount; 
//    private int updatedVotersCount;
	
	//private List<VoterEntityMongo> updatedVoters; 
    private List<String> updatedVotersIds; 
    private List<String> alreadyVotedVotersIds; 
    private List<String> notFoundVotersIds;  
    private int totalVoters; 
    private int alreadyVotedCount;
    private int updatedVotersCount;

}
