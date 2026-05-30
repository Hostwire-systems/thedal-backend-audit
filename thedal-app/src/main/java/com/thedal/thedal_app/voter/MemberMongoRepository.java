package com.thedal.thedal_app.voter;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberMongoRepository extends MongoRepository<MemberMongo, String> {
    
    // Find by account and election
    List<MemberMongo> findByAccountIdAndElectionId(Long accountId, Long electionId);
    
    // Find by membership number
    List<MemberMongo> findByMembershipNoAndAccountIdAndElectionId(String membershipNo, Long accountId, Long electionId);
    
    // Find by epic number
    List<MemberMongo> findByEpicNumberAndAccountIdAndElectionId(String epicNumber, Long accountId, Long electionId);
    
    // Find by mobile number
    List<MemberMongo> findByMobileNumberAndAccountIdAndElectionId(String mobileNumber, Long accountId, Long electionId);
    
    // Find by both membership number and epic number
    List<MemberMongo> findByMembershipNoAndEpicNumberAndAccountIdAndElectionId(
        String membershipNo, String epicNumber, Long accountId, Long electionId);
    
    // Find by member ID (PostgreSQL reference)
    MemberMongo findByMemberId(Long memberId);
    
    // Find by multiple member IDs
    List<MemberMongo> findByMemberIdIn(List<Long> memberIds);
    
    // Delete by account and election
    void deleteByAccountIdAndElectionId(Long accountId, Long electionId);
    
    // Delete by member IDs
    void deleteByMemberIdIn(List<Long> memberIds);
    
    // Delete by single member ID
    void deleteByMemberId(Long memberId);
    
    // Count by account and election
    long countByAccountIdAndElectionId(Long accountId, Long electionId);
    
    // Custom query to search members with filters
    @Query("{ 'accountId': ?0, 'electionId': ?1, " +
           "$or: [ " +
           "  { 'memberName': { $regex: ?2, $options: 'i' } }, " +
           "  { 'membershipNo': { $regex: ?2, $options: 'i' } }, " +
           "  { 'epicNumber': { $regex: ?2, $options: 'i' } }, " +
           "  { 'mobileNumber': { $regex: ?2, $options: 'i' } } " +
           "] }")
    List<MemberMongo> searchMembers(Long accountId, Long electionId, String searchTerm);
}
