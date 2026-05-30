package com.thedal.thedal_app.volunteer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.user.UserEntity;

@Repository
public interface VolunteerRepository extends JpaRepository<VolunteerEntity, Long> {
	// Role name filtering variant for paginated data - Using user's role instead of volunteer's role_id
	@Query(value = "SELECT v FROM VolunteerEntity v " +
		   "JOIN v.electionEntity e " +
		   "JOIN v.userEntity u " +
		   "LEFT JOIN u.role r " +
		   "WHERE e.id = :electionId " +
		   "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
		   "AND (:userId IS NULL OR u.id = :userId) " +
		   "AND (:roleName IS NULL OR " +
		   "     (r.roleName IS NOT NULL AND (" +
		   "       UPPER(r.roleName) = UPPER(:roleName) OR " +
		   "       UPPER(r.roleName) = UPPER(REPLACE(:roleName, ' ', '_')) OR " +
		   "       UPPER(r.roleName) = UPPER(REPLACE(:roleName, '_', ' '))" +
		   "     ))" +
		   ") " +
		   "AND (:assignedBooths IS NULL OR EXISTS (" +
		   "    SELECT 1 FROM VolunteerEntity v2 " +
		   "    JOIN v2.assignedBooth ab " +
		   "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
		   "))")
	Page<VolunteerEntity> findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserIdAndRoleName(
		@Param("electionId") Long electionId,
		@Param("assignedBooths") List<Long> assignedBooths,
		@Param("mobileNumber") String mobileNumber,
		@Param("userId") Long userId,
		@Param("roleName") String roleName,
		Pageable pageable);

	// Role name filtering with search - Using user's role instead of volunteer's role_id
	@Query(value = "SELECT v FROM VolunteerEntity v " +
		   "JOIN v.electionEntity e " +
		   "JOIN v.userEntity u " +
		   "LEFT JOIN u.role r " +
		   "WHERE e.id = :electionId " +
		   "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
		   "AND (:userId IS NULL OR u.id = :userId) " +
		   "AND (:roleName IS NULL OR " +
		   "     (r.roleName IS NOT NULL AND (" +
		   "       UPPER(r.roleName) = UPPER(:roleName) OR " +
		   "       UPPER(r.roleName) = UPPER(REPLACE(:roleName, ' ', '_')) OR " +
		   "       UPPER(r.roleName) = UPPER(REPLACE(:roleName, '_', ' '))" +
		   "     ))" +
		   ") " +
		   "AND (:assignedBooths IS NULL OR EXISTS (" +
		   "    SELECT 1 FROM VolunteerEntity v2 " +
		   "    JOIN v2.assignedBooth ab " +
		   "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
		   ")) " +
		   "AND (:searchTerm IS NULL OR " +
		   "     UPPER(u.firstName) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(u.lastName) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(CONCAT(u.firstName, ' ', u.lastName)) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(CONCAT(u.lastName, ' ', u.firstName)) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR u.mobileNumber LIKE CONCAT('%', :searchTerm, '%') " +
		   "     OR v.mobileNumber LIKE CONCAT('%', :searchTerm, '%'))")
	Page<VolunteerEntity> findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserIdAndRoleNameWithSearch(
		@Param("electionId") Long electionId,
		@Param("assignedBooths") List<Long> assignedBooths,
		@Param("mobileNumber") String mobileNumber,
		@Param("userId") Long userId,
		@Param("roleName") String roleName,
		@Param("searchTerm") String searchTerm,
		Pageable pageable);

	//Optional<VolunteerEntity> findByVolunteerId(Long volunteerId);

	List<VolunteerEntity> findByAccountId(Long accountId);

	Page<VolunteerEntity> findByAccountId(Long accountId, Pageable pageable);

	Page<VolunteerEntity> findByAccountIdAndElectionEntity(Long accountId, ElectionEntity election, Pageable pageable);

	Optional<VolunteerEntity> findByUserEntity_Id(Long volunteerUserId);

	boolean existsByUserEntityAndElectionEntity(UserEntity userEntity, ElectionEntity election);

	Optional<VolunteerEntity> findByIdAndUserEntity_Id(Long volunteerId, Long currentUserId);

	int countByElectionEntity_Id(Long electionId);

	//Optional<VolunteerEntity> findByAssignedBoothAndUserEntity_MobileNumber(String boothNumber, String mobileNumber);

