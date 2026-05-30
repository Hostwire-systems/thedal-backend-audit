package com.thedal.thedal_app.voter;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface FamilyMappingOtpRepository extends JpaRepository<FamilyMappingOtp, Long> {

    @Query("SELECT o FROM FamilyMappingOtp o WHERE o.electionId = :electionId AND o.mobileNumber = :mobileNumber AND o.isActive = true ORDER BY o.createdAt DESC")
    Optional<FamilyMappingOtp> findLatestActiveByElectionIdAndMobileNumber(@Param("electionId") Long electionId, @Param("mobileNumber") String mobileNumber);
    
    @Modifying
    @Transactional
    @Query("UPDATE FamilyMappingOtp o SET o.isActive = false WHERE o.electionId = :electionId AND o.mobileNumber = :mobileNumber")
    void deactivateAllByElectionIdAndMobileNumber(@Param("electionId") Long electionId, @Param("mobileNumber") String mobileNumber);
}