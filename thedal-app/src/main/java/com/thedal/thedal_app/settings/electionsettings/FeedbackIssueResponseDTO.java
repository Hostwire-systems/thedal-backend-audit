package com.thedal.thedal_app.settings.electionsettings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
//@AllArgsConstructor
public class FeedbackIssueResponseDTO {
    private Long id;
    private String issueName;
    private Integer orderIndex;
    private Long voterCount;
    
    public FeedbackIssueResponseDTO(Long id, String issueName, Integer orderIndex, Long voterCount) {
        this.id = id;
        this.issueName = issueName;
        this.orderIndex = orderIndex;
        this.voterCount = voterCount;
    }
}