	Optional<VolunteerEntity> findByIdAndElectionEntity(Long volunteerId, ElectionEntity electionEntity);

//	Optional<VolunteerEntity> findByAssignedBoothAndUserEntity_MobileNumberAndElectionEntity(String boothNumber,
//			String mobileNumber, ElectionEntity election);

	Optional<VolunteerEntity> findByElectionEntityAndUserEntity_Id(ElectionEntity election, Long currentUserId);

	//Optional<VolunteerEntity> findByUserEntityIdAndElectionEntityId(Long userId, Long electionId);

	Optional<VolunteerEntity> findByIdAndUserEntityIdAndElectionEntityId(Long volunteerId, Long userId,
			Long electionId);

	Page<VolunteerEntity> findByAccountIdAndUserEntityIdAndElectionEntity(Long accountId, Long userId,
			ElectionEntity election, Pageable pageable);

	// Role filtering variant for location data
	Page<VolunteerEntity> findByAccountIdAndUserEntityIdAndElectionEntityAndRoleId(Long accountId, Long userId,
			ElectionEntity election, Long roleId, Pageable pageable);

	Optional<VolunteerEntity> findByUserEntityIdAndElectionEntityId(Long userId, Long electionId);
//	@Query("SELECT v FROM VolunteerEntity v WHERE v.userEntity.id = :userId AND v.electionEntity.id = :electionId")
//	Optional<VolunteerEntity> findByUserIdAndElectionId(@Param("userId") Long userId, @Param("electionId") Long electionId);

 
	
	Optional<VolunteerEntity> findByUserEntityIdAndElectionEntityIdAndUserEntityEmail(Long userId, Long electionId,
			String newEmail);
	Optional<VolunteerEntity> findByUserEntityIdAndElectionEntityIdAndAccountId(Long userId, Long electionId,
			Long accountId);

	Optional<VolunteerEntity> findByUserEntityIdAndElectionEntityIdAndUserEntityMobileNumber(Long userId,
			Long electionId, String newMobile);

	//Optional<VolunteerEntity> findByUserEntityIdAndEmail(Long userId, String newEmail);

	//Optional<VolunteerEntity> findByUserEntityIdAndMobileNumber(Long userId, String newMobile);	


//	@Query("SELECT v FROM VolunteerEntity v " + 
//		       "WHERE v.electionEntity.id = :electionId " +
//		       "AND (:mobileNumber IS NULL OR v.userEntity.mobileNumber = :mobileNumber) " +
//		       "AND (:userId IS NULL OR v.userEntity.id = :userId)")
//		List<VolunteerEntity> findVolunteerByElectionIdAndMobileNumberAndUserId(
//		    @Param("electionId") Long electionId,
//		    @Param("mobileNumber") String mobileNumber,
//		    @Param("userId") Long userId);


	@Query("SELECT v.userEntity.id FROM VolunteerEntity v WHERE v.accountId = :accountId")
	Long findUserIdByAccountId(Long accountId);

	List<VolunteerEntity> findByElectionEntityIdAndAccountId(Long electionId, Long accountId);

//	@Query("SELECT v FROM VolunteerEntity v " +
//		       "WHERE v.electionEntity.id = :electionId " +
//		       "AND (:mobileNumber IS NULL OR v.mobileNumber = :mobileNumber) " +
//		       "AND (:userId IS NULL OR v.userEntity.id = :userId) " +
//		       "AND (:assignedBooths IS NULL OR v.assignedBooth IN (:assignedBooths))")
//		List<VolunteerEntity> findVolunteersByFilters(
//		    @Param("electionId") Long electionId,
//		    @Param("mobileNumber") String mobileNumber,
//		    @Param("userId") Long userId,
//		    @Param("assignedBooths") List<Long> assignedBooths);

	@Query("SELECT DISTINCT v FROM VolunteerEntity v " +
		       "JOIN v.electionEntity e " +
		       "JOIN v.userEntity u " +
		       "WHERE e.id = :electionId " +
		       "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
		       "AND (:userId IS NULL OR u.id = :userId) " +
		       "AND (:assignedBooths IS NULL OR EXISTS (" +
		       "    SELECT 1 FROM VolunteerEntity v2 " +
		       "    JOIN v2.assignedBooth ab " +
		       "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
		       "))" +
               "ORDER BY v.status DESC, v.id ASC") 
		List<VolunteerEntity> findVolunteerByElectionIdAndAssignedBoothsAndMobileNumberAndUserId(
		    @Param("electionId") Long electionId,
		    @Param("assignedBooths") List<Long> assignedBooths,
		    @Param("mobileNumber") String mobileNumber,
		    @Param("userId") Long userId);

