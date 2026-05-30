package com.thedal.thedal_app.voter.dto;

public interface BoothGenderStatsProjection {
	Integer getBoothNumber();
    Long getMaleCount();
    Long getFemaleCount();
    Long getOtherCount();
    Long getTotalCount();
}
