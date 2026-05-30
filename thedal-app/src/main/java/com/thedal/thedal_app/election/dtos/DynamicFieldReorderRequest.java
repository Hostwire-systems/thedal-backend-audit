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
public class DynamicFieldReorderRequest {

    @NotNull(message = "Field ID is mandatory")
    @JsonProperty("fieldId")
    private Long fieldId;

    @NotNull(message = "New order index is mandatory")
    @JsonProperty("newOrderIndex")
    private Integer newOrderIndex;
}