	// Paginated variant (no fixed ORDER BY to allow dynamic Sort via Pageable)
	@Query(value = "SELECT v FROM VolunteerEntity v " +
	       "JOIN v.electionEntity e " +
	       "JOIN v.userEntity u " +
	       "WHERE e.id = :electionId " +
	       "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
	       "AND (:userId IS NULL OR u.id = :userId) " +
	       "AND (:assignedBooths IS NULL OR EXISTS (" +
	       "    SELECT 1 FROM VolunteerEntity v2 " +
	       "    JOIN v2.assignedBooth ab " +
	       "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
	       "))",
	       countQuery = "SELECT COUNT(v.id) FROM VolunteerEntity v " +
	       "JOIN v.electionEntity e " +
	       "JOIN v.userEntity u " +
	       "WHERE e.id = :electionId " +
	       "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
	       "AND (:userId IS NULL OR u.id = :userId) " +
	       "AND (:assignedBooths IS NULL OR EXISTS (" +
	       "    SELECT 1 FROM VolunteerEntity v2 " +
	       "    JOIN v2.assignedBooth ab " +
	       "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
	       "))")
	Page<VolunteerEntity> findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserId(
	    @Param("electionId") Long electionId,
	    @Param("assignedBooths") List<Long> assignedBooths,
	    @Param("mobileNumber") String mobileNumber,
	    @Param("userId") Long userId,
	    Pageable pageable);

	// Variant with searchTerm for name/phone search
    @Query(value = "SELECT v FROM VolunteerEntity v " +
           "JOIN v.electionEntity e " +
           "JOIN v.userEntity u " +
           "WHERE e.id = :electionId " +
           "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
           "AND (:userId IS NULL OR u.id = :userId) " +
           "AND (:assignedBooths IS NULL OR EXISTS (" +
           "    SELECT 1 FROM VolunteerEntity v2 " +
           "    JOIN v2.assignedBooth ab " +
           "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
           ")) " +
		   "AND (:searchTerm IS NULL OR " +
		   "     UPPER(u.firstName) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(u.lastName) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(CONCAT(u.firstName, ' ', u.lastName)) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(CONCAT(u.lastName, ' ', u.firstName)) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR u.mobileNumber LIKE CONCAT('%', :searchTerm, '%') " +
		   "     OR v.mobileNumber LIKE CONCAT('%', :searchTerm, '%'))",
           countQuery = "SELECT COUNT(v.id) FROM VolunteerEntity v " +
           "JOIN v.electionEntity e " +
           "JOIN v.userEntity u " +
           "WHERE e.id = :electionId " +
           "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
           "AND (:userId IS NULL OR u.id = :userId) " +
           "AND (:assignedBooths IS NULL OR EXISTS (" +
           "    SELECT 1 FROM VolunteerEntity v2 " +
           "    JOIN v2.assignedBooth ab " +
           "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
           ")) " +
		   "AND (:searchTerm IS NULL OR " +
		   "     UPPER(u.firstName) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(u.lastName) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(CONCAT(u.firstName, ' ', u.lastName)) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(CONCAT(u.lastName, ' ', u.firstName)) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR u.mobileNumber LIKE CONCAT('%', :searchTerm, '%') " +
		   "     OR v.mobileNumber LIKE CONCAT('%', :searchTerm, '%'))")
    Page<VolunteerEntity> findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserId(
        @Param("electionId") Long electionId,
        @Param("assignedBooths") List<Long> assignedBooths,
        @Param("mobileNumber") String mobileNumber,
        @Param("userId") Long userId,
        @Param("searchTerm") String searchTerm,
        Pageable pageable);

	// Role filtering variants
	@Query(value = "SELECT v FROM VolunteerEntity v " +
	       "JOIN v.electionEntity e " +
	       "JOIN v.userEntity u " +
	       "WHERE e.id = :electionId " +
	       "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
	       "AND (:userId IS NULL OR u.id = :userId) " +
	       "AND (:roleId IS NULL OR v.roleId = :roleId) " +
	       "AND (:assignedBooths IS NULL OR EXISTS (" +
	       "    SELECT 1 FROM VolunteerEntity v2 " +
	       "    JOIN v2.assignedBooth ab " +
	       "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
	       "))")
	Page<VolunteerEntity> findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserIdAndRole(
	    @Param("electionId") Long electionId,
	    @Param("assignedBooths") List<Long> assignedBooths,
	    @Param("mobileNumber") String mobileNumber,
	    @Param("userId") Long userId,
	    @Param("roleId") Long roleId,
	    Pageable pageable);

