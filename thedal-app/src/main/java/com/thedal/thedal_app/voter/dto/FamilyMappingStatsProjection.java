package com.thedal.thedal_app.voter.dto;

public interface FamilyMappingStatsProjection {
    Long getUnmappedVoterCount();
    Long getSingleVoterFamilyCount();
    Long getTotalCount();
}
