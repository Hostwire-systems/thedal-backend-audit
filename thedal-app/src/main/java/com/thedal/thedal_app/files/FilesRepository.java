package com.thedal.thedal_app.files;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FilesRepository extends JpaRepository<Files, Long> {

	List<Files> findAllByHandlerTypeAndHandlerFileId(HandlerType bannerImages, Long electionId);

//	@Query("SELECT MAX(f.orderIndex) FROM Files f WHERE f.handlerFileId = :electionId AND f.handlerType = 'BANNER_IMAGES'")
//	Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);

	@Query("SELECT MAX(f.orderIndex) FROM Files f")
	Integer findMaxOrderIndex();

	List<Files> findByHandlerFileIdAndHandlerTypeOrderByOrderIndex(Long handlerFileId, HandlerType handlerType);

	List<Files> findAllByHandlerTypeAndHandlerFileIdOrderByOrderIndexAsc(HandlerType bannerImages, Long electionId);

	Optional<Files> findByIdAndHandlerTypeAndHandlerFileId(Long id, HandlerType handlerType, Long handlerFileId);
	List<Files> findAllByIdInAndHandlerTypeAndHandlerFileId(List<Long> ids, HandlerType handlerType, Long handlerFileId);

	@Modifying
    @Query("UPDATE Files f SET f.whatsappForward = false WHERE f.handlerFileId = :electionId AND f.handlerType = 'BANNER_IMAGES'")
    void resetWhatsappForwardForElection(@Param("electionId") Long electionId);

    @Query("SELECT MAX(f.orderIndex) FROM Files f WHERE f.handlerFileId = :electionId AND f.handlerType = 'BANNER_IMAGES'")
    Integer findMaxOrderIndexByElectionId(@Param("electionId") Long electionId);
    
    // Find files by account ID through bulk upload relationship
    @Query("SELECT f FROM Files f WHERE f.bulkUpload.accountId = :accountId")
    Page<Files> findByBulkUploadAccountId(@Param("accountId") Long accountId, Pageable pageable);
    
    // Find files by account ID through bulk upload member relationship
    @Query("SELECT f FROM Files f WHERE f.bulkUploadMember.accountId = :accountId")
    Page<Files> findByBulkUploadMemberAccountId(@Param("accountId") Long accountId, Pageable pageable);
}
