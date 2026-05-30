package com.thedal.thedal_app.election.dtos;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import com.thedal.thedal_app.election.SurveyFormSubmissionEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFormSubmissionsPageDTO {
    private List<SurveyFormSubmissionResponseDTO> submissions;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
    private int pageSize;

    public SurveyFormSubmissionsPageDTO(Page<SurveyFormSubmissionEntity> submissionsPage) {
        this.submissions = submissionsPage.getContent().stream()
                .map(submission -> new SurveyFormSubmissionResponseDTO(
                        submission.getId(),
                        submission.getSubmissionData(),
                        submission.getSubmittedAt()
                ))
                .collect(Collectors.toList());
        this.totalElements = submissionsPage.getTotalElements();
        this.totalPages = submissionsPage.getTotalPages();
        this.pageNumber = submissionsPage.getNumber();
        this.pageSize = submissionsPage.getSize();
    }
}