package com.thedal.thedal_app.report.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoothStrengthOverviewDTO {
    
    private Long electionId;
    private Long defaultPartyId;
    private String defaultPartyName;
    private List<Integer> strongBooths;
    private List<Integer> swingBooths;
    private List<Integer> weakBooths;
}
