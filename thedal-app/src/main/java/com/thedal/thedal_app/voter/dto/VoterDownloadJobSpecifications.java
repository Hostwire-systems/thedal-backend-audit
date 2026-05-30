package com.thedal.thedal_app.voter.dto;


import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.thedal.thedal_app.voter.VoterDownloadJob;

public class VoterDownloadJobSpecifications {
    public static Specification<VoterDownloadJob> hasAccountId(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("accountId"), accountId);
    }
    
    public static Specification<VoterDownloadJob> hasElectionId(Long electionId) {
        return (root, query, cb) -> cb.equal(root.get("electionId"), electionId);
    }
    
    public static Specification<VoterDownloadJob> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
    
    public static Specification<VoterDownloadJob> hasTimeStartedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> cb.between(root.get("timeStarted"), startDate, endDate);
    }
}