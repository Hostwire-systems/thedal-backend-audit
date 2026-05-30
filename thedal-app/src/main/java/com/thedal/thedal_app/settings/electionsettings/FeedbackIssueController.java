package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/feedback")
@Slf4j
public class FeedbackIssueController {

    @Autowired
    private RequestDetailsService requestDetails;

    @Autowired
    private FeedbackIssueService feedbackIssueService;

    @PostMapping("/{electionId}")
    public ThedalResponse<FeedbackIssueResponseDTO> createIssue(
        @RequestBody FeedbackIssueDTO issueDTO,
        @PathVariable Long electionId) {

        return feedbackIssueService.createIssue(issueDTO, electionId);
    }

    @GetMapping("/{electionId}")
    public ThedalResponse<List<FeedbackIssueResponseDTO>> getIssues(@PathVariable Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();

        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching feedback issues from PostgreSQL for electionId: {}", electionId);
        return new ThedalResponse<>(ThedalSuccess.ISSUE_FOUND, feedbackIssueService.getIssuesFromPostgreSQL(accountId, electionId));
    }

    @PutMapping("/{electionId}/{issueId}")
    public ThedalResponse<FeedbackIssueResponseDTO> updateIssue(
        @PathVariable Long electionId,
        @PathVariable Long issueId,
        @RequestBody FeedbackIssueDTO issueDTO) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        return new ThedalResponse<>(ThedalSuccess.ISSUE_UPDATED, 
            feedbackIssueService.updateIssue(accountId, electionId, issueId, issueDTO));
    }

    @DeleteMapping("/{electionId}/issues")
    public ThedalResponse<Void> deleteIssues(
        @PathVariable Long electionId,
        @RequestParam(required = false) List<Long> issueIds) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        feedbackIssueService.deleteIssues(accountId, electionId, issueIds);
        return new ThedalResponse<>(ThedalSuccess.ISSUE_DELETED, null);
    }
    @PutMapping("/{electionId}/reorder")
    public ThedalResponse<String> reorderFeedbackIssues(
        @PathVariable Long electionId,
        @RequestBody List<FeedbackIssueReorderRequest> reorderRequests) {

    Long accountId = requestDetails.getCurrentAccountId();
    if (accountId == null) {
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }

    feedbackIssueService.reorderIssues(reorderRequests, accountId, electionId);
    return new ThedalResponse<>(ThedalSuccess.ISSUE_ORDER_UPDATED, "Feedback issues reordered successfully.");
}
    
    @GetMapping("/{electionId}/mongo")
    public ThedalResponse<List<Map<String, Object>>> getIssuesFromMongo(@PathVariable Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        log.info("Fetching feedback issues from MongoDB for electionId: {}", electionId);
        return feedbackIssueService.getIssuesFromMongo(accountId, electionId);
    }

}