	// Role filtering with search
    @Query(value = "SELECT v FROM VolunteerEntity v " +
           "JOIN v.electionEntity e " +
           "JOIN v.userEntity u " +
           "WHERE e.id = :electionId " +
           "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
           "AND (:userId IS NULL OR u.id = :userId) " +
           "AND (:roleId IS NULL OR v.roleId = :roleId) " +
           "AND (:assignedBooths IS NULL OR EXISTS (" +
           "    SELECT 1 FROM VolunteerEntity v2 " +
           "    JOIN v2.assignedBooth ab " +
           "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
           ")) " +
		   "AND (:searchTerm IS NULL OR " +
		   "     UPPER(u.firstName) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(u.lastName) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(CONCAT(u.firstName, ' ', u.lastName)) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(CONCAT(u.lastName, ' ', u.firstName)) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR u.mobileNumber LIKE CONCAT('%', :searchTerm, '%') " +
		   "     OR v.mobileNumber LIKE CONCAT('%', :searchTerm, '%'))",
           countQuery = "SELECT COUNT(v.id) FROM VolunteerEntity v " +
           "JOIN v.electionEntity e " +
           "JOIN v.userEntity u " +
           "WHERE e.id = :electionId " +
           "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
           "AND (:userId IS NULL OR u.id = :userId) " +
           "AND (:roleId IS NULL OR v.roleId = :roleId) " +
           "AND (:assignedBooths IS NULL OR EXISTS (" +
           "    SELECT 1 FROM VolunteerEntity v2 " +
           "    JOIN v2.assignedBooth ab " +
           "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
           ")) " +
		   "AND (:searchTerm IS NULL OR " +
		   "     UPPER(u.firstName) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(u.lastName) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(CONCAT(u.firstName, ' ', u.lastName)) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR UPPER(CONCAT(u.lastName, ' ', u.firstName)) LIKE UPPER(CONCAT('%', :searchTerm, '%')) " +
		   "     OR u.mobileNumber LIKE CONCAT('%', :searchTerm, '%') " +
		   "     OR v.mobileNumber LIKE CONCAT('%', :searchTerm, '%'))")
    Page<VolunteerEntity> findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserIdAndRoleWithSearch(
        @Param("electionId") Long electionId,
        @Param("assignedBooths") List<Long> assignedBooths,
        @Param("mobileNumber") String mobileNumber,
        @Param("userId") Long userId,
        @Param("roleId") Long roleId,
        @Param("searchTerm") String searchTerm,
        Pageable pageable);

	// Mobile-only search variant to avoid touching name fields (needed for DBs where name columns are stored as BYTEA)
	@Query(value = "SELECT v FROM VolunteerEntity v " +
		   "JOIN v.electionEntity e " +
		   "JOIN v.userEntity u " +
		   "WHERE e.id = :electionId " +
		   "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
		   "AND (:userId IS NULL OR u.id = :userId) " +
		   "AND (:assignedBooths IS NULL OR EXISTS (" +
		   "    SELECT 1 FROM VolunteerEntity v2 " +
		   "    JOIN v2.assignedBooth ab " +
		   "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
		   ")) " +
		   "AND (:searchTerm IS NULL OR " +
		   "     u.mobileNumber LIKE CONCAT('%', :searchTerm, '%') " +
		   "     OR v.mobileNumber LIKE CONCAT('%', :searchTerm, '%'))",
		   countQuery = "SELECT COUNT(v.id) FROM VolunteerEntity v " +
		   "JOIN v.electionEntity e " +
		   "JOIN v.userEntity u " +
		   "WHERE e.id = :electionId " +
		   "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
		   "AND (:userId IS NULL OR u.id = :userId) " +
		   "AND (:assignedBooths IS NULL OR EXISTS (" +
		   "    SELECT 1 FROM VolunteerEntity v2 " +
		   "    JOIN v2.assignedBooth ab " +
		   "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
		   ")) " +
		   "AND (:searchTerm IS NULL OR " +
		   "     u.mobileNumber LIKE CONCAT('%', :searchTerm, '%') " +
		   "     OR v.mobileNumber LIKE CONCAT('%', :searchTerm, '%'))")
	Page<VolunteerEntity> findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserIdMobileOnly(
		@Param("electionId") Long electionId,
		@Param("assignedBooths") List<Long> assignedBooths,
		@Param("mobileNumber") String mobileNumber,
		@Param("userId") Long userId,
		@Param("searchTerm") String searchTerm,
		Pageable pageable);

