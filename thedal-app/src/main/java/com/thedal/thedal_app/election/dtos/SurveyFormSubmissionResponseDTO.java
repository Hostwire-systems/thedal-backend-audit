package com.thedal.thedal_app.election.dtos;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFormSubmissionResponseDTO {
    private Long id;
    private Map<String, Object> submissionData;
    private LocalDateTime submittedAt;
}