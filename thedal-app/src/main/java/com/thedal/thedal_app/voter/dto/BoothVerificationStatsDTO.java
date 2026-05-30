package com.thedal.thedal_app.voter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BoothVerificationStatsDTO {
    private Integer boothNumber;
    private long verifiedCount;
    private long unverifiedCount;
    private long totalCount;
}