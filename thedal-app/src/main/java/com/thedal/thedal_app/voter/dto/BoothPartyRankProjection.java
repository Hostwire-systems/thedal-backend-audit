package com.thedal.thedal_app.voter.dto;

public interface BoothPartyRankProjection {
    Integer getBoothNumber();
    Long getPartyId();
    Integer getPartyRank();
    Long getVoterCount();
}
