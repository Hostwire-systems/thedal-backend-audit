package com.thedal.thedal_app.election.dtos;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFormSubmissionDTO {

    @NotNull(message = "Submission data is mandatory")
    private Map<String, Object> submissionData;
       
    @Override
    public String toString() {
        return "SurveyFormSubmissionDTO{submissionData=" + submissionData + '}';
    }
    
}