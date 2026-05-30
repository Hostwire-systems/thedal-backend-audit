package com.thedal.thedal_app.familycaptain.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class FamilyUpdateRequest {

    @NotEmpty(message = "At least one family must be assigned")
    @JsonProperty("assigned_families")
    private List<UUID> assignedFamilies;
}
