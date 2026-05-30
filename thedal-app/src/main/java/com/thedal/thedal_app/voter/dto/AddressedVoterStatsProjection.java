package com.thedal.thedal_app.voter.dto;

public interface AddressedVoterStatsProjection {
    Long getAddressedCount();
    Long getNotAddressedCount();
    Long getTotalCount();
}
