package com.thedal.thedal_app.voter;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.thedal.thedal_app.election.SurveyDownloadJob;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class SurveyDownloadJobSpecifications {

    public static Specification<SurveyDownloadJob> hasAccountId(Long accountId) {
        return (Root<SurveyDownloadJob> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("accountId"), accountId);
    }

    public static Specification<SurveyDownloadJob> hasElectionId(Long electionId) {
        return (Root<SurveyDownloadJob> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("electionId"), electionId);
    }

    public static Specification<SurveyDownloadJob> hasJobId(Long jobId) {
        return (Root<SurveyDownloadJob> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("id"), jobId);
    }

    public static Specification<SurveyDownloadJob> hasStatus(String status) {
        return (Root<SurveyDownloadJob> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("status"), status);
    }
    public static Specification<SurveyDownloadJob> hasTimeStartedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (Root<SurveyDownloadJob> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate startPredicate = cb.greaterThanOrEqualTo(root.get("timeStarted"), startDate);
            Predicate endPredicate = cb.lessThanOrEqualTo(root.get("timeStarted"), endDate);
            return cb.and(startPredicate, endPredicate);
        };
    }
}