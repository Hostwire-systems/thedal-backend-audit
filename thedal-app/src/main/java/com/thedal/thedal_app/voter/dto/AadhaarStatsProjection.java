package com.thedal.thedal_app.voter.dto;

public interface AadhaarStatsProjection {
    Long getVerifiedCount();
    Long getUnverifiedCount();
    Long getTotalCount();
}