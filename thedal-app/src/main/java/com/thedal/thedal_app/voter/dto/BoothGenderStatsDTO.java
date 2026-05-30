package com.thedal.thedal_app.voter.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoothGenderStatsDTO {
	
	private Integer boothNumber;
    private Long maleCount;
    private Long femaleCount;
    private Long otherCount;
    private Long totalCount;

    public BoothGenderStatsDTO(Integer boothNumber, Long maleCount, Long femaleCount, 
                              Long otherCount, Long totalCount) {
        this.boothNumber = boothNumber;
        this.maleCount = maleCount;
        this.femaleCount = femaleCount;
        this.otherCount = otherCount;
        this.totalCount = totalCount;
    }

}
