package com.thedal.thedal_app.election.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SetDefaultPartyRequest {
    
    @NotNull(message = "Party ID is required")
    private Long partyId;
}
