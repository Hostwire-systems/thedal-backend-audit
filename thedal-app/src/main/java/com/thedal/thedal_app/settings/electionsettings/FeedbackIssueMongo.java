package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "feedback_issues")
@CompoundIndexes({
    @CompoundIndex(name = "accountId_electionId_idx", def = "{'accountId': 1, 'electionId': 1}"),
    @CompoundIndex(name = "accountId_electionId_orderIndex_idx", def = "{'accountId': 1, 'electionId': 1, 'orderIndex': 1}"),
    @CompoundIndex(name = "electionId_issueName_idx", def = "{'electionId': 1, 'issueName': 1}", unique = true)
})
public class FeedbackIssueMongo {

    @Id
    private Long id;

    @Indexed
    private String issueName;

    private Long electionId;

    private Long accountId;

    private Integer orderIndex;

    private LocalDateTime createdAt;

    // Constructor to map from FeedbackIssue
    public FeedbackIssueMongo(FeedbackIssue issue) {
        this.id = issue.getId();
        this.issueName = issue.getIssueName();
        this.electionId = issue.getElectionId();
        this.accountId = issue.getAccountId();
        this.orderIndex = issue.getOrderIndex();
        this.createdAt = issue.getCreatedAt();
    }
}