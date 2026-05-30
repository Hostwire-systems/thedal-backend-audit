package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ElectionFreezeOtpRepository extends JpaRepository<ElectionFreezeOtp, Long> {

    Optional<ElectionFreezeOtp> findByElectionIdAndOtpAndIsActiveTrue(Long electionId, String otp);

    List<ElectionFreezeOtp> findByElectionIdAndIsActiveTrue(Long electionId);

    @Modifying
    @Transactional
    @Query("UPDATE ElectionFreezeOtp e SET e.isActive = false WHERE e.electionId = :electionId")
    void deactivateAllOtpsForElection(Long electionId);

    @Modifying
    @Transactional
    @Query("UPDATE ElectionFreezeOtp e SET e.isActive = false WHERE e.expiresAt < :now")
    void deactivateExpiredOtps(LocalDateTime now);
}
