package com.thedal.thedal_app.familycaptain.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FamilyDetailsDTO {

    @JsonProperty("family_id")
    private UUID familyId;

    @JsonProperty("family_sequence_number")
    private Integer familySequenceNumber;

    @JsonProperty("family_head_name")
    private String familyHeadName;

    @JsonProperty("family_head_epic")
    private String familyHeadEpic;

    @JsonProperty("family_count")
    private Integer familyCount;

    @JsonProperty("part_number")
    private Integer partNumber;
    
    // Additional properties for family options dropdown
    @JsonProperty("family_no")
    private Long familyNo;
    
    @JsonProperty("head_name") 
    private String headName;
    
    @JsonProperty("display_text")
    private String displayText;
}
