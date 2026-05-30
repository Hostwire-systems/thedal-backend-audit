package com.thedal.thedal_app.settings.electionsettings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackIssueReorderRequest {
    private Long issueId;
    private int newOrderIndex;
}