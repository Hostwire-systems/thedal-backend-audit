package com.thedal.thedal_app.election.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaticFieldStatusDTO {

    @JsonProperty("fieldName")
    private String fieldName;

    @JsonProperty("fieldLabel") 
    private String fieldLabel;

    @JsonProperty("fieldCategory")
    private String fieldCategory;

    @JsonProperty("status")
    private Boolean status;

    @JsonProperty("mandatory")
    private Boolean mandatory;

    public StaticFieldStatusDTO(String fieldName, String fieldLabel, String fieldCategory) {
        this.fieldName = fieldName;
        this.fieldLabel = fieldLabel;
        this.fieldCategory = fieldCategory;
        this.status = true; // Default enabled
        this.mandatory = false; // Default optional
    }
}