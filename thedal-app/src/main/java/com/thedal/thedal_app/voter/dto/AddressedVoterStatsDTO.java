package com.thedal.thedal_app.voter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressedVoterStatsDTO {
    private long addressedCount;      // Voters with availability_id NOT NULL
    private long notAddressedCount;   // Voters with availability_id IS NULL
    private long totalCount;
}
