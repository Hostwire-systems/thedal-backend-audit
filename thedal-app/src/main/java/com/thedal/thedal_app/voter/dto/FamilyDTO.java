package com.thedal.thedal_app.voter.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.thedal.thedal_app.voter.VoterEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FamilyDTO {
    private UUID familyId;
    private List<VoterEntity> members;
    private Integer familyCount;
    //private Map<String, List<VoterEntity>> membersByHouseNumber; 
}