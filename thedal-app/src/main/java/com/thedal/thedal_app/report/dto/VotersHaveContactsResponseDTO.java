package com.thedal.thedal_app.report.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class VotersHaveContactsResponseDTO {
    private Long electionId;

	private Integer boothNumber;
	
	private Integer voterCount;

}
