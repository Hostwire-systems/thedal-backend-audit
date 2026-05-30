package com.thedal.thedal_app.voter;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BulkPhotoUploadRepository extends JpaRepository<BulkPhotoUploadEntity, Long> {
    
    @Query("SELECT b FROM BulkPhotoUploadEntity b WHERE b.accountId = :accountId AND b.electionId = :electionId ORDER BY b.startTime DESC")
    Page<BulkPhotoUploadEntity> findByAccountIdAndElectionIdOrderByStartTimeDesc(
        @Param("accountId") Long accountId, 
        @Param("electionId") Long electionId, 
        Pageable pageable);
    
    @Query("SELECT b FROM BulkPhotoUploadEntity b WHERE b.accountId = :accountId ORDER BY b.startTime DESC")
    Page<BulkPhotoUploadEntity> findByAccountIdOrderByStartTimeDesc(
        @Param("accountId") Long accountId, 
        Pageable pageable);
    
    List<BulkPhotoUploadEntity> findByStatusAndAccountIdAndElectionId(
        BulkUploadStatus status, Long accountId, Long electionId);
}
