package com.thedal.thedal_app.election.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateSurveyFormStatusRequest {
    @NotNull(message = "isActive is mandatory")
    private Boolean isActive;
}