package com.thedal.thedal_app.voter;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

public class VoterSpecifications {
    public static Specification<VoterEntity> hasElectionId(Long electionId) {
        return (root, query, cb) -> cb.equal(root.get("electionId"), electionId);
    }

    public static Specification<VoterEntity> hasAccountId(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("accountId"), accountId);
    }

//    public static Specification<VoterEntity> hasPartNo(Integer partNo) {
//        return (root, query, cb) -> cb.equal(root.get("partNo"), partNo);
//    }
    public static Specification<VoterEntity> hasPartNos(List<Integer> partNos) {
        return (root, query, cb) -> root.get("partNo").in(partNos);
    }

    public static Specification<VoterEntity> hasGender(String gender) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("gender")), gender.toLowerCase());
    }

    public static Specification<VoterEntity> hasMinAge(Integer minAge) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("age"), minAge);
    }

    public static Specification<VoterEntity> hasMaxAge(Integer maxAge) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("age"), maxAge);
    }

    // Additional filters for campaign estimates
    public static Specification<VoterEntity> hasReligionIds(List<Long> religionIds) {
        return (root, query, cb) -> root.get("religion").get("id").in(religionIds);
    }

    public static Specification<VoterEntity> hasCasteIds(List<Long> casteIds) {
        return (root, query, cb) -> root.get("caste").get("id").in(casteIds);
    }

    public static Specification<VoterEntity> hasSubCasteIds(List<Long> subCasteIds) {
        return (root, query, cb) -> root.get("subCaste").get("id").in(subCasteIds);
    }

    public static Specification<VoterEntity> hasCasteCategoryIds(List<Long> casteCategoryIds) {
        return (root, query, cb) -> root.get("casteCategory").get("id").in(casteCategoryIds);
    }

    public static Specification<VoterEntity> hasAvailabilityIds(List<Long> availabilityIds) {
        return (root, query, cb) -> root.get("availability1").get("id").in(availabilityIds);
    }

    public static Specification<VoterEntity> hasPartyIds(List<Long> partyIds) {
        return (root, query, cb) -> root.get("party").get("id").in(partyIds);
    }

    public static Specification<VoterEntity> hasAadhaarVerified(Boolean value) {
        return (root, query, cb) -> cb.equal(root.get("aadhaarVerified"), value);
    }

    public static Specification<VoterEntity> hasMemberVerified(Boolean value) {
        return (root, query, cb) -> cb.equal(root.get("memberVerified"), value);
    }

    public static Specification<VoterEntity> hasPollStatus(List<String> pollStatus) {
        return (root, query, cb) -> {
            if (pollStatus == null || pollStatus.isEmpty()) {
                // Empty list means "all" - no filter
                return cb.conjunction();
            }
            
            boolean hasVoted = pollStatus.stream().anyMatch(s -> "voted".equalsIgnoreCase(s));
            boolean hasNotVoted = pollStatus.stream().anyMatch(s -> "notVoted".equalsIgnoreCase(s) || "not-voted".equalsIgnoreCase(s));
            
            if (hasVoted && hasNotVoted) {
                // Both selected means "all" - no filter
                return cb.conjunction();
            } else if (hasVoted) {
                // Only voted
                return cb.equal(root.get("hasVoted"), true);
            } else if (hasNotVoted) {
                // Only not voted
                return cb.or(
                    cb.equal(root.get("hasVoted"), false),
                    cb.isNull(root.get("hasVoted"))
                );
            }
            
            // If pollStatus contains only unrecognized values, return no filter
            return cb.conjunction();
        };
    }
}