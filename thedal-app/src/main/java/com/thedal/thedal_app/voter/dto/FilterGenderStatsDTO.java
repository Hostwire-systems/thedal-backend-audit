package com.thedal.thedal_app.voter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilterGenderStatsDTO {
    private String filterName; 
    private String filterValue; 
    private GenderStatsDTO genderStats;
}