package com.thedal.thedal_app.election;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

public class SurveySpecifications {
    public static Specification<SurveyFormSubmissionEntity> hasElectionId(Long electionId) {
        return (root, query, cb) -> cb.equal(root.get("electionId"), electionId);
    }

    public static Specification<SurveyFormSubmissionEntity> hasAccountId(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("accountId"), accountId);
    }

    public static Specification<SurveyFormSubmissionEntity> hasFormId(Long formId) {
        return (root, query, cb) -> cb.equal(root.get("formId"), formId);
    }

//    public static Specification<SurveyFormSubmissionEntity> hasVoterIds(List<String> voterIds) {
//        return (root, query, cb) -> root.get("voterId").in(voterIds);
//    }
}