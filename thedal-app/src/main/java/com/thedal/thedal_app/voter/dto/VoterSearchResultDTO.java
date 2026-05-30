package com.thedal.thedal_app.voter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoterSearchResultDTO {
    private String voterId;
    private String voterFnameEn;
    private String voterFnameL1;
    private String voterFnameL2;
    private String voterLnameEn;
    private String epicNumber;
    private String rlnFnameEn;
    private String rlnLnameEn;
}