package com.thedal.thedal_app.cpanel.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FeedbackIssueResponseDTO {
    private Long id;
    private String issueName;
    private Integer orderIndex;

    public FeedbackIssueResponseDTO(Long id, String issueName, Integer orderIndex) {
        this.id = id;
        this.issueName = issueName;
        this.orderIndex = orderIndex;
    }
}