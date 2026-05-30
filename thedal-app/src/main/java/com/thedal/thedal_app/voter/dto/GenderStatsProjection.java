package com.thedal.thedal_app.voter.dto;

public interface GenderStatsProjection {
    Long getMaleCount();
    Long getFemaleCount();
    Long getOtherCount();
    Long getTotalCount();
	//Integer getBoothNumber();
}
