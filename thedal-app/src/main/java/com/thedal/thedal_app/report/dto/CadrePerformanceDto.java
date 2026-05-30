package com.thedal.thedal_app.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CadrePerformanceDto {

	private Long userId; // Changed from volunteerId
    private String userName; // Changed from volunteerName
    private Long totalVoterCreated;
    //private Long rank;
    
    public CadrePerformanceDto(Long userId, Long totalVoterCreated) {
        this.userId = userId;
        this.totalVoterCreated = totalVoterCreated;
    }
	
	
//	private Long volunteerId;
//	
//	private String volunteerName;
//	
//	private Long totalVoterCreated;
//	 // Constructor for repository query
//    public CadrePerformanceDto(Long volunteerId, Long totalVoterCreated) {
//        this.volunteerId = volunteerId;
//        this.totalVoterCreated = totalVoterCreated;
//    }
	
	
}
