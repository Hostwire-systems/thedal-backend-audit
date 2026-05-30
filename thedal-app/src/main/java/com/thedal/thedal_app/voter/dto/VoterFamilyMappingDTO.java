package com.thedal.thedal_app.voter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoterFamilyMappingDTO {

    @NotNull(message = "Voter ID cannot be null")
    private String voterId;

    @NotNull(message = "Family ID cannot be null")
    private String familyId;
}
