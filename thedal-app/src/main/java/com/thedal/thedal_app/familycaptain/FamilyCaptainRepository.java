package com.thedal.thedal_app.familycaptain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyCaptainRepository extends JpaRepository<FamilyCaptainEntity, Long> {

    // Find by user and election
    Optional<FamilyCaptainEntity> findByUserEntity_IdAndElectionEntity_IdAndAccountId(Long userId, Long electionId, Long accountId);
    
    // Find by election and account
    List<FamilyCaptainEntity> findByElectionEntity_IdAndAccountId(Long electionId, Long accountId);
    
    // Find by mobile number and election
    List<FamilyCaptainEntity> findByMobileNumberAndElectionEntity_IdAndAccountId(String mobileNumber, Long electionId, Long accountId);
    
    // Check if user is already a family captain in this election
    boolean existsByUserEntity_IdAndElectionEntity_IdAndAccountId(Long userId, Long electionId, Long accountId);
    
    // Delete by user and election
    @Modifying
    @Query("DELETE FROM FamilyCaptainEntity fc WHERE fc.userEntity.id = :userId AND fc.electionEntity.id = :electionId AND fc.accountId = :accountId")
    void deleteByUserIdAndElectionIdAndAccountId(@Param("userId") Long userId, @Param("electionId") Long electionId, @Param("accountId") Long accountId);
    
    // Delete multiple family captains
    @Modifying
    @Query("DELETE FROM FamilyCaptainEntity fc WHERE fc.userEntity.id IN :userIds AND fc.electionEntity.id = :electionId AND fc.accountId = :accountId")
    void deleteByUserIdsAndElectionIdAndAccountId(@Param("userIds") List<Long> userIds, @Param("electionId") Long electionId, @Param("accountId") Long accountId);
    
    // Find with assigned families filter
    @Query("SELECT fc FROM FamilyCaptainEntity fc WHERE fc.electionEntity.id = :electionId AND fc.accountId = :accountId " +
           "AND (:assignedFamilies IS NULL OR EXISTS (SELECT af FROM fc.assignedFamilies af WHERE af IN :assignedFamilies)) " +
           "AND (:mobileNumber IS NULL OR CAST(fc.mobileNumber AS string) LIKE CONCAT('%', CAST(:mobileNumber AS string), '%')) " +
           "AND (:searchTerm IS NULL OR LOWER(CONCAT(CAST(fc.firstName AS string), ' ', CAST(fc.lastName AS string))) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%')) " +
           "     OR CAST(fc.mobileNumber AS string) LIKE CONCAT('%', CAST(:searchTerm AS string), '%'))")
    Page<FamilyCaptainEntity> findWithFilters(@Param("electionId") Long electionId, 
                                            @Param("accountId") Long accountId,
                                            @Param("assignedFamilies") List<UUID> assignedFamilies,
                                            @Param("mobileNumber") String mobileNumber,
                                            @Param("searchTerm") String searchTerm,
                                            Pageable pageable);
    
    // Count family captains in election
    long countByElectionEntity_IdAndAccountId(Long electionId, Long accountId);
    
    // Find family captains by assigned family ID
    @Query("SELECT fc FROM FamilyCaptainEntity fc JOIN fc.assignedFamilies af WHERE af = :familyId AND fc.electionEntity.id = :electionId AND fc.accountId = :accountId")
    List<FamilyCaptainEntity> findByAssignedFamilyId(@Param("familyId") UUID familyId, @Param("electionId") Long electionId, @Param("accountId") Long accountId);
}
