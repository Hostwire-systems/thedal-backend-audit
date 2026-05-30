package com.thedal.thedal_app.election.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DynamicFieldStatusDTO {

    @NotNull(message = "Status field is mandatory")
    @JsonProperty("status")
    private Boolean status;
}