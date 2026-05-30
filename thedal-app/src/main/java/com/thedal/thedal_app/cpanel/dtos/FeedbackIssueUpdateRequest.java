package com.thedal.thedal_app.cpanel.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FeedbackIssueUpdateRequest {
    private Long issueId;
    private String issueName;
}