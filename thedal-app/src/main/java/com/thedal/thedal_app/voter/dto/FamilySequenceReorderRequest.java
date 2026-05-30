package com.thedal.thedal_app.voter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Data
@Getter
@Setter
public class FamilySequenceReorderRequest {
    
    @NotNull(message = "Family ID is required")
    private UUID familyId;
    
    @NotNull(message = "New sequence number is required")
    private Integer newSequenceNumber;
}