	// Most basic volunteer query - just election ID (for debugging empty results)
	@Query("SELECT v FROM VolunteerEntity v WHERE v.electionEntity.id = :electionId")
	Page<VolunteerEntity> findBasicVolunteersByElectionId(
		@Param("electionId") Long electionId,
		Pageable pageable);

	// Simple volunteer query like voters - avoid _user table joins
	@Query("SELECT v FROM VolunteerEntity v " +
		   "WHERE v.electionEntity.id = :electionId " +
		   "AND (:userId IS NULL OR v.userEntity.id = :userId) " +
		   "AND (:assignedBooths IS NULL OR EXISTS (" +
		   "    SELECT 1 FROM VolunteerEntity v2 " +
		   "    JOIN v2.assignedBooth ab " +
		   "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
		   "))")
	Page<VolunteerEntity> findSimpleVolunteerPageByElectionIdAndFilters(
		@Param("electionId") Long electionId,
		@Param("assignedBooths") List<Long> assignedBooths,
		@Param("userId") Long userId,
		Pageable pageable);

	// Completely LIKE-free query - for databases where ALL mobile columns are bytea
	@Query(value = "SELECT v FROM VolunteerEntity v " +
		   "JOIN v.electionEntity e " +
		   "JOIN v.userEntity u " +
		   "WHERE e.id = :electionId " +
		   "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
		   "AND (:userId IS NULL OR u.id = :userId) " +
		   "AND (:assignedBooths IS NULL OR EXISTS (" +
		   "    SELECT 1 FROM VolunteerEntity v2 " +
		   "    JOIN v2.assignedBooth ab " +
		   "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
		   "))",
		   countQuery = "SELECT COUNT(v.id) FROM VolunteerEntity v " +
		   "JOIN v.electionEntity e " +
		   "JOIN v.userEntity u " +
		   "WHERE e.id = :electionId " +
		   "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
		   "AND (:userId IS NULL OR u.id = :userId) " +
		   "AND (:assignedBooths IS NULL OR EXISTS (" +
		   "    SELECT 1 FROM VolunteerEntity v2 " +
		   "    JOIN v2.assignedBooth ab " +
		   "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
		   "))")
	Page<VolunteerEntity> findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserIdNoSearch(
		@Param("electionId") Long electionId,
		@Param("assignedBooths") List<Long> assignedBooths,
		@Param("mobileNumber") String mobileNumber,
		@Param("userId") Long userId,
		Pageable pageable);

	// Completely bytea-safe query - no LIKE operations on _user table columns at all
	@Query(value = "SELECT v FROM VolunteerEntity v " +
		   "JOIN v.electionEntity e " +
		   "JOIN v.userEntity u " +
		   "WHERE e.id = :electionId " +
		   "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
		   "AND (:userId IS NULL OR u.id = :userId) " +
		   "AND (:assignedBooths IS NULL OR EXISTS (" +
		   "    SELECT 1 FROM VolunteerEntity v2 " +
		   "    JOIN v2.assignedBooth ab " +
		   "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
		   ")) " +
		   "AND (:searchTerm IS NULL OR " +
		   "     v.mobileNumber LIKE CONCAT('%', :searchTerm, '%'))",
		   countQuery = "SELECT COUNT(v.id) FROM VolunteerEntity v " +
		   "JOIN v.electionEntity e " +
		   "JOIN v.userEntity u " +
		   "WHERE e.id = :electionId " +
		   "AND (:mobileNumber IS NULL OR u.mobileNumber = :mobileNumber) " +
		   "AND (:userId IS NULL OR u.id = :userId) " +
		   "AND (:assignedBooths IS NULL OR EXISTS (" +
		   "    SELECT 1 FROM VolunteerEntity v2 " +
		   "    JOIN v2.assignedBooth ab " +
		   "    WHERE v2.id = v.id AND ab IN :assignedBooths" +
		   ")) " +
		   "AND (:searchTerm IS NULL OR " +
		   "     v.mobileNumber LIKE CONCAT('%', :searchTerm, '%'))")
	Page<VolunteerEntity> findVolunteerPageByElectionIdAndAssignedBoothsAndMobileNumberAndUserIdByteasafe(
		@Param("electionId") Long electionId,
		@Param("assignedBooths") List<Long> assignedBooths,
		@Param("mobileNumber") String mobileNumber,
		@Param("userId") Long userId,
		@Param("searchTerm") String searchTerm,
		Pageable pageable);

