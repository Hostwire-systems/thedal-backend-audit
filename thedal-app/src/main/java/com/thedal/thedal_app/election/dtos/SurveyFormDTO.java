package com.thedal.thedal_app.election.dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFormDTO {

    private Long id;

    @NotBlank(message = "Form name is mandatory")
    @JsonProperty("formName")
    private String formName;

    @JsonProperty("formDescription")
    private String formDescription;
    
    @NotNull(message = "Custom fields are mandatory")
    @JsonProperty("customFields")
    private List<Map<String, Object>> customFields;

    @NotNull(message = "Active status is mandatory")
    @JsonProperty("isActive")
    private Boolean isActive;
    
    @JsonProperty("orderIndex")
    private Integer orderIndex;

    @JsonIgnore
    private LocalDateTime createdTime;
    @JsonIgnore
    private LocalDateTime modifiedTime;
    @JsonIgnore
    private Long submissionCount; 
    
    public SurveyFormDTO(Long id, String formName, String formDescription, 
            List<Map<String, Object>> customFields, Boolean isActive,
            LocalDateTime createdTime, LocalDateTime modifiedTime) {
     this.id = id;
     this.formName = formName;
     this.formDescription = formDescription;
     this.customFields = customFields;
     this.isActive = isActive;
     this.createdTime = createdTime;
     this.modifiedTime = modifiedTime;
     this.submissionCount = 0L; // Default to 0, updated later
}
    
}