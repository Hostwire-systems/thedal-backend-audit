package com.thedal.thedal_app.report.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ElectionDashboardOverviewResponseDTO {

    private Long electionId;
	
	private int totalBooth;
	
	private int totalVoters;
	
	private int noOfPincode;
	
	private int noOfMobileNumber;
	
	private int male;
	
	private int female;
	
	private int transgender;
	
	private int firstTimeVoters;
	
	private int seniorCitizens;
	
	private int superCitizens;


}