	//List<Long> findAssignedBoothsByUserId(Long currentUserId);

	@Query("SELECT v.assignedBooth FROM VolunteerEntity v WHERE v.userEntity.id = :userId")
	List<Long> findAssignedBoothsByUserId(@Param("userId") Long userId);

	Optional<VolunteerEntity> findByUserEntityId(Long userId);
	
	// Find all volunteers for a user across all elections
	List<VolunteerEntity> findAllByUserEntityId(Long userId);

	VolunteerEntity findByUserEntity(UserEntity userEntity);

	int countByElectionEntity_IdAndStatus(Long electionId, String status);

	@Modifying
	void deleteByUserEntityId(Long userId);
	
	@Query("SELECT COUNT(v) FROM VolunteerEntity v WHERE v.electionEntity.id = :electionId AND LOWER(v.status) = 'active'")
    int countActiveByElectionEntity_Id(@Param("electionId") Long electionId);

    @Query("SELECT COUNT(v) FROM VolunteerEntity v WHERE v.electionEntity.id = :electionId AND LOWER(v.status) = 'inactive'")
    int countInactiveByElectionEntity_Id(@Param("electionId") Long electionId);
    
    @Query("SELECT COUNT(v) FROM VolunteerEntity v WHERE v.electionEntity.id = :electionId AND v.gender = 'male'")
    int countMaleByElectionEntity_Id(@Param("electionId") Long electionId);

    @Query("SELECT COUNT(v) FROM VolunteerEntity v WHERE v.electionEntity.id = :electionId AND v.gender = 'female'")
    int countFemaleByElectionEntity_Id(@Param("electionId") Long electionId);

    @Query("SELECT COUNT(v) FROM VolunteerEntity v WHERE v.electionEntity.id = :electionId AND v.gender = 'other'")
    int countOtherByElectionEntity_Id(@Param("electionId") Long electionId);

	VolunteerEntity findByMobileNumber(String mobileNumber);


	List<VolunteerEntity> findByAccountIdAndAdminUserIdIsNullOrAdminUserId(Long accountId, long l);

	@Query("SELECT v FROM VolunteerEntity v JOIN v.userEntity u WHERE v.accountId != u.accountEntity.id AND v.accountId = :accountId")
    List<VolunteerEntity> findByAccountIdMismatch(@Param("accountId") Long accountId);
	
	// List<VolunteerEntity> findByAdminUserIdIsNullOrAdminUserId(Long adminUserId);

	@Modifying
    @Query("DELETE FROM VolunteerEntity v WHERE v.accountId = :accountId AND v.electionEntity.id = :electionId")
    int deleteByAccountIdAndElectionEntityId(Long accountId, Long electionId);

    @Modifying
    @Query("DELETE FROM VolunteerEntity v WHERE v.accountId = :accountId AND v.electionEntity.id = :electionId AND v.userEntity.id IN :userIds")
    int deleteByAccountIdAndElectionEntityIdAndUserEntityIdIn(Long accountId, Long electionId, List<Long> userIds);

    List<VolunteerEntity> findByAdminUserId(Long adminUserId);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END " +
            "FROM VolunteerEntity v WHERE v.electionEntity.id = :electionId AND v.accountId = :accountId")
     boolean existsByElectionIdAndAccountId(@Param("electionId") Long electionId, 
                                           @Param("accountId") Long accountId);

	Page<VolunteerEntity> findAll(Specification<VolunteerEntity> spec, Pageable pageable);

	int count(Specification<VolunteerEntity> spec);
	
	// Debug method to get unique role names
	@Query("SELECT DISTINCT r.roleName FROM VolunteerEntity v " +
	       "JOIN v.electionEntity e " +
	       "LEFT JOIN v.roleEntity r " +
	       "WHERE e.id = :electionId AND r.roleName IS NOT NULL")
	List<String> findUniqueRoleNamesByElectionId(@Param("electionId") Long electionId);
	
}