package com.thedal.thedal_app.voter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenderStatsDTO {
    private long maleCount;
    private long femaleCount;
    private long otherCount;
    private long totalCount;
}