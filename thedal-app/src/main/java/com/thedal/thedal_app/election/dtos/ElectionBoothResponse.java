package com.thedal.thedal_app.election.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ElectionBoothResponse {
//    private Long electionId;
//    private Long boothId;
    private Integer boothNumber;
//    @JsonIgnore
//    private Long accountId;
    private String boothVulnerability;
    private Integer orderIndex;
}