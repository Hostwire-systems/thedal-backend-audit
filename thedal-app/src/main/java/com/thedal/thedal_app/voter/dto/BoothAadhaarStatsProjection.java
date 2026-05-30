package com.thedal.thedal_app.voter.dto;

public interface BoothAadhaarStatsProjection {
    Integer getBoothNumber();
    Long getVerifiedCount();
    Long getUnverifiedCount();
    Long getTotalCount();
}