package com.thedal.thedal_app.report.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class PollingBasedOnAgeResponseDTO {
    private Long electionId;

	private Integer ageGroup18To30;
	
	private Integer ageGroup30To40;

	private Integer ageGroup40To50;

	private Integer ageGroup50To60;

	private Integer ageGroup60To70;

	private Integer above70;

}
