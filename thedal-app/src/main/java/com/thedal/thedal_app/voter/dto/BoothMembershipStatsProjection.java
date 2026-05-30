package com.thedal.thedal_app.voter.dto;

public interface BoothMembershipStatsProjection {
    Integer getBoothNumber();
    Long getVerifiedCount();
    Long getUnverifiedCount();
    Long getTotalCount();
}