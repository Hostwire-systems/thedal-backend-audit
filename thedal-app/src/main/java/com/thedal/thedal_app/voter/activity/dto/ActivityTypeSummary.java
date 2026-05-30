package com.thedal.thedal_app.voter.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTypeSummary {
    
    private String activityType;
    private Long totalCount;
    private Long uniqueVoters;
}
