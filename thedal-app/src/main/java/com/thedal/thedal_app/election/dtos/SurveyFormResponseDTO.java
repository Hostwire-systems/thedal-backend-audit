package com.thedal.thedal_app.election.dtos;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import com.thedal.thedal_app.election.SurveyFormEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFormResponseDTO {
    private List<SurveyFormDTO> forms;
    private Map<Long, Long> submissionCounts;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
    private int pageSize;

//    public SurveyFormResponseDTO(Page<SurveyFormEntity> formsPage, Map<Long, Long> submissionCounts) {
//        this.forms = formsPage.getContent().stream()
//        		.map(form -> {
//                    SurveyFormDTO dto = new SurveyFormDTO(
//                        form.getId(),
//                        form.getFormName(),
//                        form.getFormDescription(),
//                        form.getCustomFields(),
//                        form.getIsActive(),
//                        form.getOrderIndex(),
//                        form.getCreatedTime(),
//                        form.getModifiedTime()
//                    );
//                    dto.setSubmissionCount(submissionCounts.getOrDefault(form.getId(), 0L));
//                    return dto;
//                })
//                .collect(Collectors.toList());
//        this.submissionCounts = submissionCounts;
//        this.totalElements = formsPage.getTotalElements();
//        this.totalPages = formsPage.getTotalPages();
//        this.pageNumber = formsPage.getNumber();
//        this.pageSize = formsPage.getSize();
//    }
    public SurveyFormResponseDTO(Page<SurveyFormEntity> formsPage, Map<Long, Long> submissionCounts) {
        this.forms = formsPage.getContent().stream()
                .map(entity -> new SurveyFormDTO(
                        entity.getId(),
                        entity.getFormName(),
                        entity.getFormDescription(),
                        entity.getCustomFields(),
                        entity.getIsActive(),
                        entity.getOrderIndex(), // Include orderIndex
                        entity.getCreatedTime(),
                        entity.getModifiedTime(), totalElements
                ))
                .collect(Collectors.toList());
        this.submissionCounts = submissionCounts;
        this.totalElements = formsPage.getTotalElements();
        this.totalPages = formsPage.getTotalPages();
        this.pageNumber = formsPage.getNumber();
        this.pageSize = formsPage.getSize();
    }
    
}