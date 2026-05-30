package com.thedal.thedal_app.voter.activity.dto;

import java.util.List;

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
public class ElectionActivitySummary {
    
    private Long electionId;
    private List<ActivityTypeSummary> activitySummaries;
    private List<MostActiveVoter> mostActiveVoters;
    private Long totalActivities;
    private Long totalUniqueVoters;
}
