package com.thedal.thedal_app.voter.dto;

public interface MembershipStatsProjection {
    Long getVerifiedCount();
    Long getUnverifiedCount();
    Long getTotalCount();
}